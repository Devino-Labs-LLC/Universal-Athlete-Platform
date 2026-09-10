package com.devinolabs.uap.audit.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "security_audit_events")
class SecurityAuditEventJpaEntity {

	@Id
	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "id", nullable = false, updatable = false, columnDefinition = "BINARY(16)")
	private UUID id;

	@Column(name = "event_type", nullable = false, updatable = false, length = 64)
	private String eventType;

	@Column(name = "occurred_at", nullable = false, updatable = false)
	private Instant occurredAt;

	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "actor_account_id", updatable = false, columnDefinition = "BINARY(16)")
	private UUID actorAccountId;

	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "subject_account_id", updatable = false, columnDefinition = "BINARY(16)")
	private UUID subjectAccountId;

	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "subject_athlete_id", updatable = false, columnDefinition = "BINARY(16)")
	private UUID subjectAthleteId;

	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "organization_id", updatable = false, columnDefinition = "BINARY(16)")
	private UUID organizationId;

	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "team_id", updatable = false, columnDefinition = "BINARY(16)")
	private UUID teamId;

	@Column(name = "resource_type", updatable = false, length = 40)
	private String resourceType;

	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "resource_id", updatable = false, columnDefinition = "BINARY(16)")
	private UUID resourceId;

	@Column(name = "metadata_json", updatable = false, length = 500)
	private String metadataJson;

	protected SecurityAuditEventJpaEntity() {
	}

	SecurityAuditEventJpaEntity(
			UUID id,
			String eventType,
			Instant occurredAt,
			UUID actorAccountId,
			UUID subjectAccountId,
			UUID subjectAthleteId,
			UUID organizationId,
			UUID teamId,
			String resourceType,
			UUID resourceId,
			String metadataJson) {
		this.id = id;
		this.eventType = eventType;
		this.occurredAt = occurredAt;
		this.actorAccountId = actorAccountId;
		this.subjectAccountId = subjectAccountId;
		this.subjectAthleteId = subjectAthleteId;
		this.organizationId = organizationId;
		this.teamId = teamId;
		this.resourceType = resourceType;
		this.resourceId = resourceId;
		this.metadataJson = metadataJson;
	}

	UUID id() {
		return id;
	}

	String eventType() {
		return eventType;
	}

	Instant occurredAt() {
		return occurredAt;
	}

	UUID actorAccountId() {
		return actorAccountId;
	}

	UUID subjectAccountId() {
		return subjectAccountId;
	}

	UUID subjectAthleteId() {
		return subjectAthleteId;
	}

	UUID organizationId() {
		return organizationId;
	}

	UUID teamId() {
		return teamId;
	}

	String resourceType() {
		return resourceType;
	}

	UUID resourceId() {
		return resourceId;
	}

	String metadataJson() {
		return metadataJson;
	}

}
