package com.devinolabs.uap.organization.application;

import java.util.List;
import java.util.Optional;

import com.devinolabs.uap.organization.domain.Invitation;
import com.devinolabs.uap.organization.domain.InvitationId;
import com.devinolabs.uap.organization.domain.OrganizationId;
import com.devinolabs.uap.organization.domain.TeamId;

public interface InvitationRepository {

	Invitation save(Invitation invitation);

	Optional<Invitation> findById(InvitationId id);

	Optional<Invitation> findByTokenHash(String tokenHash);

	Optional<Invitation> findByTokenHashForUpdate(String tokenHash);

	Optional<Invitation> findByIdForUpdate(InvitationId id);

	List<Invitation> findAllByTeamId(TeamId teamId);

	List<Invitation> findAllByOrganizationIdAndTeamIdIsNull(OrganizationId organizationId);

	List<Invitation> findPendingByInvitedEmail(String invitedEmail);

	/**
	 * Conditionally transitions PENDING → ACCEPTED. Returns rows updated (0 or 1).
	 */
	int acceptIfPending(InvitationId id, java.util.UUID acceptedMembershipId, java.time.Instant updatedAt);

}
