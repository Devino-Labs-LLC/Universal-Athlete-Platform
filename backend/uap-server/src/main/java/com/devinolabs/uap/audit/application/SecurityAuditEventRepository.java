package com.devinolabs.uap.audit.application;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.devinolabs.uap.audit.api.SecurityAuditRecord;

/**
 * Append-only audit store. No update or delete operations are exposed.
 */
public interface SecurityAuditEventRepository {

	void append(SecurityAuditRecord record);

	Optional<SecurityAuditRecord> findById(UUID id);

	long countByEventType(String eventType);

	List<SecurityAuditRecord> findLatestByOrganizationId(UUID organizationId, int limit);

}
