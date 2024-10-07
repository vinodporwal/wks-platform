package com.wks.caseengine.rest.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.wks.caseengine.rest.entity.Case;

public interface CaseRepository extends JpaRepository<Case, Long> {
	
	@Query(value="SELECT HierarchyNode_PK_ID FROM [case_management].[dbo].[HierarchyNodes] WHERE DisplayNamePath LIKE :assetName AND isDeleted = 0",nativeQuery = true)
	String gethierarchyNodePKID(@Param(value = "assetName") String assetName);
}