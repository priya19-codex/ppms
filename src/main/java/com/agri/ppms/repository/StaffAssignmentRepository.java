package com.agri.ppms.repository;

import com.agri.ppms.entity.ProcurementCentre;
import com.agri.ppms.entity.StaffAssignment;
import com.agri.ppms.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StaffAssignmentRepository extends JpaRepository<StaffAssignment, Long> {
    Optional<StaffAssignment> findByUser(User user);
    Optional<StaffAssignment> findByUserId(Long userId);
    List<StaffAssignment> findByCentre(ProcurementCentre centre);
}
