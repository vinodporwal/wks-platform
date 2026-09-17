package com.wks.caseengine.rest.model;

public class FunctionalLocationValidationMetadata {

	private String functionalLocation;
	private String maintenancePlant;
	private String maintenancePlantDescription;
	private String planningPlant;
	private String planningPlantDescription;
	private String cmmsSystem;
	private String sortField;

	public String getFunctionalLocation() {
		return functionalLocation;
	}

	public void setFunctionalLocation(String functionalLocation) {
		this.functionalLocation = functionalLocation;
	}

	public String getMaintenancePlant() {
		return maintenancePlant;
	}

	public void setMaintenancePlant(String maintenancePlant) {
		this.maintenancePlant = maintenancePlant;
	}

	public String getMaintenancePlantDescription() {
		return maintenancePlantDescription;
	}

	public void setMaintenancePlantDescription(String maintenancePlantDescription) {
		this.maintenancePlantDescription = maintenancePlantDescription;
	}

	public String getPlanningPlant() {
		return planningPlant;
	}

	public void setPlanningPlant(String planningPlant) {
		this.planningPlant = planningPlant;
	}

	public String getPlanningPlantDescription() {
		return planningPlantDescription;
	}

	public void setPlanningPlantDescription(String planningPlantDescription) {
		this.planningPlantDescription = planningPlantDescription;
	}

	public String getCmmsSystem() {
		return cmmsSystem;
	}

	public void setCmmsSystem(String cmmsSystem) {
		this.cmmsSystem = cmmsSystem;
	}

	public String getSortField() {
		return sortField;
	}

	public void setSortField(String sortField) {
		this.sortField = sortField;
	}
}
