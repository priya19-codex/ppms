package com.ppms;

import com.ppms.Model.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import java.time.*;
import java.util.*;

/** Creates demo data on first start (empty database only). Dates are relative to today. */
@Component @RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {
    final Repos.Users users; final Repos.Farmers farmers; final Repos.Centres centres; final Repos.Slots slots;
    final Repos.Bookings bookings; final Repos.Notifications notes; final Repos.QuantityRequests qreqs; final PasswordEncoder pe;
    final Map<String, Slot> slotMap = new HashMap<>();

    User user(String name, String mobile, String pw, Role r, Long centreId) {
        User u = new User(); u.setName(name); u.setMobile(mobile); u.setPasswordHash(pe.encode(pw)); u.setRole(r); u.setCentreId(centreId); return users.save(u);
    }
    Centre centre(String code, String name, String addr, String village, String district, String phone) {
        Centre c = new Centre(); c.setCentreCode(code); c.setCentreName(name); c.setAddress(addr); c.setVillage(village); c.setDistrict(district);
        c.setContactNumber(phone); c.setMaxDailyCapacity(50); return centres.save(c);
    }
    Farmer farmer(String name, String mobile, String fid, String village, String district, double acres, Centre pref) {
        User u = user(name, mobile, "Farmer@123", Role.FARMER, null);
        Farmer f = new Farmer(); f.setUser(u); f.setFarmerId(fid); f.setAadhaarMasked("XXXX XXXX " + (1000 + (int) (acres * 111)));
        f.setAddress("12, Main Road, " + village); f.setVillage(village); f.setDistrict(district); f.setState("Tamil Nadu");
        f.setPreferredCentreId(pref.getId()); f.setLandAcres(acres); f.setApprovedMaxQty((int) (acres * 20)); return farmers.save(f);
    }
    Slot slot(Centre c, int day, int hour, int cap) {
        Slot s = new Slot(); s.setCentre(c); s.setSlotDate(LocalDate.now().plusDays(day)); s.setStartTime(LocalTime.of(hour, 0)); s.setEndTime(LocalTime.of(hour + 1, 0)); s.setCapacity(cap);
        s = slots.save(s); slotMap.put(c.getId() + ":" + day + ":" + hour, s); return s;
    }
    Booking book(Farmer f, Centre c, int day, int hour, BStatus st, int qty, String veh, String type) {
        Slot s = slotMap.get(c.getId() + ":" + day + ":" + hour);
        int seq = (int) bookings.countByCentreIdAndBookingDate(c.getId(), s.getSlotDate()) + 1;
        Booking b = new Booking(); b.setFarmer(f); b.setCentre(c); b.setSlot(s); b.setBookingDate(s.getSlotDate()); b.setTokenNumber(String.format("A%03d", seq));
        b.setBookingId("PB" + s.getSlotDate().toString().replace("-", "") + String.format("%02d%04d", c.getId() % 100, seq));
        b.setVehicleNumber(veh); b.setVehicleType(type); b.setPaddyType("Ponni"); b.setPaddyQuantity(qty); b.setContactNumber(f.getUser().getMobile()); b.setBookingStatus(st);
        if (st != BStatus.CANCELLED) { s.setBookedCount(s.getBookedCount() + 1); slots.save(s); }
        return bookings.save(b);
    }
    void note(User u, String t, String m, String type) { Notification n = new Notification(); n.setUser(u); n.setTitle(t); n.setMessage(m); n.setType(type); notes.save(n); }

    @Override public void run(String... args) {
        if (users.count() > 0) return;
        Centre c1 = centre("TNJ-001", "Thanjavur Delta Procurement Centre", "NH 83, Vallam Road", "Vallam", "Thanjavur", "04362-200101");
        Centre c2 = centre("TVR-001", "Tiruvarur Paddy Procurement Centre", "Mannargudi Road", "Kodavasal", "Tiruvarur", "04366-200202");
        Centre c3 = centre("NGP-001", "Nagapattinam Kuruvai Procurement Centre", "Velankanni Main Road", "Keelvelur", "Nagapattinam", "04365-200303");
        user("System Admin", "9000000001", "Admin@123", Role.ADMIN, null);
        user("Anitha Staff", "9000000002", "Staff@123", Role.STAFF, c1.getId());
        user("Murugan Staff", "9000000003", "Staff@123", Role.STAFF, c2.getId());
        Farmer priya = farmer("Priya Selvam", "9876500001", "TNF-1001", "Vallam", "Thanjavur", 5, c1);
        Farmer ravi = farmer("Ravi Chandran", "9876500002", "TNF-1002", "Orathanadu", "Thanjavur", 3, c1);
        Farmer kumar = farmer("Kumar Rajan", "9876500003", "TNF-1003", "Kodavasal", "Tiruvarur", 4, c1);
        Farmer suresh = farmer("Suresh Babu", "9876500004", "TNF-1004", "Keelvelur", "Nagapattinam", 2, c3);
        Farmer lakshmi = farmer("Lakshmi Devi", "9876500005", "TNF-1005", "Mannargudi", "Tiruvarur", 6, c2);
        for (Centre c : List.of(c1, c2, c3))
            for (int d = -1; d <= 6; d++)
                for (int h : new int[]{8, 9, 10, 11, 14}) slot(c, d, h, (c == c1 && d == 1 && h == 10) ? 2 : 10);
        book(priya, c1, -1, 9, BStatus.COMPLETED, 30, "TN 68 AB 1234", "Tractor");
        book(ravi, c1, -1, 10, BStatus.COMPLETED, 25, "TN 49 CD 4521", "Mini Truck");
        book(lakshmi, c1, 0, 8, BStatus.COMPLETED, 40, "TN 50 EF 9087", "Lorry");
        book(lakshmi, c1, 0, 9, BStatus.UNLOADING, 20, "TN 50 EF 9087", "Lorry");
        book(kumar, c1, 0, 9, BStatus.ARRIVED, 30, "TN 63 GH 7788", "Tractor");
        book(ravi, c1, 0, 9, BStatus.CONFIRMED, 20, "TN 49 CD 4521", "Mini Truck");
        book(suresh, c1, 0, 10, BStatus.WAITING, 15, "TN 51 JK 3321", "Tempo");
        book(priya, c1, 1, 9, BStatus.CONFIRMED, 25, "TN 68 AB 1234", "Tractor");
        book(ravi, c1, 1, 10, BStatus.CONFIRMED, 10, "TN 49 CD 4521", "Mini Truck");
        book(kumar, c1, 1, 10, BStatus.CONFIRMED, 10, "TN 63 GH 7788", "Tractor"); // fills the 2-seat slot -> FULL
        book(priya, c1, 3, 11, BStatus.CANCELLED, 20, "TN 68 AB 1234", "Tractor");
        book(lakshmi, c2, 2, 8, BStatus.CONFIRMED, 35, "TN 50 EF 9087", "Lorry");
        QuantityRequest q = new QuantityRequest(); q.setFarmer(suresh); q.setRequestedQty(20); q.setReason("Harvested more than expected after good rains."); qreqs.save(q);
        note(priya.getUser(), "Slot tomorrow", "Your paddy procurement slot is tomorrow at 09:00.", "REMINDER");
        note(priya.getUser(), "Booking confirmed", "Your token A001 is confirmed.", "BOOKING");
        note(priya.getUser(), "Reminder", "Please reach the procurement centre 15 minutes before your slot.", "REMINDER");
        note(priya.getUser(), "Procurement completed", "Your procurement has been completed (token A001).", "STATUS");
    }
}
