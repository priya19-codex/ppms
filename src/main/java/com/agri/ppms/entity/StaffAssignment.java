package com.agri.ppms.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "staff_assignments")
public class StaffAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "centre_id", nullable = false)
    private ProcurementCentre centre;

    public StaffAssignment() {}

    public StaffAssignment(User user, ProcurementCentre centre) {
        this.user = user;
        this.centre = centre;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public ProcurementCentre getCentre() { return centre; }
    public void setCentre(ProcurementCentre centre) { this.centre = centre; }
}
