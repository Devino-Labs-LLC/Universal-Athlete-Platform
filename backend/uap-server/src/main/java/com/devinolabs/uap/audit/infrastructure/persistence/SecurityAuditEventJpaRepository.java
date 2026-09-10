package com.devinolabs.uap.audit.infrastructure.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface SecurityAuditEventJpaRepository extends JpaRepository<SecurityAuditEventJpaEntity, UUID> {

	long countByEventType(String eventType);

	@Query("""
			select e from SecurityAuditEventJpaEntity e
			where e.organizationId = :organizationId
			order by e.occurredAt desc
			""")
	List<SecurityAuditEventJpaEntity> findByOrganizationIdOrderByOccurredAtDesc(
			@Param("organizationId") UUID organizationId);

}
