package com.example.jira_kpi_service.repository;

import com.example.jira_kpi_service.entity.Vendor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VendorRepository extends JpaRepository<Vendor, UUID> {
    Optional<Vendor> findByNameIgnoreCase(String vendorName);

    @Query("SELECT v FROM Vendor v WHERE v.active = true")
    List<UUID> findActiveVendorIds();
}
