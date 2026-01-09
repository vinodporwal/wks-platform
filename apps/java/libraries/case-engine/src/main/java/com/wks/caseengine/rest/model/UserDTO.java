package com.wks.caseengine.rest.model;

public class UserDTO {

	private String userId;
	private String emailId;
	public UserDTO(String userId, String emailId) {
		this.userId = userId;
        this.emailId = emailId;
	}
	public String getUserId() {
		return userId;
	}
	public void setUserId(String userId) {
		this.userId = userId;
	}
	public String getEmailId() {
		return emailId;
	}
	public void setEmailId(String emailId) {
		this.emailId = emailId;
	}
	
}
