package com.devinolabs.uap.organization.application;

import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devinolabs.uap.organization.domain.AccountId;
import com.devinolabs.uap.organization.domain.Organization;
import com.devinolabs.uap.organization.domain.OrganizationId;

@Service
public class GetOrganizationUseCase {

	private final OrganizationRepository organizationRepository;
	private final OrganizationAccessGuard accessGuard;

	public GetOrganizationUseCase(
			OrganizationRepository organizationRepository,
			OrganizationAccessGuard accessGuard) {
		this.organizationRepository = Objects.requireNonNull(organizationRepository);
		this.accessGuard = Objects.requireNonNull(accessGuard);
	}

	@Transactional(readOnly = true)
	public OrganizationResult execute(AccountId accountId, OrganizationId organizationId) {
		accessGuard.requireActiveMember(accountId, organizationId);
		Organization organization = organizationRepository.findById(organizationId)
				.orElseThrow(OrganizationNotFoundException::new);
		return OrganizationResult.from(organization);
	}

}
