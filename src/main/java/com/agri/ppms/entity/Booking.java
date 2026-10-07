package com.agri.ppms.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "bookings")
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 40)
    private String bookingId; // e.g. PB20261007010001

    @Column(nullable = false, length = 20)
    private String tokenNumber; // e.g. A001

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "farmer_id", nullable = false)
    private Farmer farmer;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "centre_id", nullable = false)
    private ProcurementCentre centre;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "slot_id", nullable = false)
    private Slot slot;

    @Column(nullable = false)
    private LocalDate slotDate;

    @Column(nullable = false, length = 40)
    private String slotTimeRange; // e.g. 08:00 AM - 09:00 AM

    @Column(nullable = false, length = 30)
    private String vehicleNumber;

    @Column(nullable = false, length = 50)
    private String vehicleType; // Tractor, Mini Truck, Lorry, Tempo, Bullock Cart

    @Column(nullable = false, length = 50)
    private String paddyType; // Ponni, ADT-45, CO-51

    @Column(nullable = false)
    private int quantityBags;

    @Column(nullable = false, length = 20)
    private String contactNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 25)
    private BookingStatus status = BookingStatus.CONFIRMED;

    @Column(columnDefinition = "TEXT")
    private String qrCodeData;

    @Column(nullable = false)
    private LocalDateTime bookedAt = LocalDateTime.now();

    private LocalDateTime arrivedAt;
    private LocalDateTime waitingAt;
    private LocalDateTime unloadingAt;
    private LocalDateTime completedAt;
    private LocalDateTime cancelledAt;

    @Column(length = 255)
    private String cancellationReason;

    public enum BookingStatus {
        CONFIRMED,
        ARRIVED,
        WAITING,
        UNLOADING,
        COMPLETED,
        CANCELLED
    }

    public Booking() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getBookingId() { return bookingId; }
    public void setBookingId(String bookingId) { this.bookingId = bookingId; }

    public String getTokenNumber() { return tokenNumber; }
    public void setTokenNumber(String tokenNumber) { this.tokenNumber = tokenNumber; }

    public Farmer getFarmer() { return farmer; }
    public void setFarmer(Farmer farmer) { this.farmer = farmer; }

    public ProcurementCentre getCentre() { return centre; }
    public void setCentre(ProcurementCentre centre) { this.centre = centre; }

    public Slot getSlot() { return slot; }
    public void setSlot(Slot slot) { this.slot = slot; }

    public LocalDate getSlotDate() { return slotDate; }
    public void setSlotDate(LocalDate slotDate) { this.slotDate = slotDate; }

    public String getSlotTimeRange() { return slotTimeRange; }
    public void setSlotTimeRange(String slotTimeRange) { this.slotTimeRange = slotTimeRange; }

    public String getVehicleNumber() { return vehicleNumber; }
    public void setVehicleNumber(String vehicleNumber) { this.vehicleNumber = vehicleNumber; }

    public String getVehicleType() { return vehicleType; }
    public void setVehicleType(String vehicleType) { this.vehicleType = vehicleType; }

    public String getPaddyType() { return paddyType; }
    public void setPaddyType(String paddyType) { this.paddyType = paddyType; }

    public int getQuantityBags() { return quantityBags; }
    public void setQuantityBags(int quantityBags) { this.quantityBags = quantityBags; }

    public String getContactNumber() { return contactNumber; }
    public void setContactNumber(String contactNumber) { this.contactNumber = contactNumber; }

    public BookingStatus getStatus() { return status; }
    public void setStatus(BookingStatus status) { this.status = status; }

    public String getQrCodeData() { return qrCodeData; }
    public void setQrCodeData(String qrCodeData) { this.qrCodeData = qrCodeData; }

    public LocalDateTime getBookedAt() { return bookedAt; }
    public void setBookedAt(LocalDateTime bookedAt) { this.bookedAt = bookedAt; }

    public LocalDateTime getArrivedAt() { return arrivedAt; }
    public void setArrivedAt(LocalDateTime arrivedAt) { this.arrivedAt = arrivedAt; }

    public LocalDateTime getWaitingAt() { return waitingAt; }
    public void setWaitingAt(LocalDateTime waitingAt) { this.waitingAt = waitingAt; }

    public LocalDateTime getUnloadingAt() { return unloadingAt; }
    public void setUnloadingAt(LocalDateTime unloadingAt) { this.unloadingAt = unloadingAt; }

    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }

    public LocalDateTime getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(LocalDateTime cancelledAt) { this.cancelledAt = cancelledAt; }

    public String getCancellationReason() { return cancellationReason; }
    public void setCancellationReason(String cancellationReason) { this.cancellationReason = cancellationReason; }
}
