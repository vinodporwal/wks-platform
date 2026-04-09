package com.wks.caseengine.repository;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.NativeQuery;
import org.springframework.stereotype.Repository;

import org.springframework.data.repository.query.Param;


import java.util.List;

import com.wks.caseengine.entity.PerformanceHighlight;

@Repository
public interface PerformanceHighlightsRepository extends JpaRepository<PerformanceHighlight, UUID>{
	
	@NativeQuery("SELECT * FROM [RIL.AOP].[dbo].[PerformanceHighlights] " +
            "WHERE SiteId = :siteId AND AOPYear = :aopYear")
	List<PerformanceHighlight> findBySiteAndYearNative(
	     @Param("siteId") UUID siteId, 
	     @Param("aopYear") String aopYear
	);
	
}
