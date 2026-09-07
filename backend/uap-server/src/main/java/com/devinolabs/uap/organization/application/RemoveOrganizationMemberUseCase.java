package com.devinolabs.uap.organization.application;

import java.time.Clock;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devinolabs.uap.organization.domain.AccountId;
import com.devinolabs.uap.organization.domain.InvitationAuthority;
import com.devinolabs.uap.organization.domain.OrganizationId;
import com.devinolabs.uap.organization.domain.OrganizationMembership;
import com.devinolabs.uap.organization.domain.OrganizationMembershipId;
import com.devinolabs.uap.organization.domain.OrganizationMembershipRole;

@Service
public class RemoveOrganizationMemberUseCase {

	private final OrganizationMembershipRepository membershipRepository;
	private final OrganizationAccessGuard accessGuard;
	private final OrganizationAuditPort auditPort;
	private final Clock clock;

	public RemoveOrganizationMemberUseCase(
			OrganizationMembershipRepository membershipRepository,
			OrganizationAccessGuard accessGuard,
			OrganizationAuditPort auditPort,
			Clock clock) {
		this.membershipRepository = Objects.requireNonNull(membershipRepository);
		this.accessGuard = Objects.requireNonNull(accessGuard);
		this.auditPort = Objects.requireNonNull(auditPort);
		this.clock = Objects.requireNonNull(clock);
	}

	@Transactional
	public void execute(AccountId actorAccountId, OrganizationId organizationId, OrganizationMembershipId membershipId) {
		OrganizationMembership actor = accessGuard.requireOrgAdminOrOwner(actorAccountId, organizationId);
		OrganizationMembership membership = membershipRepository.findById(membershipId)
				.orElseThrow(OrganizationNotFoundException::new);
		if (!membership.organizationId().equals(organizationId) || !membership.isActive()) {
			throw new OrganizationNotFoundException();
		}
		if (membership.accountId().equals(actorAccountId)) {
			throw new MembershipConflictException("CANNOT_REMOVE_SELF", "Use leave to remove your own membership");
		}
		if (!InvitationAuthority.canRemoveOrganizationMember(actor.role(), membership.role())) {
			throw new OrganizationNotFoundException();
		}
		if (membership.role() == OrganizationMembershipRole.ORG_OWNER
				&& membershipRepository.countActiveOwners(organizationId) <= 1) {
			throw new MembershipConflictException("LAST_OWNER", "Cannot remove the last organization owner");
		}
		membership.remove(clock);
		membershipRepository.save(membership);
		auditPort.membershipRemoved(
				membership.id().value(),
				organizationId,
				null,
				actorAccountId,
				membership.accountId());
	}

}
