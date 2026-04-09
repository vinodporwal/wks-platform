package com.wks.caseengine.rest.db2.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.NativeQuery;
import org.springframework.data.repository.query.Param;

import com.wks.caseengine.rest.db2.entity.Case;

public interface CaseRepository extends JpaRepository<Case, Long> {
	
	@NativeQuery("SELECT HierarchyNode_PK_ID FROM [HierarchyNodes] WHERE DisplayNamePath LIKE :assetName AND isDeleted = 0")
	String gethierarchyNodePKID(@Param(value = "assetName") String assetName);

	@NativeQuery(" select * from cases where hierarchy_node_pk_id in (:assetsPKIds) ORDER BY case_no DESC")
	List<Case> findAllByAssetsPKID(@Param(value="assetsPKIds") List<String> assetsPKIds);

	@NativeQuery(" select * from cases where case_no =:case_no")
	Case getByCaseNo(@Param(value="case_no") String case_no);
}
