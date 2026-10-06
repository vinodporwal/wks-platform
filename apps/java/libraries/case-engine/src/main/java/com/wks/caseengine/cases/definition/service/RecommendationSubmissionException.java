package com.wks.caseengine.cases.definition.service;

public class RecommendationSubmissionException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public RecommendationSubmissionException(String message) {
		super(message);
	}

	public RecommendationSubmissionException(String message, Throwable cause) {
		super(message, cause);
	}
}
