package com.agri.ppms.repository;

import com.agri.ppms.entity.Farmer;
import com.agri.ppms.entity.QuantityRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuantityRequestRepository extends JpaRepository<QuantityRequest, Long> {
    List<QuantityRequest> findByFarmerOrderByRequestDateDesc(Farmer farmer);
    List<QuantityRequest> findByFarmerIdOrderByRequestDateDesc(Long farmerId);
    List<QuantityRequest> findByStatusOrderByRequestDateDesc(QuantityRequest.RequestStatus status);
    List<QuantityRequest> findAllByOrderByRequestDateDesc();
}
