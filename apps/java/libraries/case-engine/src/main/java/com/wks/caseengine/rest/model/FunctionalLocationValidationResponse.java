package com.wks.caseengine.rest.model;

public class FunctionalLocationValidationResponse {

	private String functionalLocation;
	private String status;
	private Integer rowCount;
	private String message;
	private FunctionalLocationValidationMetadata metadata;

	public String getFunctionalLocation() {
		return functionalLocation;
	}

	public void setFunctionalLocation(String functionalLocation) {
		this.functionalLocation = functionalLocation;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public Integer getRowCount() {
		return rowCount;
	}

	public void setRowCount(Integer rowCount) {
		this.rowCount = rowCount;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public FunctionalLocationValidationMetadata getMetadata() {
		return metadata;
	}

	public void setMetadata(FunctionalLocationValidationMetadata metadata) {
		this.metadata = metadata;
	}
}
