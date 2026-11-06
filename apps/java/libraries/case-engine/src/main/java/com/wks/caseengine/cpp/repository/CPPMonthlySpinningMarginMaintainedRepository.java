
package com.wks.caseengine.cpp.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.wks.caseengine.entity.DummyEntity;

/**
 * Repository for the Spinning Margin Maintained report.
 *
 * Reads directly from dbo.CPPMonthlySpinningMarginMaintained.
 *
 * The table stores monthly spinning margin values per MarginType
 * (POWER / STEAM) and Plant.
 *
 * Rows with Plant_FK_Id = NULL are common to all plants.
 */
@Repository
public interface CPPMonthlySpinningMarginMaintainedRepository
        extends JpaRepository<DummyEntity, Long> {

    /**
     * Fetch all Spinning Margin Maintained rows for an AOP year.
     *
     * Includes rows where Plant_FK_Id is NULL (common rows).
     *
     * Result column order:
     * 0  : Id
     * 1  : AOPYear
     * 2  : MarginType
     * 3  : Plant_FK_Id
     * 4  : PlantName
     * 5  : UtilityName
     * 6  : UtilityCode
     * 7  : NormParameterFkId
     * 8-19  : Apr to Mar actual values
     * 20-31 : AprTarget to MarTarget
     * 32 : Remarks
     * 33 : ModifiedBy
     */
    @Query(value =
            "SELECT " +
            "    m.Id, " +
            "    m.AOPYear, " +
            "    m.MarginType, " +
            "    m.Plant_FK_Id, " +
            "    p.Name AS PlantName, " +
            "    np.Name AS UtilityName, " +
            "    np.SAPMaterialCode AS UtilityCode, " +
            "    m.NormParameterFkId, " +
            "    m.Apr, m.May, m.Jun, m.Jul, m.Aug, m.Sep, " +
            "    m.Oct, m.Nov, m.[Dec], m.Jan, m.Feb, m.Mar, " +
            "    m.AprTarget, m.MayTarget, m.JunTarget, m.JulTarget, " +
            "    m.AugTarget, m.SepTarget, m.OctTarget, m.NovTarget, " +
            "    m.DecTarget, m.JanTarget, m.FebTarget, m.MarTarget, " +
            "    m.Remarks, " +
            "    m.ModifiedBy " +
            "FROM dbo.CPPMonthlySpinningMarginMaintained m " +
            "LEFT JOIN dbo.Plants p ON p.Id = m.Plant_FK_Id " +
            "LEFT JOIN dbo.NormParameters np ON np.Id = m.NormParameterFkId " +
            "WHERE m.AOPYear = :aopYear " +
            "ORDER BY m.MarginType, p.Name, np.Name",
            nativeQuery = true)
    List<Object[]> findByAopYear(@Param("aopYear") String aopYear);
    /**
     * Fetch Spinning Margin Maintained rows for selected plants
     * and an AOP year.
     *
     * This query returns only selected Plant_FK_Id records.
     * Common rows (Plant_FK_Id IS NULL) are not included.
     *
     * Result column order is the same as findByAopYear().
     */
    @Query(value =
            "SELECT " +
            "    m.Id, " +
            "    m.AOPYear, " +
            "    m.MarginType, " +
            "    m.Plant_FK_Id, " +
            "    p.Name AS PlantName, " +
            "    np.Name AS UtilityName, " +
            "    np.SAPMaterialCode AS UtilityCode, " +
            "    m.NormParameterFkId, " +
            "    m.Apr, m.May, m.Jun, m.Jul, m.Aug, m.Sep, " +
            "    m.Oct, m.Nov, m.[Dec], m.Jan, m.Feb, m.Mar, " +
            "    m.AprTarget, m.MayTarget, m.JunTarget, m.JulTarget, " +
            "    m.AugTarget, m.SepTarget, m.OctTarget, m.NovTarget, " +
            "    m.DecTarget, m.JanTarget, m.FebTarget, m.MarTarget, " +
            "    m.Remarks, " +
            "    m.ModifiedBy " +
            "FROM dbo.CPPMonthlySpinningMarginMaintained m " +
            "LEFT JOIN dbo.Plants p ON p.Id = m.Plant_FK_Id " +
            "LEFT JOIN dbo.NormParameters np ON np.Id = m.NormParameterFkId " +
            "WHERE m.AOPYear = :aopYear " +
            "AND (m.Plant_FK_Id IN (:plantIds) OR m.Plant_FK_Id IS NULL) " +
            "ORDER BY m.MarginType, p.Name, np.Name",
            nativeQuery = true)
    List<Object[]> findByPlantIdsAndAopYear(
            @Param("plantIds") List<UUID> plantIds,
            @Param("aopYear") String aopYear);

    /**
     * Carry forward previous AOP year records when the selected
     * AOP year has no records.
     *
     * Copies:
     * - MarginType
     * - Plant_FK_Id
     * - NormParameterFkId
     *
     * Initializes all monthly actual and target values to zero.
     * Remarks and ModifiedBy are initialized to NULL.
     *
     * Does not insert anything if the selected AOP year already
     * contains at least one record.
     *
     * Returns the number of inserted records.
     */
    @Modifying
    @Transactional
    @Query(value =
        "INSERT INTO dbo.CPPMonthlySpinningMarginMaintained ( " +
        "    Id, AOPYear, MarginType, Plant_FK_Id, " +
        "    Apr, May, Jun, Jul, Aug, Sep, Oct, Nov, [Dec], Jan, Feb, Mar, " +
        "    AprTarget, MayTarget, JunTarget, JulTarget, AugTarget, SepTarget, " +
        "    OctTarget, NovTarget, DecTarget, JanTarget, FebTarget, MarTarget, " +
        "    Remarks, ModifiedBy, CreatedOn, ModifiedOn, NormParameterFkId " +
        ") " +
        "SELECT " +
        "    NEWID(), :aopYear, prev.MarginType, prev.Plant_FK_Id, " +
        "    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, " +
        "    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, " +
        "    NULL, NULL, GETDATE(), NULL, prev.NormParameterFkId " +
        "FROM dbo.CPPMonthlySpinningMarginMaintained prev " +
        "WHERE prev.AOPYear = :previousAopYear " +
        "AND NOT EXISTS ( " +
        "    SELECT 1 " +
        "    FROM dbo.CPPMonthlySpinningMarginMaintained currentYear " +
        "         WITH (UPDLOCK, HOLDLOCK) " +
        "    WHERE currentYear.AOPYear = :aopYear " +
        ")",
        nativeQuery = true)
    int carryForwardIfYearMissing(
            @Param("aopYear") String aopYear,
            @Param("previousAopYear") String previousAopYear
    );

    @Modifying
    @Transactional
    @Query(value =
        "UPDATE dbo.CPPMonthlySpinningMarginMaintained SET " +
        "  AprTarget = :aprTarget, MayTarget = :mayTarget, JunTarget = :junTarget, JulTarget = :julTarget, " +
        "  AugTarget = :augTarget, SepTarget = :sepTarget, OctTarget = :octTarget, NovTarget = :novTarget, " +
        "  DecTarget = :decTarget, JanTarget = :janTarget, FebTarget = :febTarget, MarTarget = :marTarget, " +
        "  Remarks = :remarks, ModifiedBy = :modifiedBy, ModifiedOn = GETDATE() " +
        "WHERE Id = :id",
        nativeQuery = true)
    int updateSpinningMarginTarget(
            @Param("id")         UUID id,
            @Param("aprTarget")  Double aprTarget,
            @Param("mayTarget")  Double mayTarget,
            @Param("junTarget")  Double junTarget,
            @Param("julTarget")  Double julTarget,
            @Param("augTarget")  Double augTarget,
            @Param("sepTarget")  Double sepTarget,
            @Param("octTarget")  Double octTarget,
            @Param("novTarget")  Double novTarget,
            @Param("decTarget")  Double decTarget,
            @Param("janTarget")  Double janTarget,
            @Param("febTarget")  Double febTarget,
            @Param("marTarget")  Double marTarget,
            @Param("remarks")    String remarks,
            @Param("modifiedBy") String modifiedBy);
}
