package com.wks.caseengine.rest.db2.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.wks.caseengine.rest.db2.entity.CaseCauseCategory;

@Repository
public interface CaseCauseCategoryRepository extends JpaRepository<CaseCauseCategory, Long> {
	List<CaseCauseCategory> findByName(String name);
}
