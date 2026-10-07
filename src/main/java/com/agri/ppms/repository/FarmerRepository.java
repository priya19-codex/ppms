package com.agri.ppms.repository;

import com.agri.ppms.entity.Farmer;
import com.agri.ppms.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FarmerRepository extends JpaRepository<Farmer, Long> {
    Optional<Farmer> findByUser(User user);
    Optional<Farmer> findByFarmerId(String farmerId);
    Optional<Farmer> findByUserMobileNumber(String mobileNumber);

    @Query("SELECT f FROM Farmer f WHERE " +
           "LOWER(f.user.fullName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(f.farmerId) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(f.user.mobileNumber) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(f.village) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(f.district) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<Farmer> searchFarmers(@Param("query") String query);
}
