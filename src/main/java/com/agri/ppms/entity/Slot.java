package com.agri.ppms.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "slots", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"centre_id", "slotDate", "startTime", "endTime"})
})
public class Slot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "centre_id", nullable = false)
    private ProcurementCentre centre;

    @Column(nullable = false)
    private LocalDate slotDate;

    @Column(nullable = false, length = 15)
    private String startTime; // e.g. "08:00 AM"

    @Column(nullable = false, length = 15)
    private String endTime; // e.g. "09:00 AM"

    @Column(nullable = false)
    private int capacity = 10;

    @Column(nullable = false)
    private int bookedCount = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SlotStatus status = SlotStatus.AVAILABLE;

    @Version
    private Long version;

    public enum SlotStatus {
        AVAILABLE,
        LIMITED,
        FULL,
        CLOSED
    }

    public Slot() {}

    public Slot(ProcurementCentre centre, LocalDate slotDate, String startTime, String endTime, int capacity) {
        this.centre = centre;
        this.slotDate = slotDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.capacity = capacity;
        this.bookedCount = 0;
        this.status = SlotStatus.AVAILABLE;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public ProcurementCentre getCentre() { return centre; }
    public void setCentre(ProcurementCentre centre) { this.centre = centre; }

    public LocalDate getSlotDate() { return slotDate; }
    public void setSlotDate(LocalDate slotDate) { this.slotDate = slotDate; }

    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }

    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }

    public int getCapacity() { return capacity; }
    public void setCapacity(int capacity) {
        this.capacity = capacity;
        updateCalculatedStatus();
    }

    public int getBookedCount() { return bookedCount; }
    public void setBookedCount(int bookedCount) {
        this.bookedCount = bookedCount;
        updateCalculatedStatus();
    }

    public int getAvailableSpaces() {
        return Math.max(0, capacity - bookedCount);
    }

    public SlotStatus getStatus() { return status; }
    public void setStatus(SlotStatus status) { this.status = status; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }

    public void updateCalculatedStatus() {
        if (this.status == SlotStatus.CLOSED) {
            return;
        }
        if (this.bookedCount >= this.capacity) {
            this.status = SlotStatus.FULL;
        } else if (this.capacity > 0 && (double) this.bookedCount / this.capacity >= 0.7) {
            this.status = SlotStatus.LIMITED;
        } else {
            this.status = SlotStatus.AVAILABLE;
        }
    }
}
