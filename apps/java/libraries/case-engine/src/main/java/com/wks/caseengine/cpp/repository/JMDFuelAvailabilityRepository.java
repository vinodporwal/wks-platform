package com.wks.caseengine.cpp.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.wks.caseengine.cpp.dto.FuelWithCategoryProjection;
import com.wks.caseengine.cpp.entity.CPPFuelAvailabilityTransaction;

@Repository
@Transactional
public interface JMDFuelAvailabilityRepository extends JpaRepository<CPPFuelAvailabilityTransaction, UUID> {

    @Query(value =
        "SELECT CAST(fm.Id AS varchar(36)) AS id, " +
        "       fm.FuelCode AS fuelCode, " +
        "       fm.FuelName AS fuelName, " +
        "       fm.FuelDisplayName AS fuelDisplayName, " +
        "       fm.[Type] AS type, " +
        "       fm.UOM AS uom, " +
        "       fm.[Description] AS fuelDescription, " +
        "       CAST(fm.Category_FK_Id AS varchar(36)) AS categoryFkId, " +
        "       cat.FuelName AS categoryName, " +
        "       cat.FuelDisplayName AS categoryDisplayName " +
        "FROM dbo.CPP_FuelMaster fm WITH(NOLOCK) " +
        "LEFT JOIN dbo.CPP_FuelMaster cat WITH(NOLOCK) ON cat.Id = fm.Category_FK_Id " +
        "WHERE (:type IS NULL OR fm.[Type] = :type) " +
        "ORDER BY cat.FuelDisplayName, fm.FuelDisplayName",
        nativeQuery = true)
    List<FuelWithCategoryProjection> getFuelsWithCategory(@Param("type") String type);


    @Modifying
    @Transactional
    @Query(value =
        "UPDATE dbo.CPPFuelAvailabilityTransaction SET " +
        // "  CPPPlantFKId = :cppPlantFkId, " +
        "  Fuel_FK_Id   = :fuelFkId, " +
        // "  [Type]       = :type, " +
        "  UOM          = :uom, " +
        // COALESCE so callers that don't carry calorificValue (DMD/NCV payloads)
        // never wipe the stored value.
        "  CalorificValue = COALESCE(:calorificValue, CalorificValue), " +
        "  Apr = :apr, May = :may, Jun = :jun, Jul = :jul, " +
        "  Aug = :aug, Sep = :sep, Oct = :oct, Nov = :nov, " +
        "  [Dec] = :dec, Jan = :jan, Feb = :feb, Mar = :mar, " +
        // "  FinancialYear = :financialYear, " +
        "  Remarks = :remarks, UpdatedDate = GETDATE() " +
        "WHERE Id = :id",
        nativeQuery = true)
    int updateFuelAvailability(
            @Param("id")            UUID id,
            // @Param("cppPlantFkId")  UUID cppPlantFkId,
            @Param("fuelFkId")      UUID fuelFkId,
            // @Param("type")          String type,
            @Param("uom")           String uom,
            @Param("calorificValue") Double calorificValue,
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
            // @Param("financialYear") String financialYear,
            @Param("remarks")       String remarks);

    @Modifying
    @Transactional
    @Query(value =
        "UPDATE dbo.CPPFuelAvailabilityTransaction SET " +
        "  Cost_Apr = :costApr, Cost_May = :costMay, Cost_Jun = :costJun, Cost_Jul = :costJul, " +
        "  Cost_Aug = :costAug, Cost_Sep = :costSep, Cost_Oct = :costOct, Cost_Nov = :costNov, " +
        "  Cost_Dec = :costDec, Cost_Jan = :costJan, Cost_Feb = :costFeb, Cost_Mar = :costMar, " +
        "  Remarks = :remarks, UpdatedDate = GETDATE() " +
        "WHERE Id = :id",
        nativeQuery = true)
    int updateFuelAvailabilityCost(
            @Param("id")            UUID id,
            @Param("costApr")       Double costApr,
            @Param("costMay")       Double costMay,
            @Param("costJun")       Double costJun,
            @Param("costJul")       Double costJul,
            @Param("costAug")       Double costAug,
            @Param("costSep")       Double costSep,
            @Param("costOct")       Double costOct,
            @Param("costNov")       Double costNov,
            @Param("costDec")       Double costDec,
            @Param("costJan")       Double costJan,
            @Param("costFeb")       Double costFeb,
            @Param("costMar")       Double costMar,
            @Param("remarks")       String remarks);
}
