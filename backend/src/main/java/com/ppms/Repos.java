package com.ppms;

import com.ppms.Model.*;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.*;
import java.util.*;

public interface Repos {
    interface Users extends JpaRepository<User, Long> {
        Optional<User> findByMobile(String mobile);
        List<User> findByRole(Role role);
    }
    interface Farmers extends JpaRepository<Farmer, Long> {
        Optional<Farmer> findByUserId(Long userId);
        boolean existsByFarmerId(String farmerId);
    }
    interface Centres extends JpaRepository<Centre, Long> {
        /** Row lock: serialises bookings per centre (token numbering + capacity checks). */
        @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select c from Centre c where c.id=:id")
        Optional<Centre> lockById(@Param("id") Long id);
    }
    interface Slots extends JpaRepository<Slot, Long> {
        List<Slot> findByCentreIdAndSlotDateOrderByStartTime(Long centreId, LocalDate d);
        List<Slot> findByCentreIdAndSlotDateGreaterThanEqualOrderBySlotDateAscStartTimeAsc(Long centreId, LocalDate d);
        List<Slot> findBySlotDateBetween(LocalDate a, LocalDate b);
        @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select s from Slot s where s.id=:id")
        Optional<Slot> lockById(@Param("id") Long id);
        @Query("select s.centre.id from Slot s where s.id=:id") Optional<Long> centreIdOf(@Param("id") Long id);
        /** Atomic guard: succeeds (returns 1) only while a seat is free and the slot is open. */
        @Modifying @Query("update Slot s set s.bookedCount=s.bookedCount+1 where s.id=:id and s.status='OPEN' and s.bookedCount<s.capacity")
        int reserve(@Param("id") Long id);
        @Modifying @Query("update Slot s set s.bookedCount=s.bookedCount-1 where s.id=:id and s.bookedCount>0")
        int release(@Param("id") Long id);
        @Query("select coalesce(sum(s.capacity-s.bookedCount),0L) from Slot s where s.centre.id=:c and s.slotDate=:d and s.status='OPEN' and s.startTime>:t")
        Number availableOn(@Param("c") Long c, @Param("d") LocalDate d, @Param("t") LocalTime t);
    }
    interface Bookings extends JpaRepository<Booking, Long> {
        Optional<Booking> findByBookingId(String bookingId);
        List<Booking> findByFarmerIdOrderByCreatedAtDesc(Long farmerId);
        List<Booking> findByCentreIdAndBookingDateOrderByTokenNumber(Long centreId, LocalDate d);
        List<Booking> findByBookingDateBetween(LocalDate a, LocalDate b);
        List<Booking> findByBookingDateAndBookingStatusAndReminderSentFalse(LocalDate d, BStatus s);
        long countByCentreIdAndBookingDate(Long centreId, LocalDate d);
        long countByCentreIdAndBookingDateAndBookingStatusIn(Long centreId, LocalDate d, Collection<BStatus> s);
        boolean existsByFarmerIdAndSlotIdAndBookingStatusNot(Long farmerId, Long slotId, BStatus s);
        @Query("select coalesce(sum(b.paddyQuantity),0L) from Booking b where b.farmer.id=:f and b.bookingStatus<>:st")
        Number activeQty(@Param("f") Long farmerId, @Param("st") BStatus st);
        @Query("select b.centre.id from Booking b where b.bookingId=:id") Optional<Long> centreIdOf(@Param("id") String id);
    }
    interface Notifications extends JpaRepository<Notification, Long> {
        List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);
        List<Notification> findByUserIdAndIsReadFalse(Long userId);
    }
    interface QuantityRequests extends JpaRepository<QuantityRequest, Long> {
        List<QuantityRequest> findByFarmerIdOrderByCreatedAtDesc(Long farmerId);
        List<QuantityRequest> findAllByOrderByCreatedAtDesc();
    }
}
