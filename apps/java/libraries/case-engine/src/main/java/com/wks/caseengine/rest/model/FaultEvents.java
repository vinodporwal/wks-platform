package com.wks.caseengine.rest.model;

import com.wks.caseengine.rest.entity.EventCategory;
import com.wks.caseengine.rest.entity.EventEnrichment;
import com.wks.caseengine.rest.entity.Events;

public class FaultEvents {
	private EventEnrichment eventEnrichment;
	private Events event;
	private EventCategory eventCategory;
	
	public EventEnrichment getEventEnrichment() {
		return eventEnrichment;
	}
	public void setEventEnrichment(EventEnrichment eventEnrichment) {
		this.eventEnrichment = eventEnrichment;
	}
	public Events getEvent() {
		return event;
	}
	public void setEvent(Events event) {
		this.event = event;
	}
	public EventCategory getEventCategory() {
		return eventCategory;
	}
	public void setEventCategory(EventCategory eventCategory) {
		this.eventCategory = eventCategory;
	}
}
