package com.devinolabs.uap.organization.infrastructure.web;

import java.util.Objects;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.devinolabs.uap.organization.application.AcceptInvitationUseCase;
import com.devinolabs.uap.organization.application.DeclineInvitationUseCase;

@RestController
@RequestMapping("/api/v1/invitations")
class InvitationTokenController {

	private final AcceptInvitationUseCase acceptInvitationUseCase;
	private final DeclineInvitationUseCase declineInvitationUseCase;

	InvitationTokenController(
			AcceptInvitationUseCase acceptInvitationUseCase,
			DeclineInvitationUseCase declineInvitationUseCase) {
		this.acceptInvitationUseCase = Objects.requireNonNull(acceptInvitationUseCase);
		this.declineInvitationUseCase = Objects.requireNonNull(declineInvitationUseCase);
	}

	@PostMapping("/{rawToken}/accept")
	AcceptInvitationResponse accept(@PathVariable String rawToken, Authentication authentication) {
		return AcceptInvitationResponse.from(acceptInvitationUseCase.executeByRawToken(
				OrganizationWebSupport.accountId(authentication),
				rawToken));
	}

	@PostMapping("/{rawToken}/decline")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void decline(@PathVariable String rawToken, Authentication authentication) {
		declineInvitationUseCase.executeByRawToken(OrganizationWebSupport.accountId(authentication), rawToken);
	}

}
