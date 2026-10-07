package com.agri.ppms.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class QuantityRequestDto {

    @NotNull(message = "Requested quantity is required")
    @Min(value = 1, message = "Requested quantity must be at least 1 bag")
    private Integer requestedQuantity;

    @NotBlank(message = "Reason for additional quantity is required")
    private String reason;

    public QuantityRequestDto() {}

    public QuantityRequestDto(Integer requestedQuantity, String reason) {
        this.requestedQuantity = requestedQuantity;
        this.reason = reason;
    }

    public Integer getRequestedQuantity() { return requestedQuantity; }
    public void setRequestedQuantity(Integer requestedQuantity) { this.requestedQuantity = requestedQuantity; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
