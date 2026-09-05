package com.devinolabs.uap.organization.application;

import java.time.Clock;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devinolabs.uap.organization.domain.AccountId;
import com.devinolabs.uap.organization.domain.Organization;
import com.devinolabs.uap.organization.domain.OrganizationId;
import com.devinolabs.uap.organization.domain.OrganizationMembership;
import com.devinolabs.uap.organization.domain.OrganizationMembershipId;

@Service
public class CreateOrganizationUseCase {

	private final OrganizationRepository organizationRepository;
	private final OrganizationMembershipRepository membershipRepository;
	private final OrganizationAuditPort auditPort;
	private final Clock clock;

	public CreateOrganizationUseCase(
			OrganizationRepository organizationRepository,
			OrganizationMembershipRepository membershipRepository,
			OrganizationAuditPort auditPort,
			Clock clock) {
		this.organizationRepository = Objects.requireNonNull(organizationRepository);
		this.membershipRepository = Objects.requireNonNull(membershipRepository);
		this.auditPort = Objects.requireNonNull(auditPort);
		this.clock = Objects.requireNonNull(clock);
	}

	@Transactional
	public OrganizationResult execute(AccountId creatorAccountId, String name) {
		Objects.requireNonNull(creatorAccountId, "creatorAccountId must not be null");

		OrganizationId organizationId = OrganizationId.generate();
		Organization organization = Organization.register(organizationId, name, clock);
		Organization saved = organizationRepository.save(organization);

		OrganizationMembership ownerMembership = OrganizationMembership.registerOwner(
				OrganizationMembershipId.generate(),
				saved.id(),
				creatorAccountId,
				clock);
		membershipRepository.save(ownerMembership);

		auditPort.logCreated(saved.id(), creatorAccountId);
		return OrganizationResult.from(saved);
	}

}
