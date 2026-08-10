package com.devinolabs.uap.identity.infrastructure.web;

import java.util.Objects;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.devinolabs.uap.identity.application.AuthenticateAccountUseCase;
import com.devinolabs.uap.identity.application.AuthenticationResult;
import com.devinolabs.uap.identity.application.ClientMetadata;
import com.devinolabs.uap.identity.application.CurrentAccountResult;
import com.devinolabs.uap.identity.application.GetCurrentAccountUseCase;
import com.devinolabs.uap.identity.application.InvalidRefreshTokenException;
import com.devinolabs.uap.identity.application.LogoutUseCase;
import com.devinolabs.uap.identity.application.RegisterAccountResult;
import com.devinolabs.uap.identity.application.RegisterAccountUseCase;
import com.devinolabs.uap.identity.application.RotateRefreshSessionUseCase;
import com.devinolabs.uap.identity.application.VerifyEmailUseCase;
import com.devinolabs.uap.identity.infrastructure.http.AuthTokenTransport;
import com.devinolabs.uap.identity.infrastructure.security.AccountPrincipal;

@RestController
@RequestMapping("/api/v1/identity")
class IdentityController {

	private final RegisterAccountUseCase registerAccountUseCase;
	private final VerifyEmailUseCase verifyEmailUseCase;
	private final AuthenticateAccountUseCase authenticateAccountUseCase;
	private final RotateRefreshSessionUseCase rotateRefreshSessionUseCase;
	private final LogoutUseCase logoutUseCase;
	private final GetCurrentAccountUseCase getCurrentAccountUseCase;
	private final AuthTokenTransport authTokenTransport;

	IdentityController(
			RegisterAccountUseCase registerAccountUseCase,
			VerifyEmailUseCase verifyEmailUseCase,
			AuthenticateAccountUseCase authenticateAccountUseCase,
			RotateRefreshSessionUseCase rotateRefreshSessionUseCase,
			LogoutUseCase logoutUseCase,
			GetCurrentAccountUseCase getCurrentAccountUseCase,
			AuthTokenTransport authTokenTransport) {
		this.registerAccountUseCase = Objects.requireNonNull(registerAccountUseCase);
		this.verifyEmailUseCase = Objects.requireNonNull(verifyEmailUseCase);
		this.authenticateAccountUseCase = Objects.requireNonNull(authenticateAccountUseCase);
		this.rotateRefreshSessionUseCase = Objects.requireNonNull(rotateRefreshSessionUseCase);
		this.logoutUseCase = Objects.requireNonNull(logoutUseCase);
		this.getCurrentAccountUseCase = Objects.requireNonNull(getCurrentAccountUseCase);
		this.authTokenTransport = Objects.requireNonNull(authTokenTransport);
	}

	@PostMapping("/register")
	@ResponseStatus(HttpStatus.CREATED)
	RegisterResponse register(@Valid @RequestBody RegisterRequest request) {
		RegisterAccountResult result = registerAccountUseCase.register(request.email(), request.password());
		return new RegisterResponse(
				result.accountId().value().toString(),
				result.email().value(),
				result.status().name());
	}

	@PostMapping("/verify-email")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
		verifyEmailUseCase.verify(request.token());
	}

	@PostMapping("/login")
	LoginResponse login(
			@Valid @RequestBody LoginRequest request,
			HttpServletRequest httpRequest,
			HttpServletResponse httpResponse) {
		AuthenticationResult result = authenticateAccountUseCase.authenticate(
				request.email(),
				request.password(),
				clientMetadata(httpRequest));
		authTokenTransport.writeTokens(httpResponse, result);
		return new LoginResponse(result.accountId().value().toString(), "AUTHENTICATED");
	}

	@PostMapping("/refresh")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void refresh(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
		String refreshToken = authTokenTransport.readRefreshToken(httpRequest)
				.orElseThrow(InvalidRefreshTokenException::new);
		AuthenticationResult result = rotateRefreshSessionUseCase.rotate(refreshToken);
		authTokenTransport.writeTokens(httpResponse, result);
	}

	@PostMapping("/logout")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void logout(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
		String refreshToken = authTokenTransport.readRefreshToken(httpRequest)
				.orElseThrow(InvalidRefreshTokenException::new);
		logoutUseCase.logout(refreshToken);
		authTokenTransport.clearTokens(httpResponse);
	}

	@PostMapping("/logout-all")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void logoutAll(Authentication authentication, HttpServletResponse httpResponse) {
		AccountPrincipal principal = requirePrincipal(authentication);
		logoutUseCase.logoutAll(principal.accountId());
		authTokenTransport.clearTokens(httpResponse);
	}

	@GetMapping("/me")
	MeResponse me(Authentication authentication) {
		AccountPrincipal principal = requirePrincipal(authentication);
		CurrentAccountResult current = getCurrentAccountUseCase.execute(principal.accountId());
		return new MeResponse(
				current.accountId().value().toString(),
				current.email().value(),
				current.status().name(),
				current.emailVerifiedAt());
	}

	private static AccountPrincipal requirePrincipal(Authentication authentication) {
		if (authentication == null || !(authentication.getPrincipal() instanceof AccountPrincipal principal)) {
			throw new IllegalStateException("Authenticated principal is required");
		}
		return principal;
	}

	private static ClientMetadata clientMetadata(HttpServletRequest request) {
		return new ClientMetadata(request.getRemoteAddr(), request.getHeader("User-Agent"));
	}

}
