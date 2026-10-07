package com.agri.ppms.dto;

import com.agri.ppms.entity.ProcurementCentre;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class CentreDto {
    private Long id;

    @NotBlank(message = "Centre code is required")
    private String code;

    @NotBlank(message = "Centre name is required")
    private String name;

    @NotBlank(message = "Address is required")
    private String address;

    @NotBlank(message = "Village is required")
    private String village;

    @NotBlank(message = "District is required")
    private String district;

    private String state = "Tamil Nadu";

    @NotBlank(message = "Contact number is required")
    private String contactNumber;

    @NotBlank(message = "Opening time is required")
    private String openingTime = "08:00 AM";

    @NotBlank(message = "Closing time is required")
    private String closingTime = "06:00 PM";

    @NotNull(message = "Max daily capacity is required")
    @Min(value = 1, message = "Max daily capacity must be at least 1")
    private Integer maxDailyCapacity = 200;

    private ProcurementCentre.CentreStatus status = ProcurementCentre.CentreStatus.OPEN;

    private Long assignedStaffId;
    private String assignedStaffName;

    // Live counts for UI cards
    private long availableSlotsToday;
    private long currentQueueCount;

    public CentreDto() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getVillage() { return village; }
    public void setVillage(String village) { this.village = village; }

    public String getDistrict() { return district; }
    public void setDistrict(String district) { this.district = district; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public String getContactNumber() { return contactNumber; }
    public void setContactNumber(String contactNumber) { this.contactNumber = contactNumber; }

    public String getOpeningTime() { return openingTime; }
    public void setOpeningTime(String openingTime) { this.openingTime = openingTime; }

    public String getClosingTime() { return closingTime; }
    public void setClosingTime(String closingTime) { this.closingTime = closingTime; }

    public Integer getMaxDailyCapacity() { return maxDailyCapacity; }
    public void setMaxDailyCapacity(Integer maxDailyCapacity) { this.maxDailyCapacity = maxDailyCapacity; }

    public ProcurementCentre.CentreStatus getStatus() { return status; }
    public void setStatus(ProcurementCentre.CentreStatus status) { this.status = status; }

    public Long getAssignedStaffId() { return assignedStaffId; }
    public void setAssignedStaffId(Long assignedStaffId) { this.assignedStaffId = assignedStaffId; }

    public String getAssignedStaffName() { return assignedStaffName; }
    public void setAssignedStaffName(String assignedStaffName) { this.assignedStaffName = assignedStaffName; }

    public long getAvailableSlotsToday() { return availableSlotsToday; }
    public void setAvailableSlotsToday(long availableSlotsToday) { this.availableSlotsToday = availableSlotsToday; }

    public long getCurrentQueueCount() { return currentQueueCount; }
    public void setCurrentQueueCount(long currentQueueCount) { this.currentQueueCount = currentQueueCount; }
}
