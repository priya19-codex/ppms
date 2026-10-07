package com.agri.ppms.dto;

import com.agri.ppms.entity.Booking;
import java.util.List;
import java.util.Map;

public class DashboardStatsDto {
    // Farmer stats
    private long totalBookings;
    private long activeBookings;
    private long completedBookings;
    private long cancelledBookings;
    private int approvedQuantity;
    private int usedQuantity;
    private int remainingQuantity;
    private Booking nextSlotBooking;
    private String currentBookingStatus;

    // Staff stats
    private long todayTotalBookings;
    private long todayConfirmed;
    private long todayArrived;
    private long todayWaiting;
    private long todayUnloading;
    private long todayCompleted;
    private String centreName;
    private String centreCode;

    // Admin stats
    private long totalFarmers;
    private long totalCentres;
    private long todayAllBookings;
    private long totalCompletedQuantityBags;
    private double overallSlotUtilization;
    private Map<String, Long> bookingsByStatus;
    private Map<String, Long> bookingsByCentre;
    private List<Map<String, Object>> dailyTrends;

    public DashboardStatsDto() {}

    public long getTotalBookings() { return totalBookings; }
    public void setTotalBookings(long totalBookings) { this.totalBookings = totalBookings; }

    public long getActiveBookings() { return activeBookings; }
    public void setActiveBookings(long activeBookings) { this.activeBookings = activeBookings; }

    public long getCompletedBookings() { return completedBookings; }
    public void setCompletedBookings(long completedBookings) { this.completedBookings = completedBookings; }

    public long getCancelledBookings() { return cancelledBookings; }
    public void setCancelledBookings(long cancelledBookings) { this.cancelledBookings = cancelledBookings; }

    public int getApprovedQuantity() { return approvedQuantity; }
    public void setApprovedQuantity(int approvedQuantity) { this.approvedQuantity = approvedQuantity; }

    public int getUsedQuantity() { return usedQuantity; }
    public void setUsedQuantity(int usedQuantity) { this.usedQuantity = usedQuantity; }

    public int getRemainingQuantity() { return remainingQuantity; }
    public void setRemainingQuantity(int remainingQuantity) { this.remainingQuantity = remainingQuantity; }

    public Booking getNextSlotBooking() { return nextSlotBooking; }
    public void setNextSlotBooking(Booking nextSlotBooking) { this.nextSlotBooking = nextSlotBooking; }

    public String getCurrentBookingStatus() { return currentBookingStatus; }
    public void setCurrentBookingStatus(String currentBookingStatus) { this.currentBookingStatus = currentBookingStatus; }

    public long getTodayTotalBookings() { return todayTotalBookings; }
    public void setTodayTotalBookings(long todayTotalBookings) { this.todayTotalBookings = todayTotalBookings; }

    public long getTodayConfirmed() { return todayConfirmed; }
    public void setTodayConfirmed(long todayConfirmed) { this.todayConfirmed = todayConfirmed; }

    public long getTodayArrived() { return todayArrived; }
    public void setTodayArrived(long todayArrived) { this.todayArrived = todayArrived; }

    public long getTodayWaiting() { return todayWaiting; }
    public void setTodayWaiting(long todayWaiting) { this.todayWaiting = todayWaiting; }

    public long getTodayUnloading() { return todayUnloading; }
    public void setTodayUnloading(long todayUnloading) { this.todayUnloading = todayUnloading; }

    public long getTodayCompleted() { return todayCompleted; }
    public void setTodayCompleted(long todayCompleted) { this.todayCompleted = todayCompleted; }

    public String getCentreName() { return centreName; }
    public void setCentreName(String centreName) { this.centreName = centreName; }

    public String getCentreCode() { return centreCode; }
    public void setCentreCode(String centreCode) { this.centreCode = centreCode; }

    public long getTotalFarmers() { return totalFarmers; }
    public void setTotalFarmers(long totalFarmers) { this.totalFarmers = totalFarmers; }

    public long getTotalCentres() { return totalCentres; }
    public void setTotalCentres(long totalCentres) { this.totalCentres = totalCentres; }

    public long getTodayAllBookings() { return todayAllBookings; }
    public void setTodayAllBookings(long todayAllBookings) { this.todayAllBookings = todayAllBookings; }

    public long getTotalCompletedQuantityBags() { return totalCompletedQuantityBags; }
    public void setTotalCompletedQuantityBags(long totalCompletedQuantityBags) { this.totalCompletedQuantityBags = totalCompletedQuantityBags; }

    public double getOverallSlotUtilization() { return overallSlotUtilization; }
    public void setOverallSlotUtilization(double overallSlotUtilization) { this.overallSlotUtilization = overallSlotUtilization; }

    public Map<String, Long> getBookingsByStatus() { return bookingsByStatus; }
    public void setBookingsByStatus(Map<String, Long> bookingsByStatus) { this.bookingsByStatus = bookingsByStatus; }

    public Map<String, Long> getBookingsByCentre() { return bookingsByCentre; }
    public void setBookingsByCentre(Map<String, Long> bookingsByCentre) { this.bookingsByCentre = bookingsByCentre; }

    public List<Map<String, Object>> getDailyTrends() { return dailyTrends; }
    public void setDailyTrends(List<Map<String, Object>> dailyTrends) { this.dailyTrends = dailyTrends; }
}
