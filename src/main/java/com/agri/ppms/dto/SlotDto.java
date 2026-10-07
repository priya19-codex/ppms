package com.agri.ppms.dto;

import com.agri.ppms.entity.Slot;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public class SlotDto {
    private Long id;
    private Long centreId;
    private String centreName;

    @NotNull(message = "Slot date is required")
    private LocalDate slotDate;

    @NotBlank(message = "Start time is required")
    private String startTime;

    @NotBlank(message = "End time is required")
    private String endTime;

    @NotNull(message = "Capacity is required")
    @Min(value = 1, message = "Capacity must be at least 1")
    private Integer capacity;

    private Integer bookedCount = 0;
    private Integer availableSpaces;
    private Slot.SlotStatus status;

    public SlotDto() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getCentreId() { return centreId; }
    public void setCentreId(Long centreId) { this.centreId = centreId; }

    public String getCentreName() { return centreName; }
    public void setCentreName(String centreName) { this.centreName = centreName; }

    public LocalDate getSlotDate() { return slotDate; }
    public void setSlotDate(LocalDate slotDate) { this.slotDate = slotDate; }

    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }

    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }

    public Integer getCapacity() { return capacity; }
    public void setCapacity(Integer capacity) { this.capacity = capacity; }

    public Integer getBookedCount() { return bookedCount; }
    public void setBookedCount(Integer bookedCount) { this.bookedCount = bookedCount; }

    public Integer getAvailableSpaces() { return availableSpaces; }
    public void setAvailableSpaces(Integer availableSpaces) { this.availableSpaces = availableSpaces; }

    public Slot.SlotStatus getStatus() { return status; }
    public void setStatus(Slot.SlotStatus status) { this.status = status; }
}
