package com.agri.ppms.service;

import com.agri.ppms.dto.CentreDto;
import com.agri.ppms.entity.ProcurementCentre;
import com.agri.ppms.entity.StaffAssignment;
import com.agri.ppms.repository.BookingRepository;
import com.agri.ppms.repository.ProcurementCentreRepository;
import com.agri.ppms.repository.SlotRepository;
import com.agri.ppms.repository.StaffAssignmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CentreService {

    private final ProcurementCentreRepository centreRepository;
    private final SlotRepository slotRepository;
    private final BookingRepository bookingRepository;
    private final StaffAssignmentRepository staffAssignmentRepository;

    public CentreService(ProcurementCentreRepository centreRepository,
                         SlotRepository slotRepository,
                         BookingRepository bookingRepository,
                         StaffAssignmentRepository staffAssignmentRepository) {
        this.centreRepository = centreRepository;
        this.slotRepository = slotRepository;
        this.bookingRepository = bookingRepository;
        this.staffAssignmentRepository = staffAssignmentRepository;
    }

    public List<CentreDto> getAllCentres(String search) {
        List<ProcurementCentre> centres;
        if (search != null && !search.trim().isEmpty()) {
            centres = centreRepository.searchCentres(search.trim());
        } else {
            centres = centreRepository.findAll();
        }

        LocalDate today = LocalDate.now();
        return centres.stream().map(c -> toDto(c, today)).collect(Collectors.toList());
    }

    public CentreDto getCentreById(Long id) {
        ProcurementCentre centre = centreRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Procurement centre not found with ID: " + id));
        return toDto(centre, LocalDate.now());
    }

    public ProcurementCentre getCentreEntity(Long id) {
        return centreRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Procurement centre not found with ID: " + id));
    }

    @Transactional
    public CentreDto createCentre(CentreDto dto) {
        if (centreRepository.findByCode(dto.getCode()).isPresent()) {
            throw new IllegalArgumentException("Centre code already exists: " + dto.getCode());
        }
        ProcurementCentre c = new ProcurementCentre(
                dto.getCode(),
                dto.getName(),
                dto.getAddress(),
                dto.getVillage(),
                dto.getDistrict(),
                dto.getContactNumber(),
                dto.getOpeningTime() != null ? dto.getOpeningTime() : "08:00 AM",
                dto.getClosingTime() != null ? dto.getClosingTime() : "06:00 PM",
                dto.getMaxDailyCapacity() != null ? dto.getMaxDailyCapacity() : 200
        );
        if (dto.getStatus() != null) {
            c.setStatus(dto.getStatus());
        }
        c = centreRepository.save(c);
        return toDto(c, LocalDate.now());
    }

    @Transactional
    public CentreDto updateCentre(Long id, CentreDto dto) {
        ProcurementCentre c = getCentreEntity(id);
        c.setName(dto.getName());
        c.setAddress(dto.getAddress());
        c.setVillage(dto.getVillage());
        c.setDistrict(dto.getDistrict());
        c.setContactNumber(dto.getContactNumber());
        c.setOpeningTime(dto.getOpeningTime());
        c.setClosingTime(dto.getClosingTime());
        if (dto.getMaxDailyCapacity() != null) {
            c.setMaxDailyCapacity(dto.getMaxDailyCapacity());
        }
        if (dto.getStatus() != null) {
            c.setStatus(dto.getStatus());
        }
        c = centreRepository.save(c);
        return toDto(c, LocalDate.now());
    }

    @Transactional
    public CentreDto toggleCentreStatus(Long id) {
        ProcurementCentre c = getCentreEntity(id);
        if (c.getStatus() == ProcurementCentre.CentreStatus.OPEN) {
            c.setStatus(ProcurementCentre.CentreStatus.CLOSED);
        } else {
            c.setStatus(ProcurementCentre.CentreStatus.OPEN);
        }
        c = centreRepository.save(c);
        return toDto(c, LocalDate.now());
    }

    private CentreDto toDto(ProcurementCentre c, LocalDate today) {
        CentreDto dto = new CentreDto();
        dto.setId(c.getId());
        dto.setCode(c.getCode());
        dto.setName(c.getName());
        dto.setAddress(c.getAddress());
        dto.setVillage(c.getVillage());
        dto.setDistrict(c.getDistrict());
        dto.setState(c.getState());
        dto.setContactNumber(c.getContactNumber());
        dto.setOpeningTime(c.getOpeningTime());
        dto.setClosingTime(c.getClosingTime());
        dto.setMaxDailyCapacity(c.getMaxDailyCapacity());
        dto.setStatus(c.getStatus());

        // Live statistics
        dto.setAvailableSlotsToday(slotRepository.countAvailableSlotsToday(c.getId(), today));
        dto.setCurrentQueueCount(bookingRepository.countCurrentQueueByCentreAndDate(c.getId(), today));

        // Assigned staff
        List<StaffAssignment> assignments = staffAssignmentRepository.findByCentre(c);
        if (!assignments.isEmpty()) {
            dto.setAssignedStaffId(assignments.get(0).getUser().getId());
            dto.setAssignedStaffName(assignments.get(0).getUser().getFullName());
        }

        return dto;
    }
}
