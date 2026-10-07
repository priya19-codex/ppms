package com.ppms;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.*;

/** All JPA entities (kept together for compactness). */
public class Model {
    public enum Role { FARMER, STAFF, ADMIN }
    public enum BStatus { CONFIRMED, ARRIVED, WAITING, UNLOADING, COMPLETED, CANCELLED }

    @Entity(name = "User") @Table(name = "users") @Getter @Setter
    public static class User {
        @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
        @Column(nullable = false) private String name;
        @Column(unique = true, nullable = false, length = 15) private String mobile;
        @JsonIgnore @Column(nullable = false) private String passwordHash;
        @Enumerated(EnumType.STRING) @Column(nullable = false) private Role role;
        @Column(nullable = false) private String status = "ACTIVE";
        private Long centreId; // staff members are bound to one centre
        private LocalDateTime createdAt = LocalDateTime.now();
    }

    @Entity(name = "Farmer") @Table(name = "farmers") @Getter @Setter
    public static class Farmer {
        @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
        @OneToOne(optional = false) @JoinColumn(name = "user_id", unique = true) private User user;
        @Column(unique = true, nullable = false) private String farmerId;
        @JsonIgnore private String aadhaarMasked; // only the last 4 digits are ever stored
        private String address, village, district, state;
        private Long preferredCentreId;
        private double landAcres;
        private int approvedMaxQty; // bags
    }

    @Entity(name = "Centre") @Table(name = "procurement_centres") @Getter @Setter
    public static class Centre {
        @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
        @Column(unique = true, nullable = false) private String centreCode;
        @Column(nullable = false) private String centreName;
        private String address, village, district, contactNumber;
        private LocalTime openTime = LocalTime.of(8, 0), closeTime = LocalTime.of(17, 0);
        private int maxDailyCapacity = 100;
        @Column(nullable = false) private String status = "OPEN"; // OPEN | CLOSED
    }

    @Entity(name = "Slot") @Getter @Setter
    @Table(name = "slots",
        uniqueConstraints = @UniqueConstraint(columnNames = {"centre_id", "slot_date", "start_time"}),
        indexes = @Index(columnList = "centre_id,slot_date"))
    public static class Slot {
        @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
        @ManyToOne(optional = false) @JoinColumn(name = "centre_id") private Centre centre;
        @Column(nullable = false) private LocalDate slotDate;
        @Column(nullable = false) private LocalTime startTime, endTime;
        private int capacity;
        private int bookedCount;
        @Column(nullable = false) private String status = "OPEN"; // OPEN | CLOSED

        @JsonProperty("available") public int available() { return Math.max(0, capacity - bookedCount); }

        /** Single source of truth for slot colour: AVAILABLE(green) LIMITED(yellow) FULL(red) CLOSED(grey). */
        @JsonProperty("state") public String state() {
            if (!"OPEN".equals(status) || LocalDateTime.of(slotDate, startTime).isBefore(LocalDateTime.now())) return "CLOSED";
            int a = available();
            if (a <= 0) return "FULL";
            return a <= Math.max(2, capacity * 3 / 10) ? "LIMITED" : "AVAILABLE";
        }
    }

    @Entity(name = "Booking") @Getter @Setter
    @Table(name = "bookings",
        uniqueConstraints = {@UniqueConstraint(columnNames = "booking_id"),
                             @UniqueConstraint(columnNames = {"centre_id", "booking_date", "token_number"})},
        indexes = {@Index(columnList = "farmer_id"), @Index(columnList = "slot_id"), @Index(columnList = "booking_date")})
    public static class Booking {
        @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
        @Column(name = "booking_id", nullable = false) private String bookingId;
        @Column(nullable = false) private String tokenNumber;
        @ManyToOne(optional = false) @JoinColumn(name = "farmer_id") private Farmer farmer;
        @ManyToOne(optional = false) @JoinColumn(name = "centre_id") private Centre centre;
        @ManyToOne(optional = false) @JoinColumn(name = "slot_id") private Slot slot;
        @Column(nullable = false) private LocalDate bookingDate;
        private String vehicleNumber, vehicleType, paddyType, contactNumber;
        private int paddyQuantity;
        @Enumerated(EnumType.STRING) @Column(nullable = false) private BStatus bookingStatus = BStatus.CONFIRMED;
        private boolean reminderSent;
        private LocalDateTime createdAt = LocalDateTime.now(), updatedAt = LocalDateTime.now();
    }

    @Entity(name = "Notification") @Table(name = "notifications", indexes = @Index(columnList = "user_id")) @Getter @Setter
    public static class Notification {
        @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
        @JsonIgnore @ManyToOne(optional = false) @JoinColumn(name = "user_id") private User user;
        private String title, type;
        @Column(length = 500) private String message;
        private boolean isRead;
        private LocalDateTime createdAt = LocalDateTime.now();
    }

    @Entity(name = "QuantityRequest") @Table(name = "quantity_requests") @Getter @Setter
    public static class QuantityRequest {
        @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
        @ManyToOne(optional = false) @JoinColumn(name = "farmer_id") private Farmer farmer;
        private int requestedQty;
        private String reason;
        private String status = "PENDING"; // PENDING | APPROVED | REJECTED
        private LocalDateTime createdAt = LocalDateTime.now();
    }
}
