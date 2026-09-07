package com.wks.caseengine.cpp.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.wks.caseengine.cpp.dto.InterSitePowerTransferProjection;
import com.wks.caseengine.cpp.entity.CPPInterSitePowerTransfer;

@Repository
@Transactional
public interface InterSitePowerTransferRepository
        extends JpaRepository<CPPInterSitePowerTransfer, UUID> {

    // ── GET (native query with JOINs to Plants for names) ───────────────
    @Query(value =
        "SELECT " +
        "    t.Id              AS id, " +
        "    t.FromPlantId     AS fromPlantId, " +
        "    fp.Name           AS fromPlantName, " +
        "    t.ToPlantId       AS toPlantId, " +
        "    tp.Name           AS toPlantName, " +
        "    t.UOM             AS uom, " +
        "    t.FinancialYear   AS financialYear, " +
        "    t.Apr             AS apr, " +
        "    t.May             AS may, " +
        "    t.Jun             AS jun, " +
        "    t.Jul             AS jul, " +
        "    t.Aug             AS aug, " +
        "    t.Sep             AS sep, " +
        "    t.Oct             AS oct, " +
        "    t.Nov             AS nov, " +
        "    t.Dec             AS dec, " +
        "    t.Jan             AS jan, " +
        "    t.Feb             AS feb, " +
        "    t.Mar             AS mar, " +
        "    t.Remarks         AS remarks " +
        "FROM dbo.CPPInterSitePowerTransfer AS t " +
        "LEFT JOIN dbo.Plants AS fp ON fp.Id = t.FromPlantId " +
        "LEFT JOIN dbo.Plants AS tp ON tp.Id = t.ToPlantId " +
        "WHERE t.FinancialYear = :financialYear " +
        "  AND (t.FromPlantId IN (" +
        "      SELECT TRY_CAST(value AS UNIQUEIDENTIFIER) FROM STRING_SPLIT(:plantIds, ',') " +
        "  ) OR t.ToPlantId IN (" +
        "      SELECT TRY_CAST(value AS UNIQUEIDENTIFIER) FROM STRING_SPLIT(:plantIds, ',') " +
        "  ))",
        nativeQuery = true)
    List<InterSitePowerTransferProjection> getInterSitePowerTransfer(
            @Param("plantIds") String plantIds,
            @Param("financialYear") String financialYear);

    // ── UPDATE (month columns + remarks by Id) ──────────────────────────
    @Modifying
    @Transactional
    @Query(value =
        "UPDATE dbo.CPPInterSitePowerTransfer SET " +
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

    // ── INSERT (new row) ────────────────────────────────────────────────
    @Modifying
    @Transactional
    @Query(value =
        "INSERT INTO dbo.CPPInterSitePowerTransfer (" +
        "  Id, FromPlantId, ToPlantId, UOM, FinancialYear, " +
        "  Apr, May, Jun, Jul, Aug, Sep, Oct, Nov, Dec, Jan, Feb, Mar, " +
        "  Remarks, CreatedDate, UpdatedDate" +
        ") VALUES (" +
        "  :id, :fromPlantId, :toPlantId, :uom, :financialYear, " +
        "  :apr, :may, :jun, :jul, :aug, :sep, :oct, :nov, :dec, :jan, :feb, :mar, " +
        "  :remarks, GETDATE(), GETDATE()" +
        ")",
        nativeQuery = true)
    int insertRecord(
            @Param("id")            UUID   id,
            @Param("fromPlantId")   UUID   fromPlantId,
            @Param("toPlantId")     UUID   toPlantId,
            @Param("uom")           String uom,
            @Param("financialYear") String financialYear,
            @Param("apr")           Double apr,
            @Param("may")           Double may,
            @Param("jun")           Double jun,
            @Param("jul")           Double jul,
            @Param("aug")           Double aug,
            @Param("sep")           Double sep,
            @Param("oct")           Double oct,
            @Param("nov")           Double nov,
            @Param("dec")           Double dec,
            @Param("jan")           Double jan,
            @Param("feb")           Double feb,
            @Param("mar")           Double mar,
            @Param("remarks")       String remarks);
}
