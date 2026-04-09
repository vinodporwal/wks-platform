package com.wks.caseengine.rest.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.NativeQuery;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.wks.caseengine.rest.entity.CaseCauseCategory;
import com.wks.caseengine.rest.entity.CaseCauseDescription;

@Repository
public interface CaseCauseDescriptionRepository //extends JpaRepository<CaseCauseDescription, Long> 
{
    
	List<CaseCauseDescription> findByCategory(CaseCauseCategory category);

	@NativeQuery("SELECT * FROM [case_management].[dbo].[case_cause_description] where [category_id] = :category_id")
	List<CaseCauseDescription> findAllDescriptionByCategoryId(@Param(value = "category_id") Long category_id);

}