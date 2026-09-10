package com.devinolabs.uap.audit.infrastructure;

import java.util.Objects;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.devinolabs.uap.audit.api.SecurityAuditRecord;
import com.devinolabs.uap.audit.api.SecurityAuditWriter;
import com.devinolabs.uap.audit.application.SecurityAuditEventRepository;

/**
 * Joins the caller's transaction (REQUIRED) so mutation + audit commit or roll back together.
 */
@Component
class PersistingSecurityAuditWriter implements SecurityAuditWriter {

	private final SecurityAuditEventRepository repository;

	PersistingSecurityAuditWriter(SecurityAuditEventRepository repository) {
		this.repository = Objects.requireNonNull(repository);
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRED)
	public void append(SecurityAuditRecord record) {
		repository.append(Objects.requireNonNull(record, "record must not be null"));
	}

}
