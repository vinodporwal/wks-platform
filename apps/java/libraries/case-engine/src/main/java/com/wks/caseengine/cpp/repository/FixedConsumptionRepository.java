package com.wks.caseengine.cpp.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.NativeQuery;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.wks.caseengine.cpp.dto.FinancialYearMonthProjection;
import com.wks.caseengine.cpp.dto.FixedConsumptionProjection;
import com.wks.caseengine.entity.DummyEntity;

import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional
public interface FixedConsumptionRepository extends JpaRepository<DummyEntity, Long> {
   // @Transactional
    @NativeQuery("EXEC dbo.CPP_NMD_GetFixedConsumptionByPlant :plantId, :financialYear")
    List<FixedConsumptionProjection> getFixedConsumption(@Param("plantId") UUID plantId, @Param("financialYear") String financialYear);

    @NativeQuery("SELECT Id, Year, Month FROM FinancialYearMonth WITH(NOLOCK)")
    List<FinancialYearMonthProjection> getFinancialYearMonth();

    @NativeQuery("SELECT CostCenterId FROM CPPCostCenters WITH(NOLOCK) WHERE CostCenterCode IN :costCenterCodes")
    List<String> getCostCenterIds(@Param("costCenterCodes") List<String> costCenterCodes);

    @NativeQuery("SELECT Id FROM NormParameters np WITH(NOLOCK) WHERE np.Id IN ( Select NormParameterFK_Id from CostCenterNormParameterMapping WITH(NOLOCK) where CostCenterFK_Id IN :costCenterIds)")
    List<String> getNormParameterIds(@Param("costCenterIds") List<String> costCenterIds);

    @NativeQuery("SELECT Id from UtilityFixedConsumption WITH(NOLOCK) WHERE CostCenter_FK_Id IN :costCenterIds AND NormParameter_FK_Id = :normParameterId AND FinancialYearMonth_FK_Id IN :financialYearMonthIds")
    List<String> getUtilityFixedConsumptionIds(@Param("costCenterIds") List<String> costCenterIds, @Param("normParameterId") String normParameterId, @Param("financialYearMonthIds") List<String> financialYearMonthIds);

   @Modifying
   @Transactional
   @NativeQuery("UPDATE UtilityFixedConsumption SET ConsumptionValue = :consumptionValue WHERE Id IN :utilityFixedConsumptionIds AND FinancialYearMonth_FK_Id = :financialYearMonthId")
   void updateUtilityFixedConsumption(@Param("consumptionValue") Double consumptionValue, @Param("utilityFixedConsumptionIds") List<String> utilityFixedConsumptionIds, @Param("financialYearMonthId") String financialYearMonthId);

   //get FinancialYearMonthIds for UtilityFixedConsumptionIds
   @NativeQuery("SELECT FinancialYearMonth_FK_Id FROM UtilityFixedConsumption WITH(NOLOCK) WHERE Id IN :utilityFixedConsumptionIds")
   List<String> getFinancialYearMonthIdsForUtilityFixedConsumptionIds(@Param("utilityFixedConsumptionIds") List<String> utilityFixedConsumptionIds);

   // data entry for missing month
   // @Modifying
   // @Transactional                                                                                                                    
   // @Query(value = "INSERT INTO UtilityFixedConsumption (FinancialYearMonth_FK_Id, NormParameter_FK_Id, CostCenter_FK_Id, ConsumptionValue) VALUES (:financialYearMonthId, :normParameterId, :costCenterId, :consumptionValue)", nativeQuery = true)
   // void insertUtilityFixedConsumption(@Param("financialYearMonthId") String financialYearMonthId, @Param("normParameterId") String normParameterId, @Param("costCenterId") String costCenterId, @Param("consumptionValue") Double consumptionValue);

   
   // @Modifying
   // @Transactional
   // @Query(value = "INSERT INTO UtilityFixedConsumption " +
   //         "(FinancialYearMonth_FK_Id, NormParameter_FK_Id, CostCenter_FK_Id, ConsumptionValue) " +
   //         "OUTPUT INSERTED.Id " +
   //         "VALUES (:financialYearMonthId, :normParameterId, :costCenterId, :consumptionValue)",
   //         nativeQuery = true)
   // String insertUtilityFixedConsumption(
   //         @Param("financialYearMonthId") String financialYearMonthId,
   //         @Param("normParameterId") String normParameterId,
   //         @Param("costCenterId") String costCenterId,
   //         @Param("consumptionValue") Double consumptionValue);
   


 @Transactional
@NativeQuery("INSERT INTO UtilityFixedConsumption " +
    "(FinancialYearMonth_FK_Id, NormParameter_FK_Id, CostCenter_FK_Id, ConsumptionValue) " +
    "OUTPUT INSERTED.Id " +
    "SELECT :financialYearMonthId, :normParameterId, :costCenterId, :consumptionValue")
String insertUtilityFixedConsumption(
        @Param("financialYearMonthId") String financialYearMonthId,
        @Param("normParameterId") String normParameterId,
        @Param("costCenterId") String costCenterId,
        @Param("consumptionValue") Double consumptionValue);


      @NativeQuery("SELECT Id FROM UtilityFixedConsumption_Remarks WITH(NOLOCK) where Id  In (:remarkIds)")
      List<UUID> getExistingRemarkIds(@Param("remarkIds") List<UUID> remarkIds);
     
    
}



