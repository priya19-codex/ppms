package com.agri.ppms.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "procurement_centres")
public class ProcurementCentre {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String code;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, length = 255)
    private String address;

    @Column(nullable = false, length = 100)
    private String village;

    @Column(nullable = false, length = 100)
    private String district;

    @Column(length = 50)
    private String state = "Tamil Nadu";

    @Column(nullable = false, length = 15)
    private String contactNumber;

    @Column(nullable = false, length = 10)
    private String openingTime = "08:00 AM";

    @Column(nullable = false, length = 10)
    private String closingTime = "06:00 PM";

    @Column(nullable = false)
    private int maxDailyCapacity = 200; // in bags / vehicles

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CentreStatus status = CentreStatus.OPEN;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public enum CentreStatus {
        OPEN,
        CLOSED
    }

    public ProcurementCentre() {}

    public ProcurementCentre(String code, String name, String address, String village, String district, String contactNumber, String openingTime, String closingTime, int maxDailyCapacity) {
        this.code = code;
        this.name = name;
        this.address = address;
        this.village = village;
        this.district = district;
        this.state = "Tamil Nadu";
        this.contactNumber = contactNumber;
        this.openingTime = openingTime;
        this.closingTime = closingTime;
        this.maxDailyCapacity = maxDailyCapacity;
        this.status = CentreStatus.OPEN;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getVillage() { return village; }
    public void setVillage(String village) { this.village = village; }

    public String getDistrict() { return district; }
    public void setDistrict(String district) { this.district = district; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public String getContactNumber() { return contactNumber; }
    public void setContactNumber(String contactNumber) { this.contactNumber = contactNumber; }

    public String getOpeningTime() { return openingTime; }
    public void setOpeningTime(String openingTime) { this.openingTime = openingTime; }

    public String getClosingTime() { return closingTime; }
    public void setClosingTime(String closingTime) { this.closingTime = closingTime; }

    public int getMaxDailyCapacity() { return maxDailyCapacity; }
    public void setMaxDailyCapacity(int maxDailyCapacity) { this.maxDailyCapacity = maxDailyCapacity; }

    public CentreStatus getStatus() { return status; }
    public void setStatus(CentreStatus status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
