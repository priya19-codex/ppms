package com.agri.ppms.repository;

import com.agri.ppms.entity.ProcurementCentre;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProcurementCentreRepository extends JpaRepository<ProcurementCentre, Long> {
    Optional<ProcurementCentre> findByCode(String code);

    List<ProcurementCentre> findByStatus(ProcurementCentre.CentreStatus status);

    @Query("SELECT c FROM ProcurementCentre c WHERE " +
           "LOWER(c.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(c.code) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(c.village) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(c.district) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<ProcurementCentre> searchCentres(@Param("query") String query);
}
