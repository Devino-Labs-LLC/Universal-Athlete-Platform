package com.devinolabs.uap.organization.infrastructure.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.devinolabs.uap.organization.domain.OrganizationMembershipRole;

record CreateInvitationRequest(
		@NotBlank @Email @Size(max = 320) String email,
		@NotNull OrganizationMembershipRole role) {
}
