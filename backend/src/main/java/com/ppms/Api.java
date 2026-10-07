package com.ppms;

import com.ppms.CoreService.*;
import com.ppms.Errors.ApiException;
import com.ppms.Model.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

import static com.ppms.Model.BStatus.*;

/** REST controllers (thin: validation + delegation to CoreService / repositories). */
public class Api {
    static Map<String, Object> m(Object... kv) {
        var r = new LinkedHashMap<String, Object>();
        for (int i = 0; i < kv.length; i += 2) r.put((String) kv[i], kv[i + 1]);
        return r;
    }
    static long cnt(List<Booking> l, BStatus s) { return l.stream().filter(b -> b.getBookingStatus() == s).count(); }
    static int qty(List<Booking> l) { return l.stream().filter(b -> b.getBookingStatus() != CANCELLED).mapToInt(Booking::getPaddyQuantity).sum(); }
    static final List<BStatus> ACTIVE = List.of(CONFIRMED, ARRIVED, WAITING, UNLOADING);

    @RestController @RequestMapping("/api/auth") @RequiredArgsConstructor
    static class AuthCtl {
        final CoreService svc;
        @PostMapping("/register") @ResponseStatus(HttpStatus.CREATED)
        Map<String, Object> register(@Valid @RequestBody RegisterReq r) { User u = svc.register(r); return m("id", u.getId(), "name", u.getName()); }
        @PostMapping("/login") Map<String, Object> login(Authentication a) {
            User u = svc.user(a);
            return m("id", u.getId(), "name", u.getName(), "mobile", u.getMobile(), "role", u.getRole(), "centreId", u.getCentreId());
        }
    }

    @RestController @RequestMapping("/api/centres") @RequiredArgsConstructor
    static class CentreCtl {
        final Repos.Centres centres; final CoreService svc;
        @GetMapping List<Map<String, Object>> list(@RequestParam(required = false) String q) {
            String k = q == null ? "" : q.toLowerCase().trim();
            return centres.findAll().stream()
                .filter(c -> k.isEmpty() || (c.getCentreName() + " " + c.getVillage() + " " + c.getDistrict() + " " + c.getAddress()).toLowerCase().contains(k))
                .map(svc::centreView).toList();
        }
        @GetMapping("/{id}") Map<String, Object> one(@PathVariable Long id) {
            return svc.centreView(centres.findById(id).orElseThrow(() -> new ApiException(404, "Centre not found.")));
        }
    }

    @RestController @RequestMapping("/api/slots") @RequiredArgsConstructor
    static class SlotCtl {
        final Repos.Slots slots;
        List<Slot> day(Long centreId, LocalDate d) { return slots.findByCentreIdAndSlotDateOrderByStartTime(centreId, d == null ? LocalDate.now() : d); }
        @GetMapping List<Slot> list(@RequestParam Long centreId, @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) { return day(centreId, date); }
        @GetMapping("/available") List<Slot> avail(@RequestParam Long centreId, @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
            return day(centreId, date).stream().filter(s -> s.state().equals("AVAILABLE") || s.state().equals("LIMITED")).toList();
        }
        @GetMapping("/{id}") Slot one(@PathVariable Long id) { return slots.findById(id).orElseThrow(() -> new ApiException(404, "The selected slot was not found.")); }
    }

    @RestController @RequestMapping("/api/bookings") @RequiredArgsConstructor
    static class BookingCtl {
        final CoreService svc; final Repos.Bookings bookings;
        @PostMapping @ResponseStatus(HttpStatus.CREATED) Map<String, Object> book(@Valid @RequestBody BookReq r, Authentication a) { return svc.book(svc.user(a), r); }
        @GetMapping({"/my", "/mine"}) List<Map<String, Object>> mine(Authentication a) {
            return bookings.findByFarmerIdOrderByCreatedAtDesc(svc.farmer(svc.user(a)).getId()).stream().map(svc::view).toList();
        }
        @GetMapping("/{id}") Map<String, Object> one(@PathVariable String id, Authentication a) {
            User u = svc.user(a);
            Booking b = bookings.findByBookingId(id).orElseThrow(() -> new ApiException(404, "Booking not found."));
            if (u.getRole() == Role.FARMER && !b.getFarmer().getUser().getId().equals(u.getId())) throw new ApiException(403, "You can only view your own bookings.");
            svc.scope(u, b.getCentre().getId());
            return svc.view(b);
        }
        @RequestMapping(value = "/{id}/cancel", method = {RequestMethod.PUT, RequestMethod.POST})
        Map<String, Object> cancel(@PathVariable String id, Authentication a) { return svc.cancel(svc.user(a), id); }
    }

    @RestController @RequestMapping("/api/notifications") @RequiredArgsConstructor
    static class NotifCtl {
        final CoreService svc; final Repos.Notifications notes;
        @GetMapping List<Notification> list(Authentication a) { return notes.findByUserIdOrderByCreatedAtDesc(svc.user(a).getId()); }
        @GetMapping("/unread-count") Map<String, Object> count(Authentication a) { return m("count", notes.findByUserIdAndIsReadFalse(svc.user(a).getId()).size()); }
        @PutMapping("/read-all") @Transactional void readAll(Authentication a) {
            var l = notes.findByUserIdAndIsReadFalse(svc.user(a).getId()); l.forEach(n -> n.setRead(true)); notes.saveAll(l);
        }
    }

    @RestController @RequestMapping("/api/farmers/me") @RequiredArgsConstructor
    static class FarmerCtl {
        final CoreService svc; final Repos.Farmers farmers; final Repos.QuantityRequests qreqs;
        @GetMapping Map<String, Object> me(Authentication a) { return svc.farmerView(svc.farmer(svc.user(a))); }
        @PutMapping Map<String, Object> update(@RequestBody Map<String, Object> b, Authentication a) {
            Farmer f = svc.farmer(svc.user(a));
            f.setAddress(str(b, "address", f.getAddress())); f.setVillage(str(b, "village", f.getVillage()));
            f.setDistrict(str(b, "district", f.getDistrict())); f.setState(str(b, "state", f.getState()));
            if (b.get("preferredCentreId") != null) f.setPreferredCentreId(((Number) b.get("preferredCentreId")).longValue());
            farmers.save(f); return svc.farmerView(f);
        }
        static String str(Map<String, Object> b, String k, String d) { Object v = b.get(k); return v == null || v.toString().isBlank() ? d : v.toString().trim(); }
        @GetMapping("/quantity-requests") List<QuantityRequest> reqs(Authentication a) { return qreqs.findByFarmerIdOrderByCreatedAtDesc(svc.farmer(svc.user(a)).getId()); }
        @PostMapping("/quantity-requests") @ResponseStatus(HttpStatus.CREATED) void req(@RequestBody Map<String, Object> b, Authentication a) {
            svc.requestExtra(svc.user(a), ((Number) b.getOrDefault("quantity", 0)).intValue(), String.valueOf(b.getOrDefault("reason", "")));
        }
    }

    // ---------------- Staff (and Admin) ----------------
    record SlotReq(Long centreId, @NotNull LocalDate date, @NotNull LocalTime start, @NotNull LocalTime end, @Min(value = 1, message = "Capacity must be at least 1.") int capacity, String status) {}

    @RestController @RequestMapping("/api/staff") @RequiredArgsConstructor
    static class StaffCtl {
        final CoreService svc; final Repos.Slots slots; final Repos.Bookings bookings; final Repos.Centres centres;
        Long cid(User u, Long p) {
            Long c = u.getRole() == Role.STAFF ? u.getCentreId() : p;
            if (c == null) throw new ApiException(400, "Select a procurement centre.");
            return c;
        }
        @GetMapping("/slots") List<Slot> slotList(Authentication a, @RequestParam(required = false) Long centreId) {
            return slots.findByCentreIdAndSlotDateGreaterThanEqualOrderBySlotDateAscStartTimeAsc(cid(svc.user(a), centreId), LocalDate.now());
        }
        @PostMapping("/slots") @Transactional @ResponseStatus(HttpStatus.CREATED) Slot create(@Valid @RequestBody SlotReq r, Authentication a) {
            Long c = cid(svc.user(a), r.centreId());
            if (!r.end().isAfter(r.start())) throw new ApiException(400, "End time must be after start time.");
            if (slots.findByCentreIdAndSlotDateOrderByStartTime(c, r.date()).stream().anyMatch(s -> s.getStartTime().equals(r.start())))
                throw new ApiException(409, "A slot already exists for this time.");
            Slot s = new Slot(); s.setCentre(centres.findById(c).orElseThrow(() -> new ApiException(404, "Centre not found.")));
            s.setSlotDate(r.date()); s.setStartTime(r.start()); s.setEndTime(r.end()); s.setCapacity(r.capacity());
            if (r.status() != null) s.setStatus(r.status());
            return slots.save(s);
        }
        @PutMapping("/slots/{id}") @Transactional Slot update(@PathVariable Long id, @RequestBody Map<String, Object> b, Authentication a) {
            User u = svc.user(a);
            Long c = slots.centreIdOf(id).orElseThrow(() -> new ApiException(404, "Slot not found."));
            svc.scope(u, c); centres.lockById(c);
            Slot s = slots.lockById(id).orElseThrow();
            if (b.get("capacity") != null) {
                int cap = ((Number) b.get("capacity")).intValue();
                if (cap < s.getBookedCount()) throw new ApiException(409, "Capacity cannot be lower than the " + s.getBookedCount() + " existing bookings.");
                if (cap < 1) throw new ApiException(400, "Capacity must be at least 1.");
                s.setCapacity(cap);
            }
            if (b.get("status") != null) s.setStatus(b.get("status").toString());
            return slots.save(s);
        }
        @DeleteMapping("/slots/{id}") @Transactional void delete(@PathVariable Long id, Authentication a) {
            Long c = slots.centreIdOf(id).orElseThrow(() -> new ApiException(404, "Slot not found."));
            svc.scope(svc.user(a), c); centres.lockById(c);
            Slot s = slots.lockById(id).orElseThrow();
            if (s.getBookedCount() > 0) throw new ApiException(409, "This slot has bookings. Close it instead of deleting.");
            slots.delete(s);
        }
        @GetMapping("/bookings") List<Map<String, Object>> queue(Authentication a, @RequestParam(required = false) Long centreId,
                @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
            return bookings.findByCentreIdAndBookingDateOrderByTokenNumber(cid(svc.user(a), centreId), date == null ? LocalDate.now() : date)
                .stream().map(svc::view).toList();
        }
        @PutMapping("/bookings/{id}/status") Map<String, Object> status(@PathVariable String id, @RequestBody Map<String, String> b, Authentication a) {
            BStatus s; try { s = BStatus.valueOf(b.get("status")); } catch (Exception e) { throw new ApiException(400, "Invalid status."); }
            return svc.updateStatus(svc.user(a), id, s);
        }
        @GetMapping("/stats") Map<String, Object> stats(Authentication a, @RequestParam(required = false) Long centreId,
                @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
            Long c = cid(svc.user(a), centreId); LocalDate d = date == null ? LocalDate.now() : date;
            var bs = bookings.findByCentreIdAndBookingDateOrderByTokenNumber(c, d).stream().filter(b -> b.getBookingStatus() != CANCELLED).toList();
            var sl = slots.findByCentreIdAndSlotDateOrderByStartTime(c, d);
            return m("total", bs.size(), "availableSlots", sl.stream().filter(s -> s.state().equals("AVAILABLE") || s.state().equals("LIMITED")).count(),
                "occupiedSlots", sl.stream().filter(s -> s.getBookedCount() > 0).count(), "confirmed", cnt(bs, CONFIRMED), "arrived", cnt(bs, ARRIVED),
                "waiting", cnt(bs, WAITING), "unloading", cnt(bs, UNLOADING), "completed", cnt(bs, COMPLETED), "quantity", qty(bs));
        }
    }

    // ---------------- Admin ----------------
    record CentreReq(@NotBlank String centreCode, @NotBlank String centreName, @NotBlank String address, String village, String district,
        String contactNumber, LocalTime openTime, LocalTime closeTime, @Min(1) int maxDailyCapacity, String status) {}
    record StaffReq(@NotBlank String name, @Pattern(regexp = "\\d{10}", message = "Enter a valid 10-digit mobile number.") String mobile,
        @Size(min = 6, message = "Password must be at least 6 characters.") String password, @NotNull Long centreId) {}

    @RestController @RequestMapping("/api/admin") @RequiredArgsConstructor
    static class AdminCtl {
        final CoreService svc; final Repos.Users users; final Repos.Farmers farmers; final Repos.Centres centres; final Repos.Slots slots;
        final Repos.Bookings bookings; final Repos.QuantityRequests qreqs; final PasswordEncoder pe;

        @GetMapping("/dashboard") Map<String, Object> dash() {
            LocalDate t = LocalDate.now();
            var all = bookings.findByBookingDateBetween(t.minusDays(30), t.plusDays(30));
            var daily = new LinkedHashMap<String, Object>(); var done = new LinkedHashMap<String, Object>();
            for (int i = -6; i <= 0; i++) {
                LocalDate d = t.plusDays(i);
                var l = all.stream().filter(b -> b.getBookingDate().equals(d)).toList();
                daily.put(d.toString().substring(5), l.stream().filter(b -> b.getBookingStatus() != CANCELLED).count()); done.put(d.toString().substring(5), cnt(l, COMPLETED));
            }
            var byCentre = all.stream().filter(b -> b.getBookingStatus() != CANCELLED).collect(Collectors.groupingBy(b -> b.getCentre().getCentreName(), TreeMap::new, Collectors.counting()));
            var ss = slots.findBySlotDateBetween(t, t.plusDays(6));
            var util = new LinkedHashMap<String, Object>();
            int totalCap = ss.stream().mapToInt(Slot::getCapacity).sum();
            int totalBk = ss.stream().mapToInt(Slot::getBookedCount).sum();
            int overallUtil = totalCap > 0 ? Math.round(totalBk * 100f / totalCap) : 0;
            ss.stream().collect(Collectors.groupingBy(s -> s.getCentre().getCentreName(), TreeMap::new, Collectors.toList())).forEach((k, l) -> {
                int cap = l.stream().mapToInt(Slot::getCapacity).sum(), bk = l.stream().mapToInt(Slot::getBookedCount).sum();
                util.put(k, cap == 0 ? 0 : Math.round(bk * 100f / cap));
            });
            var today = all.stream().filter(b -> b.getBookingDate().equals(t)).toList();
            long totalPaddyQuantity = all.stream().filter(b -> b.getBookingStatus() != CANCELLED).mapToLong(Booking::getPaddyQuantity).sum();
            long todayPaddyQuantity = today.stream().filter(b -> b.getBookingStatus() != CANCELLED).mapToLong(Booking::getPaddyQuantity).sum();
            var statusBreakdown = new LinkedHashMap<String, Long>();
            for (BStatus st : BStatus.values()) {
                statusBreakdown.put(st.name(), all.stream().filter(b -> b.getBookingStatus() == st).count());
            }
            return m("totalFarmers", farmers.count(), "totalCentres", centres.count(),
                "todayBookings", today.stream().filter(b -> b.getBookingStatus() != CANCELLED).count(),
                "todayConfirmed", cnt(today, CONFIRMED), "todayArrived", cnt(today, ARRIVED),
                "todayWaiting", cnt(today, WAITING), "todayUnloading", cnt(today, UNLOADING),
                "todayCompleted", cnt(today, COMPLETED), "todayCancelled", cnt(today, CANCELLED),
                "completed", cnt(all, COMPLETED), "pending", all.stream().filter(b -> ACTIVE.contains(b.getBookingStatus())).count(), "cancelled", cnt(all, CANCELLED),
                "totalPaddyQuantity", totalPaddyQuantity, "todayPaddyQuantity", todayPaddyQuantity,
                "slotUtilization", overallUtil, "statusBreakdown", statusBreakdown,
                "availableSlots", ss.stream().filter(s -> s.state().equals("AVAILABLE") || s.state().equals("LIMITED")).count(),
                "daily", daily, "completedDaily", done, "centreWise", byCentre, "utilization", util);
        }
        @GetMapping("/farmers") List<Map<String, Object>> farmerList() { return farmers.findAll().stream().map(svc::farmerView).toList(); }
        @PutMapping("/farmers/{id}/max-quantity") void maxQty(@PathVariable Long id, @RequestBody Map<String, Object> b) {
            int v = ((Number) b.getOrDefault("value", -1)).intValue();
            if (v < 0) throw new ApiException(400, "Enter a valid quantity.");
            Farmer f = farmers.findById(id).orElseThrow(() -> new ApiException(404, "Farmer not found."));
            f.setApprovedMaxQty(v); farmers.save(f);
            svc.push(f.getUser(), "Paddy limit updated", "Your approved maximum quantity is now " + v + " bags.", "INFO");
        }
        @GetMapping("/quantity-requests") List<Map<String, Object>> reqs() {
            return qreqs.findAllByOrderByCreatedAtDesc().stream().map(q -> m("id", q.getId(), "farmer", q.getFarmer().getUser().getName(), "farmerId", q.getFarmer().getFarmerId(),
                "approvedMaxQty", q.getFarmer().getApprovedMaxQty(), "requestedQty", q.getRequestedQty(), "reason", q.getReason(), "status", q.getStatus(), "createdAt", q.getCreatedAt())).toList();
        }
        @PutMapping("/quantity-requests/{id}") void decide(@PathVariable Long id, @RequestBody Map<String, Object> b) { svc.decide(id, Boolean.TRUE.equals(b.get("approve"))); }
        @PutMapping("/users/{id}/status") void userStatus(@PathVariable Long id, @RequestBody Map<String, String> b) {
            String s = b.get("status");
            if (!"ACTIVE".equals(s) && !"INACTIVE".equals(s)) throw new ApiException(400, "Invalid status.");
            User u = users.findById(id).orElseThrow(() -> new ApiException(404, "User not found."));
            if (u.getRole() == Role.ADMIN) throw new ApiException(403, "Admin accounts cannot be deactivated here.");
            u.setStatus(s); users.save(u);
        }
        @GetMapping("/centres") List<Map<String, Object>> centreList() { return centres.findAll().stream().map(svc::centreView).toList(); }
        Centre apply(Centre c, CentreReq r) {
            c.setCentreCode(r.centreCode().trim()); c.setCentreName(r.centreName().trim()); c.setAddress(r.address()); c.setVillage(r.village()); c.setDistrict(r.district());
            c.setContactNumber(r.contactNumber()); if (r.openTime() != null) c.setOpenTime(r.openTime()); if (r.closeTime() != null) c.setCloseTime(r.closeTime());
            c.setMaxDailyCapacity(r.maxDailyCapacity()); if (r.status() != null) c.setStatus(r.status());
            return centres.save(c);
        }
        @PostMapping("/centres") @ResponseStatus(HttpStatus.CREATED) Map<String, Object> addCentre(@Valid @RequestBody CentreReq r) { return svc.centreView(apply(new Centre(), r)); }
        @PutMapping("/centres/{id}") Map<String, Object> editCentre(@PathVariable Long id, @Valid @RequestBody CentreReq r) {
            return svc.centreView(apply(centres.findById(id).orElseThrow(() -> new ApiException(404, "Centre not found.")), r));
        }
        @DeleteMapping("/centres/{id}") void closeCentre(@PathVariable Long id) { // deactivate, never hard-delete (bookings reference it)
            Centre c = centres.findById(id).orElseThrow(() -> new ApiException(404, "Centre not found.")); c.setStatus("CLOSED"); centres.save(c);
        }
        @GetMapping("/staff") List<Map<String, Object>> staff() {
            return users.findByRole(Role.STAFF).stream().map(u -> m("id", u.getId(), "name", u.getName(), "mobile", u.getMobile(), "status", u.getStatus(), "centreId", u.getCentreId(),
                "centre", u.getCentreId() == null ? "" : centres.findById(u.getCentreId()).map(Centre::getCentreName).orElse(""))).toList();
        }
        @PostMapping("/staff") @ResponseStatus(HttpStatus.CREATED) void addStaff(@Valid @RequestBody StaffReq r) {
            if (users.findByMobile(r.mobile()).isPresent()) throw new ApiException(409, "This mobile number is already registered.");
            centres.findById(r.centreId()).orElseThrow(() -> new ApiException(404, "Centre not found."));
            User u = new User(); u.setName(r.name().trim()); u.setMobile(r.mobile()); u.setPasswordHash(pe.encode(r.password())); u.setRole(Role.STAFF); u.setCentreId(r.centreId()); users.save(u);
        }
        @GetMapping("/bookings") List<Map<String, Object>> allBookings(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                @RequestParam(required = false) String status, @RequestParam(required = false) Long centreId) {
            LocalDate t = LocalDate.now();
            return bookings.findByBookingDateBetween(date != null ? date : t.minusDays(60), date != null ? date : t.plusDays(60)).stream()
                .filter(b -> status == null || status.isBlank() || b.getBookingStatus().name().equals(status))
                .filter(b -> centreId == null || b.getCentre().getId().equals(centreId))
                .sorted(Comparator.comparing(Booking::getCreatedAt).reversed()).limit(500).map(svc::view).toList();
        }
        @GetMapping("/reports") Map<String, Object> report(@RequestParam String type,
                @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from, @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
            var bs = bookings.findByBookingDateBetween(from, to);
            List<String> cols; var rows = new ArrayList<List<Object>>();
            switch (type) {
                case "centre" -> { cols = List.of("Centre", "Bookings", "Completed", "Cancelled", "Quantity (bags)");
                    bs.stream().collect(Collectors.groupingBy(b -> b.getCentre().getCentreName(), TreeMap::new, Collectors.toList()))
                        .forEach((k, l) -> rows.add(List.of(k, l.size(), cnt(l, COMPLETED), cnt(l, CANCELLED), qty(l)))); }
                case "completed" -> { cols = List.of("Booking ID", "Token", "Date", "Centre", "Farmer", "Vehicle", "Quantity (bags)");
                    bs.stream().filter(b -> b.getBookingStatus() == COMPLETED).sorted(Comparator.comparing(Booking::getBookingDate))
                        .forEach(b -> rows.add(List.of(b.getBookingId(), b.getTokenNumber(), b.getBookingDate().toString(), b.getCentre().getCentreName(),
                            b.getFarmer().getUser().getName(), b.getVehicleNumber(), b.getPaddyQuantity()))); }
                case "utilization" -> { cols = List.of("Centre", "Slots", "Capacity", "Booked", "Utilization %");
                    slots.findBySlotDateBetween(from, to).stream().collect(Collectors.groupingBy(s -> s.getCentre().getCentreName(), TreeMap::new, Collectors.toList()))
                        .forEach((k, l) -> { int cap = l.stream().mapToInt(Slot::getCapacity).sum(), bk = l.stream().mapToInt(Slot::getBookedCount).sum();
                            rows.add(List.of(k, l.size(), cap, bk, cap == 0 ? 0 : Math.round(bk * 100f / cap))); }); }
                case "farmer" -> { cols = List.of("Farmer ID", "Farmer", "Bookings", "Completed", "Quantity (bags)");
                    bs.stream().collect(Collectors.groupingBy(b -> b.getFarmer().getFarmerId() + "|" + b.getFarmer().getUser().getName(), TreeMap::new, Collectors.toList()))
                        .forEach((k, l) -> rows.add(List.of(k.split("\\|")[0], k.split("\\|")[1], l.size(), cnt(l, COMPLETED), qty(l)))); }
                default -> { cols = List.of("Date", "Bookings", "Completed", "Cancelled", "Quantity (bags)");
                    bs.stream().collect(Collectors.groupingBy(b -> b.getBookingDate().toString(), TreeMap::new, Collectors.toList()))
                        .forEach((k, l) -> rows.add(List.of(k, l.size(), cnt(l, COMPLETED), cnt(l, CANCELLED), qty(l)))); }
            }
            return m("columns", cols, "rows", rows);
        }
    }
}
