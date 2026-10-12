package com.devinolabs.uap.aicoach.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.domain.Persistable;

@Entity
@Table(name = "ai_coach_messages")
public class CoachMessageEntity implements Persistable<UUID> {

	@Id
	@JdbcTypeCode(SqlTypes.UUID)
	@Column(nullable = false, updatable = false, columnDefinition = "BINARY(16)")
	private UUID id;

	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "conversation_id", nullable = false, updatable = false, columnDefinition = "BINARY(16)")
	private UUID conversationId;

	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "athlete_id", nullable = false, updatable = false, columnDefinition = "BINARY(16)")
	private UUID athleteId;

	@Column(nullable = false, length = 16, updatable = false)
	private String role;

	@Column(nullable = false, length = 4000, updatable = false)
	private String body;

	@Column(name = "content_kind", nullable = false, length = 32, updatable = false)
	private String contentKind;

	@Column(nullable = false, updatable = false)
	private boolean forecast;

	@Column(length = 500, updatable = false)
	private String uncertainty;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Transient
	private boolean isNew = true;

	protected CoachMessageEntity() {
	}

	public static CoachMessageEntity create(
			UUID id,
			UUID conversationId,
			UUID athleteId,
			String role,
			String body,
			String contentKind,
			boolean forecast,
			String uncertainty,
			Instant createdAt) {
		CoachMessageEntity entity = new CoachMessageEntity();
		entity.id = id;
		entity.conversationId = conversationId;
		entity.athleteId = athleteId;
		entity.role = role;
		entity.body = bound(body, 4000);
		entity.contentKind = contentKind;
		entity.forecast = forecast;
		entity.uncertainty = uncertainty == null ? null : bound(uncertainty, 500);
		entity.createdAt = createdAt;
		entity.isNew = true;
		return entity;
	}

	private static String bound(String value, int max) {
		if (value == null) {
			return "";
		}
		return value.length() <= max ? value : value.substring(0, max);
	}

	@Override
	public UUID getId() {
		return id;
	}

	@Override
	public boolean isNew() {
		return isNew;
	}

	@PostLoad
	@PostPersist
	void markNotNew() {
		this.isNew = false;
	}

	public UUID getConversationId() {
		return conversationId;
	}

	public UUID getAthleteId() {
		return athleteId;
	}

	public String getRole() {
		return role;
	}

	public String getBody() {
		return body;
	}

	public String getContentKind() {
		return contentKind;
	}

	public boolean isForecast() {
		return forecast;
	}

	public String getUncertainty() {
		return uncertainty;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

}
