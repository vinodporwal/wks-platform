package com.wks.caseengine.cpp.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.NativeQuery;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.wks.caseengine.cpp.dto.heatrate.HeatRateDropDownProjection;
import com.wks.caseengine.cpp.dto.heatrate.HeatRateProjection;
import com.wks.caseengine.entity.DummyEntity;

@Repository
public interface HeatRateRepository extends JpaRepository<DummyEntity, Long> {
    
    // query for drop down list
    @NativeQuery("select AssetId, AssetName from PowerGenerationAssets WITH(NOLOCK) where CPPPLANT_FK_Id = :cppId and AssetType = :assetType")
    List<HeatRateDropDownProjection> findAssetNamesByCppIdAndAssetType(@Param("cppId") UUID cppId, @Param("assetType") String assetType);

    @NativeQuery("select Id, EquipType, CPPUtility, GTLoad, HeatRate, FreeSteamFactor, Remarks from HeatRateLookup WITH(NOLOCK) where AssetId = :assetId order by GTLoad asc")
    List<HeatRateProjection> findHeatRateByAssetId(@Param("assetId") UUID assetId);

    @NativeQuery("SELECT curr.Id, curr.AssetName as EquipType, curr.UtilityId as CPPUtility, curr.GTLoad, curr.FinalHeatRate as HeatRate, curr.FreeSteamFactor, curr.Remarks, prev.FinalHeatRate as PreviousYearHeatRate, curr.FinalHeatRate, curr.OEMHeatRate, curr.SelectedHeatRate FROM CPP_GTHeatRate curr WITH(NOLOCK) LEFT JOIN CPP_GTHeatRate prev WITH(NOLOCK) ON curr.Asset_FK_Id = prev.Asset_FK_Id AND curr.GTLoad = prev.GTLoad AND prev.FinancialYear = :previousFinancialYear WHERE curr.Asset_FK_Id = :assetId AND curr.FinancialYear = :financialYear ORDER BY curr.GTLoad ASC")
    List<HeatRateProjection> findGtHeatRateByAssetId(@Param("assetId") UUID assetId, @Param("financialYear") String financialYear, @Param("previousFinancialYear") String previousFinancialYear);

    // HRSG dropdown query - uses LinkedPowerAssetId to join with PowerGenerationAssets
    @NativeQuery("SELECT s.AssetId, s.AssetName FROM SteamGenerationAssets s WITH(NOLOCK) " +
                   "INNER JOIN PowerGenerationAssets p WITH(NOLOCK) ON s.LinkedPowerAssetId = p.AssetId " +
                   "WHERE p.CPPPLANT_FK_Id = :cppId AND s.AssetType = :assetType")
    List<HeatRateDropDownProjection> findHRSGAssetNamesByCppIdAndAssetType(@Param("cppId") UUID cppId, @Param("assetType") String assetType);

    // HRSG heat rate query
    @NativeQuery("SELECT curr.Id, curr.AssetName as EquipType, curr.UtilityId as CPPUtility, curr.HRSGLoad, curr.FinalHeatRate as HeatRate, curr.Remarks, prev.FinalHeatRate as PreviousYearHeatRate, curr.FinalHeatRate, curr.OEMHeatRate, curr.SelectedHeatRate FROM CPP_HRSGHeatRate curr WITH(NOLOCK) LEFT JOIN CPP_HRSGHeatRate prev WITH(NOLOCK) ON curr.Asset_FK_Id = prev.Asset_FK_Id AND curr.HRSGLoad = prev.HRSGLoad AND prev.FinancialYear = :previousFinancialYear WHERE curr.Asset_FK_Id = :assetId AND curr.FinancialYear = :financialYear ORDER BY curr.HRSGLoad ASC")
    List<com.wks.caseengine.cpp.dto.heatrate.HRSGHeatRateProjection> findHrsgHeatRateByAssetId(@Param("assetId") UUID assetId, @Param("financialYear") String financialYear, @Param("previousFinancialYear") String previousFinancialYear);
}


