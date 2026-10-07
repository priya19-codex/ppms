package com.agri.ppms.service;

import com.agri.ppms.dto.SlotDto;
import com.agri.ppms.entity.ProcurementCentre;
import com.agri.ppms.entity.Slot;
import com.agri.ppms.repository.ProcurementCentreRepository;
import com.agri.ppms.repository.SlotRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SlotService {

    private final SlotRepository slotRepository;
    private final ProcurementCentreRepository centreRepository;

    private static final String[][] DEFAULT_TIME_SLOTS = {
            {"08:00 AM", "09:00 AM"},
            {"09:00 AM", "10:00 AM"},
            {"10:00 AM", "11:00 AM"},
            {"02:00 PM", "03:00 PM"},
            {"03:00 PM", "04:00 PM"}
    };

    public SlotService(SlotRepository slotRepository, ProcurementCentreRepository centreRepository) {
        this.slotRepository = slotRepository;
        this.centreRepository = centreRepository;
    }

    @Transactional
    public List<SlotDto> getSlotsForCentreAndDate(Long centreId, LocalDate date) {
        ensureSlotsExist(centreId, date);
        List<Slot> slots = slotRepository.findByCentreIdAndSlotDateOrderByStartTimeAsc(centreId, date);
        return slots.stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional
    public Map<String, List<SlotDto>> getSlotsForNext7Days(Long centreId) {
        LocalDate today = LocalDate.now();
        Map<String, List<SlotDto>> result = new LinkedHashMap<>();

        for (int i = 0; i < 7; i++) {
            LocalDate d = today.plusDays(i);
            ensureSlotsExist(centreId, d);
            List<Slot> slots = slotRepository.findByCentreIdAndSlotDateOrderByStartTimeAsc(centreId, d);
            result.put(d.toString(), slots.stream().map(this::toDto).collect(Collectors.toList()));
        }

        return result;
    }

    @Transactional
    public void ensureSlotsExist(Long centreId, LocalDate date) {
        ProcurementCentre centre = centreRepository.findById(centreId).orElse(null);
        if (centre == null) return;

        List<Slot> existing = slotRepository.findByCentreIdAndSlotDateOrderByStartTimeAsc(centreId, date);
        if (existing.isEmpty()) {
            for (String[] timeRange : DEFAULT_TIME_SLOTS) {
                Slot slot = new Slot(centre, date, timeRange[0], timeRange[1], 10);
                slotRepository.save(slot);
            }
        }
    }

    public SlotDto getSlotById(Long slotId) {
        Slot slot = slotRepository.findById(slotId)
                .orElseThrow(() -> new IllegalArgumentException("Slot not found with ID: " + slotId));
        return toDto(slot);
    }

    @Transactional
    public SlotDto updateSlot(Long slotId, Integer capacity, Slot.SlotStatus status) {
        Slot slot = slotRepository.findById(slotId)
                .orElseThrow(() -> new IllegalArgumentException("Slot not found with ID: " + slotId));

        if (capacity != null && capacity >= slot.getBookedCount()) {
            slot.setCapacity(capacity);
        }
        if (status != null) {
            slot.setStatus(status);
        } else {
            slot.updateCalculatedStatus();
        }

        slot = slotRepository.save(slot);
        return toDto(slot);
    }

    private SlotDto toDto(Slot slot) {
        SlotDto dto = new SlotDto();
        dto.setId(slot.getId());
        dto.setCentreId(slot.getCentre().getId());
        dto.setCentreName(slot.getCentre().getName());
        dto.setSlotDate(slot.getSlotDate());
        dto.setStartTime(slot.getStartTime());
        dto.setEndTime(slot.getEndTime());
        dto.setCapacity(slot.getCapacity());
        dto.setBookedCount(slot.getBookedCount());
        dto.setAvailableSpaces(slot.getAvailableSpaces());
        dto.setStatus(slot.getStatus());
        return dto;
    }
}
