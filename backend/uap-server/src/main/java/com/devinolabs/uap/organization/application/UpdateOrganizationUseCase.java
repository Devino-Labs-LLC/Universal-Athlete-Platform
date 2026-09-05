package com.devinolabs.uap.organization.application;

import java.time.Clock;
import java.util.Objects;

import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devinolabs.uap.organization.domain.AccountId;
import com.devinolabs.uap.organization.domain.Organization;
import com.devinolabs.uap.organization.domain.OrganizationId;
import com.devinolabs.uap.organization.domain.OrganizationStatus;

@Service
public class UpdateOrganizationUseCase {

	private final OrganizationRepository organizationRepository;
	private final OrganizationAccessGuard accessGuard;
	private final Clock clock;

	public UpdateOrganizationUseCase(
			OrganizationRepository organizationRepository,
			OrganizationAccessGuard accessGuard,
			Clock clock) {
		this.organizationRepository = Objects.requireNonNull(organizationRepository);
		this.accessGuard = Objects.requireNonNull(accessGuard);
		this.clock = Objects.requireNonNull(clock);
	}

	@Transactional
	public OrganizationResult execute(
			AccountId accountId,
			OrganizationId organizationId,
			String name,
			Long expectedVersion) {
		accessGuard.requireManageAccess(accountId, organizationId);
		Organization organization = organizationRepository.findById(organizationId)
				.orElseThrow(OrganizationNotFoundException::new);
		if (organization.status() == OrganizationStatus.ARCHIVED) {
			throw new OrganizationArchivedException();
		}
		if (expectedVersion != null && organization.version() != expectedVersion) {
			throw new ObjectOptimisticLockingFailureException(Organization.class.getName(), organizationId.value());
		}
		try {
			organization.rename(name, clock);
		}
		catch (IllegalStateException ex) {
			throw new OrganizationArchivedException();
		}
		return OrganizationResult.from(organizationRepository.save(organization));
	}

}
