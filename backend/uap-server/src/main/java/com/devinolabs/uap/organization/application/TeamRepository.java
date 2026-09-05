package com.devinolabs.uap.organization.application;

import java.util.List;
import java.util.Optional;

import com.devinolabs.uap.organization.domain.OrganizationId;
import com.devinolabs.uap.organization.domain.Team;
import com.devinolabs.uap.organization.domain.TeamId;

public interface TeamRepository {

	Team save(Team team);

	Optional<Team> findById(TeamId id);

	List<Team> findAllByOrganizationId(OrganizationId organizationId);

	boolean existsByOrganizationIdAndName(OrganizationId organizationId, String name);

}
