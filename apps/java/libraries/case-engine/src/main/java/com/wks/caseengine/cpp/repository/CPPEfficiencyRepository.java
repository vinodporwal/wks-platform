package com.wks.caseengine.cpp.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.wks.caseengine.cpp.entity.CPPEfficiency;

@Repository
@Transactional
public interface CPPEfficiencyRepository extends JpaRepository<CPPEfficiency, UUID> {

    List<CPPEfficiency> findByCppPlantFkIdInAndAopYearOrderByAssetName(
            List<UUID> plantIds, String aopYear);

    @Modifying
    @Transactional
    @Query(value =
        "UPDATE dbo.CPP_Efficiency SET " +
        "  Asset_FK_Id = :assetFkId, " +
        "  AssetName   = :assetName, " +
        "  UOM         = :uom, " +
        "  [Value]     = :value, " +
        "  Remarks     = :remarks, " +
        "  UpdatedDate = GETDATE() " +
        "WHERE Id = :id",
        nativeQuery = true)
    int updateEfficiency(
            @Param("id")        UUID id,
            @Param("assetFkId") UUID assetFkId,
            @Param("assetName") String assetName,
            @Param("uom")       String uom,
            @Param("value")     Double value,
            @Param("remarks")   String remarks);
}
