package com.agri.ppms.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "farmers")
public class Farmer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false, unique = true, length = 30)
    private String farmerId; // e.g., TN-FARM-00101

    @Column(nullable = false, length = 20)
    private String maskedAadhaar; // e.g. XXXX XXXX 1234

    @Column(nullable = false)
    private double landAcres = 1.0;

    @Column(nullable = false, length = 255)
    private String address;

    @Column(nullable = false, length = 100)
    private String village;

    @Column(nullable = false, length = 100)
    private String district;

    @Column(nullable = false, length = 50)
    private String state = "Tamil Nadu";

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "preferred_centre_id")
    private ProcurementCentre preferredCentre;

    @Column(nullable = false)
    private int approvedQuantity = 20; // in bags

    @Column(nullable = false)
    private int usedQuantity = 0; // in bags

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Farmer() {}

    public Farmer(User user, String farmerId, String maskedAadhaar, double landAcres, String address, String village, String district, String state, ProcurementCentre preferredCentre) {
        this.user = user;
        this.farmerId = farmerId;
        this.maskedAadhaar = maskedAadhaar;
        this.landAcres = landAcres;
        this.address = address;
        this.village = village;
        this.district = district;
        this.state = state != null ? state : "Tamil Nadu";
        this.preferredCentre = preferredCentre;
        this.approvedQuantity = (int) Math.round(landAcres * 20); // 20 bags per acre default provisional limit
        this.usedQuantity = 0;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getFarmerId() { return farmerId; }
    public void setFarmerId(String farmerId) { this.farmerId = farmerId; }

    public String getMaskedAadhaar() { return maskedAadhaar; }
    public void setMaskedAadhaar(String maskedAadhaar) { this.maskedAadhaar = maskedAadhaar; }

    public double getLandAcres() { return landAcres; }
    public void setLandAcres(double landAcres) { this.landAcres = landAcres; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getVillage() { return village; }
    public void setVillage(String village) { this.village = village; }

    public String getDistrict() { return district; }
    public void setDistrict(String district) { this.district = district; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public ProcurementCentre getPreferredCentre() { return preferredCentre; }
    public void setPreferredCentre(ProcurementCentre preferredCentre) { this.preferredCentre = preferredCentre; }

    public int getApprovedQuantity() { return approvedQuantity; }
    public void setApprovedQuantity(int approvedQuantity) { this.approvedQuantity = approvedQuantity; }

    public int getUsedQuantity() { return usedQuantity; }
    public void setUsedQuantity(int usedQuantity) { this.usedQuantity = usedQuantity; }

    public int getRemainingQuantity() {
        return Math.max(0, approvedQuantity - usedQuantity);
    }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
