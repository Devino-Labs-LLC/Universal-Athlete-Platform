package com.devinolabs.uap.audit.infrastructure.persistence;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.devinolabs.uap.audit.api.SecurityAuditRecord;
import com.devinolabs.uap.audit.application.SecurityAuditEventRepository;

@Repository
class JpaSecurityAuditEventRepository implements SecurityAuditEventRepository {

	private final SecurityAuditEventJpaRepository jpaRepository;

	JpaSecurityAuditEventRepository(SecurityAuditEventJpaRepository jpaRepository) {
		this.jpaRepository = Objects.requireNonNull(jpaRepository);
	}

	@Override
	@Transactional
	public void append(SecurityAuditRecord record) {
		Objects.requireNonNull(record, "record must not be null");
		jpaRepository.save(toEntity(record));
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<SecurityAuditRecord> findById(UUID id) {
		Objects.requireNonNull(id, "id must not be null");
		return jpaRepository.findById(id).map(this::toRecord);
	}

	@Override
	@Transactional(readOnly = true)
	public long countByEventType(String eventType) {
		Objects.requireNonNull(eventType, "eventType must not be null");
		return jpaRepository.countByEventType(eventType);
	}

	@Override
	@Transactional(readOnly = true)
	public List<SecurityAuditRecord> findLatestByOrganizationId(UUID organizationId, int limit) {
		Objects.requireNonNull(organizationId, "organizationId must not be null");
		int safeLimit = Math.max(1, Math.min(limit, 100));
		return jpaRepository.findByOrganizationIdOrderByOccurredAtDesc(organizationId).stream()
				.limit(safeLimit)
				.map(this::toRecord)
				.toList();
	}

	private static SecurityAuditEventJpaEntity toEntity(SecurityAuditRecord record) {
		return new SecurityAuditEventJpaEntity(
				record.id(),
				record.eventType(),
				record.occurredAt(),
				record.actorAccountId(),
				record.subjectAccountId(),
				record.subjectAthleteId(),
				record.organizationId(),
				record.teamId(),
				record.resourceType(),
				record.resourceId(),
				record.metadataJson());
	}

	private SecurityAuditRecord toRecord(SecurityAuditEventJpaEntity entity) {
		return new SecurityAuditRecord(
				entity.id(),
				entity.eventType(),
				entity.occurredAt(),
				entity.actorAccountId(),
				entity.subjectAccountId(),
				entity.subjectAthleteId(),
				entity.organizationId(),
				entity.teamId(),
				entity.resourceType(),
				entity.resourceId(),
				entity.metadataJson());
	}

}
