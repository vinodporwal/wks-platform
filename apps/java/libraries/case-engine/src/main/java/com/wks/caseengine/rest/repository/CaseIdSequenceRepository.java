package com.wks.caseengine.rest.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.NativeQuery;

import com.wks.caseengine.rest.entity.CaseIdSequences;

public interface CaseIdSequenceRepository //extends JpaRepository<CaseIdSequences, Long> 
{

	@NativeQuery("SELECT * FROM case_id_sequence")
	CaseIdSequences findLastElement();

}
