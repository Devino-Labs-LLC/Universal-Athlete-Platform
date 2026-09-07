package com.devinolabs.uap.organization.application;

import java.time.Clock;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devinolabs.uap.organization.domain.AccountId;
import com.devinolabs.uap.organization.domain.OrganizationId;
import com.devinolabs.uap.organization.domain.OrganizationMembership;
import com.devinolabs.uap.organization.domain.OrganizationMembershipRole;

@Service
public class LeaveOrganizationUseCase {

	private final OrganizationMembershipRepository membershipRepository;
	private final OrganizationAuditPort auditPort;
	private final Clock clock;

	public LeaveOrganizationUseCase(
			OrganizationMembershipRepository membershipRepository,
			OrganizationAuditPort auditPort,
			Clock clock) {
		this.membershipRepository = Objects.requireNonNull(membershipRepository);
		this.auditPort = Objects.requireNonNull(auditPort);
		this.clock = Objects.requireNonNull(clock);
	}

	@Transactional
	public void execute(AccountId accountId, OrganizationId organizationId) {
		OrganizationMembership membership = membershipRepository
				.findActiveByOrganizationIdAndAccountId(organizationId, accountId)
				.orElseThrow(OrganizationNotFoundException::new);
		if (membership.role() == OrganizationMembershipRole.ORG_OWNER
				&& membershipRepository.countActiveOwners(organizationId) <= 1) {
			throw new MembershipConflictException(
					"LAST_OWNER",
					"The last organization owner cannot leave");
		}
		membership.leave(clock);
		membershipRepository.save(membership);
		auditPort.membershipLeft(membership.id().value(), organizationId, null, accountId);
	}

}
