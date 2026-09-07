package com.devinolabs.uap.organization.application;

import java.util.List;
import java.util.Optional;

import com.devinolabs.uap.organization.domain.AccountId;
import com.devinolabs.uap.organization.domain.TeamId;
import com.devinolabs.uap.organization.domain.TeamMembership;
import com.devinolabs.uap.organization.domain.TeamMembershipId;

public interface TeamMembershipRepository {

	TeamMembership save(TeamMembership membership);

	Optional<TeamMembership> findById(TeamMembershipId id);

	Optional<TeamMembership> findActiveByTeamIdAndAccountId(TeamId teamId, AccountId accountId);

	List<TeamMembership> findAllByTeamId(TeamId teamId);

	List<TeamMembership> findAllActiveByAccountId(AccountId accountId);

	boolean existsActiveMembership(AccountId accountId, TeamId teamId);

}
