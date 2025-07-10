package com.wks.caseengine.rest.model;

public class FaultEvents {
	private EventEnrichmentModel eventEnrichment;
	private EventsModel events;
	private EventCategoryModel eventCategory;
	private String AssetDisplayName;
	private String assetName;
	private String startTime;
	private String endTime;
	private String path;
	private String faultDisplayName;
	private String mainAsset;

	public EventEnrichmentModel getEventEnrichment() {
		return eventEnrichment;
	}
	public void setEventEnrichment(EventEnrichmentModel eventEnrichment) {
		this.eventEnrichment = eventEnrichment;
	}
	public EventsModel getEvents() {
		return events;
	}
	public void setEvents(EventsModel events) {
		this.events = events;
	}
	public EventCategoryModel getEventCategory() {
		return eventCategory;
	}
	public void setEventCategory(EventCategoryModel eventCategory) {
		this.eventCategory = eventCategory;
	}
	public String getAssetDisplayName() {
		return AssetDisplayName;
	}
	public void setAssetDisplayName(String assetDisplayName) {
		AssetDisplayName = assetDisplayName;
	}
	public String getAssetName() {
		return assetName;
	}
	public void setAssetName(String assetName) {
		this.assetName = assetName;
	}
	public String getStartTime() {
		return startTime;
	}
	public void setStartTime(String startTime) {
		this.startTime = startTime;
	}
	public String getEndTime() {
		return endTime;
	}
	public void setEndTime(String endTime) {
		this.endTime = endTime;
	}
	public String getPath() {
		return path;
	}
	public void setPath(String path) {
		this.path = path;
	}
	public String getFaultDisplayName() {
		return faultDisplayName;
	}
	public void setFaultDisplayName(String faultDisplayName) {
		this.faultDisplayName = faultDisplayName;
	}
	public String getMainAsset() {
		return mainAsset;
	}
	public void setMainAsset(String mainAsset) {
		this.mainAsset = mainAsset;
	}
}
