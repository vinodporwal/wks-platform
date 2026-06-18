package com.wks.caseengine.cpp.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.wks.caseengine.cpp.dto.FixedConsumptionProjection;
import com.wks.caseengine.cpp.entity.CPPFixedConsumption;

@Repository
@Transactional
public interface JMDFixedConsumptionRepository extends JpaRepository<CPPFixedConsumption, UUID> {

    // ── GET ──────────────────────────────────────────────────────────────────
    @Query(value = "EXEC dbo.CPP_GetFixedConsumptionByPlant :plantIds, :financialYear", nativeQuery = true)
    List<FixedConsumptionProjection> getFixedConsumptionForPlants(
            @Param("plantIds") String plantIds,
            @Param("financialYear") String financialYear);

    // ── SAVE (update month columns by Id) ────────────────────────────────────
    @Modifying
    @Transactional
    @Query(value =
        "UPDATE dbo.CPPFixConsuption SET " +
        "  Apr = :apr, May = :may, Jun = :jun, Jul = :jul, " +
        "  Aug = :aug, Sep = :sep, Oct = :oct, Nov = :nov, " +
        "  Dec = :dec, Jan = :jan, Feb = :feb, Mar = :mar, " +
        "  Remarks = :remarks, UpdatedDate = GETDATE() " +
        "WHERE Id = :id",
        nativeQuery = true)
    int updateMonthValues(
            @Param("id")      UUID   id,
            @Param("apr")     Double apr,
            @Param("may")     Double may,
            @Param("jun")     Double jun,
            @Param("jul")     Double jul,
            @Param("aug")     Double aug,
            @Param("sep")     Double sep,
            @Param("oct")     Double oct,
            @Param("nov")     Double nov,
            @Param("dec")     Double dec,
            @Param("jan")     Double jan,
            @Param("feb")     Double feb,
            @Param("mar")     Double mar,
            @Param("remarks") String remarks);
}
