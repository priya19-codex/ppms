package com.agri.ppms.dto;

import jakarta.validation.constraints.*;

public class BookingCreateRequest {

    @NotNull(message = "Slot selection is required")
    private Long slotId;

    @NotBlank(message = "Vehicle number is required")
    private String vehicleNumber;

    @NotBlank(message = "Vehicle type is required")
    private String vehicleType; // Tractor, Mini Truck, Lorry, Tempo, Bullock Cart

    @NotBlank(message = "Paddy type is required")
    private String paddyType; // Ponni, ADT-45, CO-51

    @NotNull(message = "Quantity in bags is required")
    @Min(value = 1, message = "Quantity must be at least 1 bag")
    private Integer quantityBags;

    @NotBlank(message = "Contact number is required")
    @Pattern(regexp = "^[0-9]{10}$", message = "Contact number must be 10 digits")
    private String contactNumber;

    public BookingCreateRequest() {}

    public Long getSlotId() { return slotId; }
    public void setSlotId(Long slotId) { this.slotId = slotId; }

    public String getVehicleNumber() { return vehicleNumber; }
    public void setVehicleNumber(String vehicleNumber) { this.vehicleNumber = vehicleNumber; }

    public String getVehicleType() { return vehicleType; }
    public void setVehicleType(String vehicleType) { this.vehicleType = vehicleType; }

    public String getPaddyType() { return paddyType; }
    public void setPaddyType(String paddyType) { this.paddyType = paddyType; }

    public Integer getQuantityBags() { return quantityBags; }
    public void setQuantityBags(Integer quantityBags) { this.quantityBags = quantityBags; }

    public String getContactNumber() { return contactNumber; }
    public void setContactNumber(String contactNumber) { this.contactNumber = contactNumber; }
}
