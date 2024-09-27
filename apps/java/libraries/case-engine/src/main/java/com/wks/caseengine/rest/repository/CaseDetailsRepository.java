package com.wks.caseengine.rest.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.wks.caseengine.rest.entity.FaultCategory;

@Repository
public interface CaseDetailsRepository extends JpaRepository<FaultCategory, Long> {

}
