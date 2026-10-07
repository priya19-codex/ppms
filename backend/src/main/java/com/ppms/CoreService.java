package com.ppms;

import com.ppms.Errors.ApiException;
import com.ppms.Model.*;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static com.ppms.Model.BStatus.*;

@Service @RequiredArgsConstructor
public class CoreService {
    final Repos.Users users; final Repos.Farmers farmers; final Repos.Centres centres; final Repos.Slots slots;
    final Repos.Bookings bookings; final Repos.Notifications notes; final Repos.QuantityRequests qreqs; final PasswordEncoder pe;
    @Value("${ppms.bags-per-acre:20}") int bagsPerAcre;

    public record RegisterReq(@NotBlank String name,
        @Pattern(regexp = "\\d{10}", message = "Enter a valid 10-digit mobile number.") String mobile,
        @Size(min = 6, message = "Password must be at least 6 characters.") String password,
        @NotBlank String address, @NotBlank String village, @NotBlank String district, @NotBlank String state,
        @Pattern(regexp = "\\d{12}", message = "Aadhaar number must be 12 digits.") String aadhaar,
        @NotBlank String farmerId, Long preferredCentreId, @DecimalMin(value = "0.1", message = "Enter land area in acres.") double landAcres) {}

    public record BookReq(@NotNull Long slotId, @NotBlank String vehicleNumber, @NotBlank String vehicleType,
        @NotBlank String paddyType, @Min(value = 1, message = "Enter a valid paddy quantity.") int quantity, String contactNumber) {}

    // ---------- helpers ----------
    public User user(Authentication a) { return users.findByMobile(a.getName()).orElseThrow(() -> new ApiException(401, "Please login again.")); }
    public Farmer farmer(User u) { return farmers.findByUserId(u.getId()).orElseThrow(() -> new ApiException(403, "A farmer account is required.")); }
    public void push(User u, String title, String msg, String type) {
        Notification n = new Notification(); n.setUser(u); n.setTitle(title); n.setMessage(msg); n.setType(type); notes.save(n);
    }
    public void scope(User u, Long centreId) {
        if (u.getRole() == Role.STAFF && !Objects.equals(centreId, u.getCentreId()))
            throw new ApiException(403, "You can only manage your own procurement centre.");
    }
    public int usedQty(Farmer f) { return bookings.activeQty(f.getId(), CANCELLED).intValue(); }

    public Map<String, Object> view(Booking b) {
        Slot s = b.getSlot(); Farmer f = b.getFarmer();
        return Api.m("bookingId", b.getBookingId(), "token", b.getTokenNumber(), "status", b.getBookingStatus(),
            "centreId", b.getCentre().getId(), "centre", b.getCentre().getCentreName(), "farmer", f.getUser().getName(),
            "farmerId", f.getFarmerId(), "mobile", f.getUser().getMobile(), "date", b.getBookingDate(),
            "start", s.getStartTime(), "end", s.getEndTime(), "vehicle", b.getVehicleNumber(), "vehicleType", b.getVehicleType(),
            "quantity", b.getPaddyQuantity(), "paddyType", b.getPaddyType(), "contact", b.getContactNumber(), "createdAt", b.getCreatedAt());
    }
    public Map<String, Object> farmerView(Farmer f) {
        String prefName = f.getPreferredCentreId() != null ?
            centres.findById(f.getPreferredCentreId()).map(Centre::getCentreName).orElse("Not specified") : "Not specified";
        return Api.m("id", f.getId(), "userId", f.getUser().getId(), "name", f.getUser().getName(), "mobile", f.getUser().getMobile(),
            "status", f.getUser().getStatus(), "farmerId", f.getFarmerId(), "aadhaar", f.getAadhaarMasked(), "address", f.getAddress(),
            "village", f.getVillage(), "district", f.getDistrict(), "state", f.getState(), "preferredCentreId", f.getPreferredCentreId(),
            "preferredCentreName", prefName, "landAcres", f.getLandAcres(), "approvedMaxQty", f.getApprovedMaxQty(), "usedQty", usedQty(f));
    }
    public Map<String, Object> centreView(Centre c) {
        LocalDate t = LocalDate.now();
        return Api.m("id", c.getId(), "centreCode", c.getCentreCode(), "centreName", c.getCentreName(), "address", c.getAddress(),
            "village", c.getVillage(), "district", c.getDistrict(), "contactNumber", c.getContactNumber(), "openTime", c.getOpenTime(),
            "closeTime", c.getCloseTime(), "maxDailyCapacity", c.getMaxDailyCapacity(), "status", c.getStatus(),
            "availableToday", slots.availableOn(c.getId(), t, LocalTime.now()),
            "queue", bookings.countByCentreIdAndBookingDateAndBookingStatusIn(c.getId(), t, List.of(CONFIRMED, ARRIVED, WAITING, UNLOADING)));
    }

    // ---------- registration ----------
    @Transactional
    public User register(RegisterReq r) {
        if (users.findByMobile(r.mobile()).isPresent()) throw new ApiException(409, "This mobile number is already registered.");
        if (farmers.existsByFarmerId(r.farmerId().trim())) throw new ApiException(409, "This Farmer ID is already registered.");
        User u = new User(); u.setName(r.name().trim()); u.setMobile(r.mobile()); u.setPasswordHash(pe.encode(r.password())); u.setRole(Role.FARMER);
        users.save(u);
        Farmer f = new Farmer(); f.setUser(u); f.setFarmerId(r.farmerId().trim()); f.setAadhaarMasked("XXXX XXXX " + r.aadhaar().substring(8));
        f.setAddress(r.address()); f.setVillage(r.village()); f.setDistrict(r.district()); f.setState(r.state());
        f.setPreferredCentreId(r.preferredCentreId()); f.setLandAcres(r.landAcres());
        f.setApprovedMaxQty((int) Math.round(r.landAcres() * bagsPerAcre)); // provisional until admin reviews
        farmers.save(f);
        push(u, "Welcome", "Your account is ready. Your provisional paddy limit is " + f.getApprovedMaxQty() + " bags; the admin may revise it.", "INFO");
        return u;
    }

    // ---------- THE CORE: booking ----------
    /**
     * Concurrency strategy: (1) lock the centre row (serialises token numbering), (2) re-read the slot after the lock,
     * (3) atomic "UPDATE ... WHERE booked_count < capacity" as the final overbooking guard, (4) unique constraints as a last net.
     */
    @Transactional
    public Map<String, Object> book(User u, BookReq r) {
        Farmer f = farmer(u);
        Long centreId = slots.centreIdOf(r.slotId()).orElseThrow(() -> new ApiException(404, "The selected slot was not found."));
        Centre c = centres.lockById(centreId).orElseThrow(() -> new ApiException(404, "Procurement centre not found."));
        Slot s = slots.lockById(r.slotId()).orElseThrow(() -> new ApiException(404, "The selected slot was not found."));
        if (!"OPEN".equals(c.getStatus())) throw new ApiException(409, "Centre is currently closed.");
        if (!"OPEN".equals(s.getStatus())) throw new ApiException(409, "No slots available for this time.");
        if (LocalDateTime.of(s.getSlotDate(), s.getStartTime()).isBefore(LocalDateTime.now()))
            throw new ApiException(400, "This slot has already started. Please choose a later slot.");
        if (s.getBookedCount() >= s.getCapacity()) throw new ApiException(409, "This slot is already full.");
        if (bookings.existsByFarmerIdAndSlotIdAndBookingStatusNot(f.getId(), s.getId(), CANCELLED))
            throw new ApiException(409, "You already have a booking in this slot.");
        int used = usedQty(f);
        if (used + r.quantity() > f.getApprovedMaxQty())
            throw new ApiException(422, "Quantity exceeds your approved limit of " + f.getApprovedMaxQty() + " bags (" + used
                + " already booked). Request extra quantity from your profile.");
        if (slots.reserve(s.getId()) != 1) throw new ApiException(409, "This slot is already full.");

        int seq = (int) bookings.countByCentreIdAndBookingDate(c.getId(), s.getSlotDate()) + 1;
        Booking b = new Booking();
        b.setFarmer(f); b.setCentre(c); b.setSlot(s); b.setBookingDate(s.getSlotDate());
        b.setTokenNumber(String.format("A%03d", seq));
        b.setBookingId("PB" + s.getSlotDate().format(DateTimeFormatter.BASIC_ISO_DATE) + String.format("%02d%04d", c.getId() % 100, seq));
        b.setVehicleNumber(r.vehicleNumber().trim().toUpperCase()); b.setVehicleType(r.vehicleType()); b.setPaddyType(r.paddyType());
        b.setPaddyQuantity(r.quantity()); b.setContactNumber(r.contactNumber() == null || r.contactNumber().isBlank() ? u.getMobile() : r.contactNumber());
        bookings.saveAndFlush(b);
        push(u, "Booking confirmed", "Your token " + b.getTokenNumber() + " is confirmed for " + s.getSlotDate() + " at " + s.getStartTime() + ".", "BOOKING");
        push(u, "Reminder", "Please reach the procurement centre 15 minutes before your slot.", "REMINDER");
        return view(b);
    }

    @Transactional
    public Map<String, Object> cancel(User u, String bookingId) {
        Long cid = bookings.centreIdOf(bookingId).orElseThrow(() -> new ApiException(404, "Booking not found."));
        centres.lockById(cid);
        Booking b = bookings.findByBookingId(bookingId).orElseThrow();
        boolean owner = b.getFarmer().getUser().getId().equals(u.getId());
        if (!owner && u.getRole() == Role.FARMER) throw new ApiException(403, "You can only modify your own bookings.");
        if (!owner) scope(u, cid);
        if (b.getBookingStatus() != CONFIRMED) throw new ApiException(409, "Only confirmed bookings can be cancelled.");
        if (owner && LocalDateTime.now().isAfter(LocalDateTime.of(b.getBookingDate(), b.getSlot().getStartTime()).minusHours(2)))
            throw new ApiException(409, "Bookings can only be cancelled up to 2 hours before the slot.");
        slots.release(b.getSlot().getId());
        b.setBookingStatus(CANCELLED); b.setUpdatedAt(LocalDateTime.now()); bookings.save(b);
        push(b.getFarmer().getUser(), "Booking cancelled", "Booking " + b.getBookingId() + " (token " + b.getTokenNumber() + ") was cancelled.", "BOOKING");
        return view(b);
    }

    static final Map<BStatus, List<BStatus>> FLOW = Map.of(CONFIRMED, List.of(ARRIVED), ARRIVED, List.of(WAITING, UNLOADING),
        WAITING, List.of(UNLOADING), UNLOADING, List.of(COMPLETED));

    @Transactional
    public Map<String, Object> updateStatus(User u, String bookingId, BStatus next) {
        Booking b = bookings.findByBookingId(bookingId).orElseThrow(() -> new ApiException(404, "Booking not found."));
        scope(u, b.getCentre().getId());
        if (!FLOW.getOrDefault(b.getBookingStatus(), List.of()).contains(next))
            throw new ApiException(409, "Status cannot change from " + b.getBookingStatus() + " to " + next + ".");
        b.setBookingStatus(next); b.setUpdatedAt(LocalDateTime.now()); bookings.save(b);
        User fu = b.getFarmer().getUser(); String t = b.getTokenNumber();
        switch (next) {
            case ARRIVED -> push(fu, "Arrival recorded", "Token " + t + ": you are marked as arrived. Please wait for your turn.", "STATUS");
            case WAITING -> push(fu, "In waiting queue", "Token " + t + ": your vehicle is in the waiting queue.", "STATUS");
            case UNLOADING -> push(fu, "Unloading started", "Your unloading process has started (token " + t + ").", "STATUS");
            case COMPLETED -> push(fu, "Procurement completed", "Your procurement has been completed (token " + t + ").", "STATUS");
            default -> { }
        }
        return view(b);
    }

    @Scheduled(initialDelay = 15000, fixedDelay = 1800000)
    @Transactional
    public void sendReminders() {
        for (Booking b : bookings.findByBookingDateAndBookingStatusAndReminderSentFalse(LocalDate.now().plusDays(1), CONFIRMED)) {
            push(b.getFarmer().getUser(), "Slot tomorrow", "Your paddy procurement slot is tomorrow at " + b.getSlot().getStartTime() + ".", "REMINDER");
            b.setReminderSent(true); bookings.save(b);
        }
    }

    // ---------- quantity limits ----------
    @Transactional
    public void requestExtra(User u, int qty, String reason) {
        Farmer f = farmer(u);
        if (qty < 1) throw new ApiException(400, "Enter a valid quantity.");
        boolean pending = qreqs.findByFarmerIdOrderByCreatedAtDesc(f.getId()).stream().anyMatch(q -> "PENDING".equals(q.getStatus()));
        if (pending) throw new ApiException(409, "You already have a pending request.");
        QuantityRequest q = new QuantityRequest(); q.setFarmer(f); q.setRequestedQty(qty); q.setReason(reason); qreqs.save(q);
    }
    @Transactional
    public void decide(Long id, boolean approve) {
        QuantityRequest q = qreqs.findById(id).orElseThrow(() -> new ApiException(404, "Request not found."));
        if (!"PENDING".equals(q.getStatus())) throw new ApiException(409, "This request is already reviewed.");
        q.setStatus(approve ? "APPROVED" : "REJECTED"); qreqs.save(q);
        Farmer f = q.getFarmer();
        if (approve) { f.setApprovedMaxQty(f.getApprovedMaxQty() + q.getRequestedQty()); farmers.save(f); }
        push(f.getUser(), "Extra quantity " + (approve ? "approved" : "rejected"),
            approve ? "Your limit is now " + f.getApprovedMaxQty() + " bags." : "Your request for " + q.getRequestedQty() + " extra bags was not approved.", "INFO");
    }
}
