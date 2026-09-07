package com.devinolabs.uap.organization.infrastructure.web;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.devinolabs.uap.organization.application.ArchiveTeamUseCase;
import com.devinolabs.uap.organization.application.CreateTeamInvitationUseCase;
import com.devinolabs.uap.organization.application.GetTeamRosterUseCase;
import com.devinolabs.uap.organization.application.GetTeamUseCase;
import com.devinolabs.uap.organization.application.LeaveTeamUseCase;
import com.devinolabs.uap.organization.application.ListTeamInvitationsUseCase;
import com.devinolabs.uap.organization.application.ListTeamMembershipsUseCase;
import com.devinolabs.uap.organization.application.RemoveTeamMemberUseCase;
import com.devinolabs.uap.organization.application.RevokeInvitationUseCase;
import com.devinolabs.uap.organization.application.UpdateTeamUseCase;
import com.devinolabs.uap.organization.domain.InvitationId;
import com.devinolabs.uap.organization.domain.TeamId;
import com.devinolabs.uap.organization.domain.TeamMembershipId;

@RestController
@RequestMapping("/api/v1/teams")
class TeamController {

	private final GetTeamUseCase getTeamUseCase;
	private final UpdateTeamUseCase updateTeamUseCase;
	private final ArchiveTeamUseCase archiveTeamUseCase;
	private final CreateTeamInvitationUseCase createTeamInvitationUseCase;
	private final ListTeamInvitationsUseCase listTeamInvitationsUseCase;
	private final RevokeInvitationUseCase revokeInvitationUseCase;
	private final ListTeamMembershipsUseCase listTeamMembershipsUseCase;
	private final GetTeamRosterUseCase getTeamRosterUseCase;
	private final RemoveTeamMemberUseCase removeTeamMemberUseCase;
	private final LeaveTeamUseCase leaveTeamUseCase;

	TeamController(
			GetTeamUseCase getTeamUseCase,
			UpdateTeamUseCase updateTeamUseCase,
			ArchiveTeamUseCase archiveTeamUseCase,
			CreateTeamInvitationUseCase createTeamInvitationUseCase,
			ListTeamInvitationsUseCase listTeamInvitationsUseCase,
			RevokeInvitationUseCase revokeInvitationUseCase,
			ListTeamMembershipsUseCase listTeamMembershipsUseCase,
			GetTeamRosterUseCase getTeamRosterUseCase,
			RemoveTeamMemberUseCase removeTeamMemberUseCase,
			LeaveTeamUseCase leaveTeamUseCase) {
		this.getTeamUseCase = Objects.requireNonNull(getTeamUseCase);
		this.updateTeamUseCase = Objects.requireNonNull(updateTeamUseCase);
		this.archiveTeamUseCase = Objects.requireNonNull(archiveTeamUseCase);
		this.createTeamInvitationUseCase = Objects.requireNonNull(createTeamInvitationUseCase);
		this.listTeamInvitationsUseCase = Objects.requireNonNull(listTeamInvitationsUseCase);
		this.revokeInvitationUseCase = Objects.requireNonNull(revokeInvitationUseCase);
		this.listTeamMembershipsUseCase = Objects.requireNonNull(listTeamMembershipsUseCase);
		this.getTeamRosterUseCase = Objects.requireNonNull(getTeamRosterUseCase);
		this.removeTeamMemberUseCase = Objects.requireNonNull(removeTeamMemberUseCase);
		this.leaveTeamUseCase = Objects.requireNonNull(leaveTeamUseCase);
	}

	@GetMapping("/{teamId}")
	TeamResponse get(@PathVariable UUID teamId, Authentication authentication) {
		return TeamResponse.from(
				getTeamUseCase.execute(OrganizationWebSupport.accountId(authentication), TeamId.of(teamId)));
	}

	@PatchMapping("/{teamId}")
	TeamResponse update(
			@PathVariable UUID teamId,
			@Valid @RequestBody UpdateTeamRequest request,
			Authentication authentication) {
		return TeamResponse.from(updateTeamUseCase.execute(
				OrganizationWebSupport.accountId(authentication),
				TeamId.of(teamId),
				request.name(),
				request.expectedVersion()));
	}

	@PostMapping("/{teamId}/archive")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void archive(@PathVariable UUID teamId, Authentication authentication) {
		archiveTeamUseCase.execute(OrganizationWebSupport.accountId(authentication), TeamId.of(teamId));
	}

	@PostMapping("/{teamId}/invitations")
	@ResponseStatus(HttpStatus.CREATED)
	InvitationResponse createInvitation(
			@PathVariable UUID teamId,
			@Valid @RequestBody CreateInvitationRequest request,
			Authentication authentication) {
		return InvitationResponse.from(createTeamInvitationUseCase.execute(
				OrganizationWebSupport.accountId(authentication),
				TeamId.of(teamId),
				request.email(),
				request.role()));
	}

	@GetMapping("/{teamId}/invitations")
	List<InvitationResponse> listInvitations(@PathVariable UUID teamId, Authentication authentication) {
		return listTeamInvitationsUseCase
				.execute(OrganizationWebSupport.accountId(authentication), TeamId.of(teamId))
				.stream()
				.map(InvitationResponse::withoutToken)
				.toList();
	}

	@PostMapping("/{teamId}/invitations/{invitationId}/revoke")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void revokeInvitation(
			@PathVariable UUID teamId,
			@PathVariable UUID invitationId,
			Authentication authentication) {
		revokeInvitationUseCase.revokeTeamInvitation(
				OrganizationWebSupport.accountId(authentication),
				TeamId.of(teamId),
				InvitationId.of(invitationId));
	}

	@GetMapping("/{teamId}/memberships")
	List<TeamMembershipResponse> listMemberships(@PathVariable UUID teamId, Authentication authentication) {
		return listTeamMembershipsUseCase
				.execute(OrganizationWebSupport.accountId(authentication), TeamId.of(teamId))
				.stream()
				.map(TeamMembershipResponse::from)
				.toList();
	}

	@GetMapping("/{teamId}/roster")
	List<TeamRosterEntryResponse> listRoster(@PathVariable UUID teamId, Authentication authentication) {
		return getTeamRosterUseCase
				.execute(OrganizationWebSupport.accountId(authentication), TeamId.of(teamId))
				.stream()
				.map(TeamRosterEntryResponse::from)
				.toList();
	}

	@DeleteMapping("/{teamId}/memberships/{membershipId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void removeMembership(
			@PathVariable UUID teamId,
			@PathVariable UUID membershipId,
			Authentication authentication) {
		removeTeamMemberUseCase.execute(
				OrganizationWebSupport.accountId(authentication),
				TeamId.of(teamId),
				TeamMembershipId.of(membershipId));
	}

	@PostMapping("/{teamId}/memberships/me/leave")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void leave(@PathVariable UUID teamId, Authentication authentication) {
		leaveTeamUseCase.execute(OrganizationWebSupport.accountId(authentication), TeamId.of(teamId));
	}

}
