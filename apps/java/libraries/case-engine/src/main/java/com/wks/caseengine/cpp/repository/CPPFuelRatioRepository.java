package com.wks.caseengine.cpp.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.wks.caseengine.cpp.entity.CPPFuelRatio;

@Repository
@Transactional
public interface CPPFuelRatioRepository extends JpaRepository<CPPFuelRatio, UUID> {

    List<CPPFuelRatio> findByCppPlantFkIdInAndAopYearOrderByFuelName(
            List<UUID> plantIds, String aopYear);

    @Modifying
    @Transactional
    @Query(value =
        "UPDATE dbo.CPP_FuelRatio SET " +
        "  Fuel_FK_Id     = :fuelFkId, " +
        "  FuelName       = :fuelName, " +
        "  GCV            = :gcv, " +
        "  PercentageByWt = :percentageByWt, " +
        "  Remarks        = :remarks, " +
        "  UpdatedDate    = GETDATE() " +
        "WHERE Id = :id",
        nativeQuery = true)
    int updateFuelRatio(
            @Param("id")             UUID id,
            @Param("fuelFkId")       UUID fuelFkId,
            @Param("fuelName")       String fuelName,
            @Param("gcv")            Double gcv,
            @Param("percentageByWt") Double percentageByWt,
            @Param("remarks")        String remarks);
}
