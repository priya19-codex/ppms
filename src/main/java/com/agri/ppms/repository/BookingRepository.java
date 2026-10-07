package com.agri.ppms.repository;

import com.agri.ppms.entity.Booking;
import com.agri.ppms.entity.Farmer;
import com.agri.ppms.entity.ProcurementCentre;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    Optional<Booking> findByBookingId(String bookingId);

    Optional<Booking> findByTokenNumberAndSlotDateAndCentreId(String tokenNumber, LocalDate slotDate, Long centreId);

    List<Booking> findByFarmerOrderBySlotDateDescBookedAtDesc(Farmer farmer);

    List<Booking> findByFarmerIdOrderBySlotDateDescBookedAtDesc(Long farmerId);

    List<Booking> findByCentreAndSlotDateOrderByTokenNumberAsc(ProcurementCentre centre, LocalDate slotDate);

    List<Booking> findByCentreIdAndSlotDateOrderByTokenNumberAsc(Long centreId, LocalDate slotDate);

    List<Booking> findBySlotDateBetweenOrderBySlotDateDescBookedAtDesc(LocalDate startDate, LocalDate endDate);

    @Query("SELECT COUNT(b) FROM Booking b WHERE b.centre.id = :centreId AND b.slotDate = :date AND b.status IN ('ARRIVED', 'WAITING', 'UNLOADING')")
    long countCurrentQueueByCentreAndDate(@Param("centreId") Long centreId, @Param("date") LocalDate date);

    @Query("SELECT COUNT(b) FROM Booking b WHERE b.centre.id = :centreId AND b.slotDate = :date AND b.status = :status")
    long countByCentreAndDateAndStatus(@Param("centreId") Long centreId, @Param("date") LocalDate date, @Param("status") Booking.BookingStatus status);

    @Query("SELECT COUNT(b) FROM Booking b WHERE b.slotDate = :date AND b.status = :status")
    long countByDateAndStatus(@Param("date") LocalDate date, @Param("status") Booking.BookingStatus status);

    @Query("SELECT COUNT(b) FROM Booking b WHERE b.slotDate = :date")
    long countByDate(@Param("date") LocalDate date);

    @Query("SELECT COUNT(b) FROM Booking b WHERE b.status = :status")
    long countByStatus(@Param("status") Booking.BookingStatus status);

    @Query("SELECT COALESCE(SUM(b.quantityBags), 0) FROM Booking b WHERE b.status = 'COMPLETED'")
    long sumCompletedPaddyQuantity();

    @Query("SELECT COALESCE(SUM(b.quantityBags), 0) FROM Booking b WHERE b.centre.id = :centreId AND b.status = 'COMPLETED'")
    long sumCompletedPaddyQuantityByCentre(@Param("centreId") Long centreId);

    @Query("SELECT COUNT(b) FROM Booking b WHERE b.centre.id = :centreId AND b.slotDate = :date")
    long countBookingsByCentreAndDate(@Param("centreId") Long centreId, @Param("date") LocalDate date);

    @Query("SELECT b FROM Booking b WHERE " +
           "(:centreId IS NULL OR b.centre.id = :centreId) AND " +
           "(:status IS NULL OR b.status = :status) AND " +
           "(:startDate IS NULL OR b.slotDate >= :startDate) AND " +
           "(:endDate IS NULL OR b.slotDate <= :endDate) " +
           "ORDER BY b.slotDate DESC, b.bookedAt DESC")
    List<Booking> filterBookings(@Param("centreId") Long centreId,
                                 @Param("status") Booking.BookingStatus status,
                                 @Param("startDate") LocalDate startDate,
                                 @Param("endDate") LocalDate endDate);
}
