package com.devinolabs.uap.organization.application;

import java.time.Clock;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devinolabs.uap.organization.domain.AccountId;
import com.devinolabs.uap.organization.domain.Organization;
import com.devinolabs.uap.organization.domain.OrganizationId;
import com.devinolabs.uap.organization.domain.OrganizationStatus;

@Service
public class ArchiveOrganizationUseCase {

	private final OrganizationRepository organizationRepository;
	private final OrganizationAccessGuard accessGuard;
	private final OrganizationAuditPort auditPort;
	private final Clock clock;

	public ArchiveOrganizationUseCase(
			OrganizationRepository organizationRepository,
			OrganizationAccessGuard accessGuard,
			OrganizationAuditPort auditPort,
			Clock clock) {
		this.organizationRepository = Objects.requireNonNull(organizationRepository);
		this.accessGuard = Objects.requireNonNull(accessGuard);
		this.auditPort = Objects.requireNonNull(auditPort);
		this.clock = Objects.requireNonNull(clock);
	}

	@Transactional
	public void execute(AccountId accountId, OrganizationId organizationId) {
		accessGuard.requireManageAccess(accountId, organizationId);
		Organization organization = organizationRepository.findById(organizationId)
				.orElseThrow(OrganizationNotFoundException::new);
		if (organization.status() == OrganizationStatus.ARCHIVED) {
			throw new InvalidOrganizationStatusException("Organization is already archived");
		}
		try {
			organization.archive(clock);
		}
		catch (IllegalStateException ex) {
			throw new InvalidOrganizationStatusException(ex.getMessage());
		}
		organizationRepository.save(organization);
		auditPort.logArchived(organizationId, accountId);
	}

}
