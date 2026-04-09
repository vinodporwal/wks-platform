package com.wks.caseengine.rest.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.NativeQuery;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.wks.caseengine.rest.entity.EventEnrichment;

@Repository
public interface EventEnrichmentRepository extends JpaRepository<EventEnrichment, Long> {
	
	@NativeQuery("SELECT * FROM EventEnrichments as ee where ee.Event_Enrichment_PK_ID IN (:eventIdsString);")
	List<EventEnrichment> getAllEventEnrichmentsByIds(@Param(value = "eventIdsString") List<Long> eventIdsString);

	@NativeQuery("SELECT * FROM EventEnrichments as ee where ee.Event_Enrichment_PK_ID = :eventId;")
	EventEnrichment getEventEnrichmentByEventId(@Param(value = "eventId") String eventId);
	
	@NativeQuery("SELECT fh.[Equipment_PK_ID] FROM [dbo].[FaultHistory] as fh where fh.[event_enrichment_pk_id] = :enrichmentPkId;")
	String findEquipmentPkId(@Param(value = "enrichmentPkId") String enrichmentPkId);
	
	@NativeQuery("SELECT Display_Name FROM Equipments as e where e.Equipment_PK_ID = :equipmentPKID;")
	String findEquipmentName(@Param(value = "equipmentPKID") String equipmentPKID);

}
