package com.wks.caseengine.rest.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.NativeQuery;
import org.springframework.data.repository.query.Param;

import com.wks.caseengine.rest.entity.FaultHistory;

public interface FaultHistoryRepository extends JpaRepository<FaultHistory, Long> {

	@NativeQuery("SELECT * FROM FaultHistory WHERE event_enrichment_pk_id IN (:eventIdsString);")
	List<FaultHistory> getAllFaultHistoryFromEventIds(@Param(value = "eventIdsString") List<Long> eventIdsString);

}
