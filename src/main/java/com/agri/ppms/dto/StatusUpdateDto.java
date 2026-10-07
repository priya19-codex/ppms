package com.agri.ppms.dto;

import com.agri.ppms.entity.Booking;
import jakarta.validation.constraints.NotNull;

public class StatusUpdateDto {

    @NotNull(message = "Status is required")
    private Booking.BookingStatus status;

    private String remarks;

    public StatusUpdateDto() {}

    public StatusUpdateDto(Booking.BookingStatus status, String remarks) {
        this.status = status;
        this.remarks = remarks;
    }

    public Booking.BookingStatus getStatus() { return status; }
    public void setStatus(Booking.BookingStatus status) { this.status = status; }

    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
}
