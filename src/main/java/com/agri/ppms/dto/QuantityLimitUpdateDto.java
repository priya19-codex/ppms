package com.agri.ppms.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class QuantityLimitUpdateDto {

    @NotNull(message = "Approved quantity is required")
    @Min(value = 0, message = "Approved quantity cannot be negative")
    private Integer approvedQuantity;

    public QuantityLimitUpdateDto() {}

    public QuantityLimitUpdateDto(Integer approvedQuantity) {
        this.approvedQuantity = approvedQuantity;
    }

    public Integer getApprovedQuantity() { return approvedQuantity; }
    public void setApprovedQuantity(Integer approvedQuantity) { this.approvedQuantity = approvedQuantity; }
}
