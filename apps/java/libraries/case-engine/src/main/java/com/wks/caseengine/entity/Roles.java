package com.wks.caseengine.entity;

import java.util.UUID;

import org.hibernate.annotations.GenericGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Local mirror of Keycloak realm roles (name, description, screens).
 * Kept in sync on create / update / delete alongside Keycloak.
 */
@Entity
@Table(name = "Roles", uniqueConstraints = {
		@UniqueConstraint(name = "UQ_Roles_Name", columnNames = "Name")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Roles {

	@Id
	@GeneratedValue(generator = "UUID")
	@GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
	@Column(name = "Id", nullable = false, updatable = false, columnDefinition = "uniqueidentifier")
	private UUID id;

	@Column(name = "Name", nullable = false, length = 255)
	private String name;

	@Column(name = "Description", columnDefinition = "nvarchar(max)")
	private String description;

	/** JSON array of screen codes, e.g. ["screen_a","screen_b"]. */
	@Column(name = "Screens", columnDefinition = "nvarchar(max)")
	private String screens;

	@Column(name = "KeycloakRoleId", length = 100)
	private String keycloakRoleId;
}
