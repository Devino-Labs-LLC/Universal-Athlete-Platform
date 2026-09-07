package com.devinolabs.uap.organization.infrastructure.web;

import java.util.Objects;

import com.devinolabs.uap.organization.application.AcceptInvitationUseCase.MembershipAcceptResult;

record AcceptInvitationResponse(
		OrganizationMembershipResponse organizationMembership,
		TeamMembershipResponse teamMembership) {

	static AcceptInvitationResponse from(MembershipAcceptResult result) {
		Objects.requireNonNull(result);
		return new AcceptInvitationResponse(
				result.organizationMembership() == null
						? null
						: OrganizationMembershipResponse.from(result.organizationMembership()),
				result.teamMembership() == null ? null : TeamMembershipResponse.from(result.teamMembership()));
	}

}
