package com.agri.ppms.repository;

import com.agri.ppms.entity.ProcurementCentre;
import com.agri.ppms.entity.Slot;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface SlotRepository extends JpaRepository<Slot, Long> {

    List<Slot> findByCentreAndSlotDateOrderByStartTimeAsc(ProcurementCentre centre, LocalDate slotDate);

    List<Slot> findByCentreIdAndSlotDateOrderByStartTimeAsc(Long centreId, LocalDate slotDate);

    List<Slot> findByCentreIdAndSlotDateBetweenOrderBySlotDateAscStartTimeAsc(Long centreId, LocalDate startDate, LocalDate endDate);

    Optional<Slot> findByCentreAndSlotDateAndStartTime(ProcurementCentre centre, LocalDate slotDate, String startTime);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Slot s WHERE s.id = :id")
    Optional<Slot> findByIdWithLock(@Param("id") Long id);

    @Query("SELECT COUNT(s) FROM Slot s WHERE s.centre.id = :centreId AND s.slotDate = :date AND s.status != 'FULL' AND s.status != 'CLOSED'")
    long countAvailableSlotsToday(@Param("centreId") Long centreId, @Param("date") LocalDate date);
}
