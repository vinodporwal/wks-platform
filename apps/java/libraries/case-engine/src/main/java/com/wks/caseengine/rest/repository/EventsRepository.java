package com.wks.caseengine.rest.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.NativeQuery;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.wks.caseengine.rest.entity.Events;

@Repository
public interface EventsRepository extends JpaRepository<Events, Long> {

	@NativeQuery("SELECT * FROM Events WHERE event_pk_id = :eventId")
	Events findByEventId(@Param(value = "eventId") UUID eventId);

}
