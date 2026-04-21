package com.wks.caseengine.db2.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.wks.caseengine.db2.entity.AnnualAOPCostDB2;

@Repository
public interface AnnualAOPCostDB2Repository extends JpaRepository<AnnualAOPCostDB2, UUID> {

	List<AnnualAOPCostDB2> findAllByAopYearAndPlantFkIdAndParticulatesAndAopType(
			String aopYear, UUID plantFkId, String particulates, String aopType);
}
