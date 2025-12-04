package com.example.jira_kpi_service.repository;

import com.example.jira_kpi_service.entity.VendorKpi;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VendorKpiRepository extends JpaRepository<VendorKpi, Long> {
    Optional<VendorKpi> findByVendorIdAndPeriodDateAndPeriodType(UUID vendorId,
                                                                 LocalDate periodDate,
                                                                 VendorKpi.PeriodType periodType);

    List<VendorKpi> findByPeriodTypeAndPeriodDate(VendorKpi.PeriodType periodType, LocalDate periodDate);

    @Query("""
        SELECT k FROM VendorKpi k 
        WHERE k.periodType = :periodType 
          AND k.periodDate >= :startDate 
        ORDER BY k.periodDate DESC
        """)
    List<VendorKpi> findRecentByPeriodType(@Param("periodType") VendorKpi.PeriodType periodType,
                                           @Param("startDate") LocalDate startDate);

    @Modifying
    @Transactional
    @Query("""
        DELETE FROM VendorKpi k 
        WHERE k.vendor.id = :vendorId 
          AND k.periodDate = :periodDate 
          AND k.periodType = :periodType
        """)
    void deleteExisting(@Param("vendorId") UUID vendorId,
                        @Param("periodDate") LocalDate periodDate,
                        @Param("periodType") VendorKpi.PeriodType periodType);

    // For upsert pattern
    default void upsert(VendorKpi kpi) {
        deleteExisting(kpi.getVendorId(), kpi.getPeriodDate(), kpi.getPeriodType());
        save(kpi);
    }
}
