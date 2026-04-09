package com.wks.caseengine.repository;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.NativeQuery;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import com.wks.caseengine.entity.ApprovedAOP;

@Repository
public interface ApprovedAOPRepository extends JpaRepository<ApprovedAOP,UUID>{
	
	@NativeQuery("SELECT * FROM ApprovedAOP WHERE AOPYear = :aopYear AND Plant_FK_Id = :plantId")
	    List<ApprovedAOP> findByYearAndPlant(@Param("aopYear") String aopYear, 
	                                               @Param("plantId") UUID plantId);
	
}
