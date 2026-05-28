package com.wks.caseengine.cpp.repository;

import com.wks.caseengine.cpp.dto.CPPImportPowerProjection;
import com.wks.caseengine.cpp.entity.CPPImportPower;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CPPImportPowerRepository extends JpaRepository<CPPImportPower, UUID> {

    @Query(value = """
        SELECT
            ip.Id AS id,
            importPlant.DisplayName AS procurementPlant,
            np.SAPMaterialCode AS utility,
            np.Name AS material,
            np.UOM AS uom,
            ip.Apr AS apr,
            ip.May AS may,
            ip.Jun AS jun,
            ip.Jul AS jul,
            ip.Aug AS aug,
            ip.Sep AS sep,
            ip.Oct AS oct,
            ip.Nov AS nov,
            ip.[Dec] AS dec,
            ip.Jan AS jan,
            ip.Feb AS feb,
            ip.Mar AS mar,
            ip.AOPYear AS aopYear,
            ip.Site_FK_Id AS siteFkId,
            ip.Vertical_FK_ID AS verticalFkId,
            ip.Remarks AS remarks,
            CONVERT(VARCHAR(19), ip.CreatedDate, 120) AS createdDate,
            CONVERT(VARCHAR(19), ip.UpdatedDate, 120) AS updatedDate,
            ip.ImportPlantFK_ID AS importPlantFkId,
            ip.CPPPlant_FK_ID AS cppPlantFkId,
            ip.NormParameter_FK_Id AS normParameterFkId
        FROM [RIL.AOP].[dbo].[CPPImportPower] ip WITH(NOLOCK)
        LEFT JOIN [RIL.AOP].[dbo].[Plants] importPlant WITH(NOLOCK)
            ON importPlant.Id = ip.ImportPlantFK_ID
        LEFT JOIN [RIL.AOP].[dbo].[NormParameters] np WITH(NOLOCK)
            ON np.Id = ip.NormParameter_FK_Id
        WHERE ip.CPPPlant_FK_ID IN :plantIds
          AND ip.AOPYear = :aopYear
        ORDER BY importPlant.DisplayName, np.SAPMaterialCode
        """, nativeQuery = true)
    List<CPPImportPowerProjection> findImportedPowerPlans(
            @Param("plantIds") List<UUID> plantIds,
            @Param("aopYear") String aopYear);
}
