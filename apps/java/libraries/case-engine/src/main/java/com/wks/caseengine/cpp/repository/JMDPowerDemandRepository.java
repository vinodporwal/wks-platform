package com.wks.caseengine.cpp.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.wks.caseengine.cpp.dto.JMDPowerDemandProjection;
import com.wks.caseengine.cpp.entity.CPPInterSitePowerTransfer;

/**
 * Repository for executing {@code dbo.CPP_GetJMDPowerDemand} stored procedure.
 *
 * <p>Uses {@link CPPInterSitePowerTransfer} purely as the JPA anchor entity —
 * no write operations are performed through this repository.</p>
 */
@Repository
@Transactional
public interface JMDPowerDemandRepository extends JpaRepository<CPPInterSitePowerTransfer, UUID> {

    // ── GET Power Demand by plant IDs and financial year ──────────────────────
    @Query(value = "EXEC dbo.CPP_GetJMDPowerDemand @CppPlantIds = :plantIds, @FinancialYear = :financialYear",
           nativeQuery = true)
    List<JMDPowerDemandProjection> getPowerDemand(
            @Param("plantIds") String plantIds,
            @Param("financialYear") String financialYear);
}
