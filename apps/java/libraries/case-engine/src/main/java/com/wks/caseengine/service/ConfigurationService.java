package com.wks.caseengine.service;

import java.util.List;
import java.util.Map;
import java.util.UUID;


import org.springframework.web.multipart.MultipartFile;

import com.wks.caseengine.dto.AopBasisDTO;
import com.wks.caseengine.dto.CatalystChangeOverDTO;
import com.wks.caseengine.dto.TankConfigurationDTO;
import com.wks.caseengine.dto.ConfigurationDTO;
import com.wks.caseengine.dto.ConfigurationVersionDTO;
import com.wks.caseengine.dto.ExecutionDetailDto;
import com.wks.caseengine.dto.NormAttributeTransactionReceipeRequestDTO;
import com.wks.caseengine.dto.NormLineRequestDTO;
import com.wks.caseengine.dto.SpyroInputMinMaxDTO;
import com.wks.caseengine.entity.NormAttributeTransactionReceipe;
import com.wks.caseengine.message.vm.AOPMessageVM;



public interface ConfigurationService {
	
	public AOPMessageVM getConfigurationData(String year, UUID plantFKId,String version);
	public List<ConfigurationDTO> getMonthlyProductionData(String year, UUID plantFKId);
	AOPMessageVM calculateSteadyNorms(String year, String plantId,String periodTo,String periodFrom);
	AOPMessageVM carryForward(String year, String plantId);
	public AOPMessageVM getConfigurationConstants(String year,String plantFKId, boolean iscatcam);
	public AOPMessageVM getProductionConstraints(String year, String plantFKId, String type);
	public AOPMessageVM getConfigurationIntermediateValues(String year, UUID plantFKId);
    public List<ConfigurationDTO> saveConfigurationData( String year, String plantFKId,String version, List<ConfigurationDTO> configurationDTOList,Boolean calculation, boolean isMinMax);
    public   List<Map<String, Object>>  getNormAttributeTransactionReceipe(String year, String plantId, boolean iscatcam);
    public List<NormAttributeTransactionReceipeRequestDTO> updateCalculatedConsumptionNorms( String year, String plantId,  List<NormAttributeTransactionReceipeRequestDTO> normAttributeTransactionReceipeDTOLists);
    public AOPMessageVM getConfigurationIntermediateValuesData(String year, String plantId);
    public byte[] createExcel(String year, UUID plantFKId, List<String> reportTypes,String version,boolean isAfterSave, List<ConfigurationDTO> list);
    public byte[] createShutdownRateExcel(String year, UUID plantFKId,String type, boolean isAfterSave, List<ConfigurationDTO> list);
    public byte[] createConfigurationConstantsExcel(String year, UUID plantFKId, boolean iscatcam);
    public byte[] createProductionConstraintsExcel(String year, UUID plantFKId, String type);
    public byte[] exportConfigurationConstantsNorms(String year, String plantId);
    public byte[] exportConfigData(String year, UUID plantFKId, boolean isAfterSave, List<NormAttributeTransactionReceipeRequestDTO> dtoList, boolean iscatcam);
    public byte[] exportLineConfigData(String year, UUID plantFKId, boolean isAfterSave, List<NormAttributeTransactionReceipeRequestDTO> dtoList);
    public AOPMessageVM importExcel(String year, UUID fromString,List<String> reportTypes,String version, MultipartFile file,Boolean calculation, boolean isMinMax);
    public AOPMessageVM importShutdownRateExcel(String year, UUID fromString,String type,String version, MultipartFile file,Boolean calculation, boolean isMinMax);
    public AOPMessageVM importRecipe(String year, UUID fromString, MultipartFile file, boolean iscatcam);
    public AOPMessageVM importConfigurationConstantsExcel(String year, UUID plantId,String version, MultipartFile file,Boolean calculation, boolean isMinMax);
	public AOPMessageVM getConfigurationExecution( String year, String plantId);
	public AOPMessageVM getConfigurationExecutionNorms( String year, String plantId);
	public AOPMessageVM getConfigurationVersion(String year, String plantId);
	public AOPMessageVM updateConfigurationVersion(List<ConfigurationVersionDTO> configurationVersionDTOs);
    public AOPMessageVM saveConfigurationExecution( List<ExecutionDetailDto> executionDetailDtoList);
    public AOPMessageVM saveConfigurationExecutionNorms( List<ExecutionDetailDto> executionDetailDtoList);
    byte[] createConfigurationConstantsExcelResponse(String year, UUID plantFKId, List<ConfigurationDTO> list);
    public AOPMessageVM getConfigurationConstantsNorms(String year, String plantFKId);

    public List<ConfigurationDTO> saveOtherConfigurationData(String year, String plantFKId, String version,
            List<ConfigurationDTO> configurationDTOList, Boolean calculation);

    public AOPMessageVM getOtherProductionNormsData(String year, String plantId, String gradeId);
    public AOPMessageVM getNormAttributeTransactionLine(String year, String plantId);

    public AOPMessageVM getConfigurationDataReportMannualEntry(String year, UUID plantFKId, String version);
        public AOPMessageVM updateLineConfiguration(
                        String year,
                        String plantId,
                        List<NormLineRequestDTO> normLineRequestDTOList);

        public AOPMessageVM importLineConfiguration(String year, UUID plantFKId, MultipartFile file);

        public List<Map<String, Object>> getSeasonMonths(UUID plantId, String aopYear);

        public AOPMessageVM LoadConfigurationValues(String year, String plantId);

        public AOPMessageVM getCatalystChangeOver(String year, String plantFKId);

        public AOPMessageVM saveCatalystChangeOver(List<CatalystChangeOverDTO> catalystChangeOverDTOList, String year);

        public AOPMessageVM deleteCatalystChangeOver(String Id);

        public byte[] createCatalystChangeOverExcel(String year, String plantFKId, boolean isAfterSave, List<CatalystChangeOverDTO> dtoList);

        public AOPMessageVM importCatalystChangeOverExcel(String year, String plantId, MultipartFile file);

        public AOPMessageVM getTankConfiguration(String year, String plantId);

        public AOPMessageVM saveTankConfiguration(List<TankConfigurationDTO> tankConfigurationDTOList, String plantId, String aopYear);

        public byte[] createManualEntryExcel(String year, UUID plantFKId, boolean isAfterSave, List<ConfigurationDTO> dtoList);

        public AOPMessageVM importManualEntryExcel(String year, UUID plantFKId, MultipartFile file);

        public AOPMessageVM calculateCombine(UUID plantId, String aopYear);

        public AOPMessageVM getConfigurationOtherCost(String year, UUID plantFKId);
        public List<ConfigurationDTO> saveConfigurationOtherCost(String year, String plantFKId, List<ConfigurationDTO> configurationDTOList);

        public byte[] createConfigurationOtherCostExcel(String year, UUID plantFKId, boolean isAfterSave, List<ConfigurationDTO> dtoList);
        public AOPMessageVM importConfigurationOtherCostExcel(String year, UUID plantFKId, MultipartFile file);

        public List<AopBasisDTO> saveAopBasis(String year, String plantFKId, List<AopBasisDTO> configurationDTOList);
        public AOPMessageVM getAopBasis(String year, String plantFKId, String type);
        public AOPMessageVM getAopBasiswithStartDate(String year, String plantFKId, String type);
        public AOPMessageVM getCrackerC2OptimizingVariablesDropdown();
        public List<SpyroInputMinMaxDTO> saveSpyroInputMinMax(String year, String plantFKId, List<SpyroInputMinMaxDTO> configurationDTOList);
        public AOPMessageVM getGroupMaterialDetails(String year, String plantFKId);
}
