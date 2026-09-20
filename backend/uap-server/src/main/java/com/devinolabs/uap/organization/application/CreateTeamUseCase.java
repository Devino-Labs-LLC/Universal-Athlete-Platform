package com.devinolabs.uap.organization.application;

import java.time.Clock;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devinolabs.uap.entitlements.CommercialCapability;
import com.devinolabs.uap.entitlements.CommercialEntitlementGuard;
import com.devinolabs.uap.organization.domain.AccountId;
import com.devinolabs.uap.organization.domain.Organization;
import com.devinolabs.uap.organization.domain.OrganizationId;
import com.devinolabs.uap.organization.domain.OrganizationStatus;
import com.devinolabs.uap.organization.domain.Team;
import com.devinolabs.uap.organization.domain.TeamId;

@Service
public class CreateTeamUseCase {

	private final OrganizationRepository organizationRepository;
	private final TeamRepository teamRepository;
	private final OrganizationAccessGuard accessGuard;
	private final CommercialEntitlementGuard entitlementGuard;
	private final OrganizationAuditPort auditPort;
	private final Clock clock;

	public CreateTeamUseCase(
			OrganizationRepository organizationRepository,
			TeamRepository teamRepository,
			OrganizationAccessGuard accessGuard,
			CommercialEntitlementGuard entitlementGuard,
			OrganizationAuditPort auditPort,
			Clock clock) {
		this.organizationRepository = Objects.requireNonNull(organizationRepository);
		this.teamRepository = Objects.requireNonNull(teamRepository);
		this.accessGuard = Objects.requireNonNull(accessGuard);
		this.entitlementGuard = Objects.requireNonNull(entitlementGuard);
		this.auditPort = Objects.requireNonNull(auditPort);
		this.clock = Objects.requireNonNull(clock);
	}

	@Transactional
	public TeamResult execute(AccountId accountId, OrganizationId organizationId, String name) {
		accessGuard.requireManageAccess(accountId, organizationId);
		Organization organization = organizationRepository.findById(organizationId)
				.orElseThrow(OrganizationNotFoundException::new);
		if (organization.status() == OrganizationStatus.ARCHIVED) {
			throw new OrganizationArchivedException();
		}
		entitlementGuard.requireOrganizationCapability(
				organizationId.value(), CommercialCapability.ORG_TEAM_MANAGEMENT);
		Team team = Team.register(TeamId.generate(), organizationId, name, clock);
		if (teamRepository.existsByOrganizationIdAndName(organizationId, team.name())) {
			throw new IllegalArgumentException("A team with this name already exists in the organization");
		}
		Team saved = teamRepository.save(team);
		auditPort.logTeamCreated(saved.id().value(), organizationId, accountId);
		return TeamResult.from(saved);
	}

}
