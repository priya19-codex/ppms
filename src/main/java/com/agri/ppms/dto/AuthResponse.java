package com.agri.ppms.dto;

import com.agri.ppms.entity.User;

public class AuthResponse {
    private String token;
    private Long id;
    private String mobileNumber;
    private String fullName;
    private User.Role role;
    private String farmerId;
    private Long centreId;
    private String centreName;

    public AuthResponse() {}

    public AuthResponse(String token, Long id, String mobileNumber, String fullName, User.Role role, String farmerId, Long centreId, String centreName) {
        this.token = token;
        this.id = id;
        this.mobileNumber = mobileNumber;
        this.fullName = fullName;
        this.role = role;
        this.farmerId = farmerId;
        this.centreId = centreId;
        this.centreName = centreName;
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getMobileNumber() { return mobileNumber; }
    public void setMobileNumber(String mobileNumber) { this.mobileNumber = mobileNumber; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public User.Role getRole() { return role; }
    public void setRole(User.Role role) { this.role = role; }

    public String getFarmerId() { return farmerId; }
    public void setFarmerId(String farmerId) { this.farmerId = farmerId; }

    public Long getCentreId() { return centreId; }
    public void setCentreId(Long centreId) { this.centreId = centreId; }

    public String getCentreName() { return centreName; }
    public void setCentreName(String centreName) { this.centreName = centreName; }
}
