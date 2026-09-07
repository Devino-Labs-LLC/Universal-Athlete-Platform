package com.devinolabs.uap.organization.infrastructure.web;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.devinolabs.uap.organization.application.AcceptInvitationUseCase;
import com.devinolabs.uap.organization.application.DeclineInvitationUseCase;
import com.devinolabs.uap.organization.application.ListMyInvitationsUseCase;
import com.devinolabs.uap.organization.domain.InvitationId;

@RestController
@RequestMapping("/api/v1/me/invitations")
class MyInvitationsController {

	private final ListMyInvitationsUseCase listMyInvitationsUseCase;
	private final AcceptInvitationUseCase acceptInvitationUseCase;
	private final DeclineInvitationUseCase declineInvitationUseCase;

	MyInvitationsController(
			ListMyInvitationsUseCase listMyInvitationsUseCase,
			AcceptInvitationUseCase acceptInvitationUseCase,
			DeclineInvitationUseCase declineInvitationUseCase) {
		this.listMyInvitationsUseCase = Objects.requireNonNull(listMyInvitationsUseCase);
		this.acceptInvitationUseCase = Objects.requireNonNull(acceptInvitationUseCase);
		this.declineInvitationUseCase = Objects.requireNonNull(declineInvitationUseCase);
	}

	@GetMapping
	List<MyInvitationResponse> list(Authentication authentication) {
		return listMyInvitationsUseCase.execute(OrganizationWebSupport.accountId(authentication)).stream()
				.map(MyInvitationResponse::from)
				.toList();
	}

	@PostMapping("/{invitationId}/accept")
	AcceptInvitationResponse accept(@PathVariable UUID invitationId, Authentication authentication) {
		return AcceptInvitationResponse.from(acceptInvitationUseCase.executeByInvitationId(
				OrganizationWebSupport.accountId(authentication),
				InvitationId.of(invitationId)));
	}

	@PostMapping("/{invitationId}/decline")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void decline(@PathVariable UUID invitationId, Authentication authentication) {
		declineInvitationUseCase.executeByInvitationId(
				OrganizationWebSupport.accountId(authentication),
				InvitationId.of(invitationId));
	}

}
