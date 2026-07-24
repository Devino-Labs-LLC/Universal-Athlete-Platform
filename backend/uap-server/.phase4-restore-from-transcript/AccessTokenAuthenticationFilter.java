package com.devinolabs.uap.identity.infrastructure.security;

import java.io.IOException;
import java.util.Optional;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import com.devinolabs.uap.identity.domain.AccessTokenClaims;
import com.devinolabs.uap.identity.domain.AccessTokenIssuer;
import com.devinolabs.uap.identity.infrastructure.http.AuthTokenTransport;

public class AccessTokenAuthenticationFilter extends OncePerRequestFilter {

	private final AuthTokenTransport authTokenTransport;
	private final AccessTokenIssuer accessTokenIssuer;

	public AccessTokenAuthenticationFilter(AuthTokenTransport authTokenTransport, AccessTokenIssuer accessTokenIssuer) {
		this.authTokenTransport = authTokenTransport;
		this.accessTokenIssuer = accessTokenIssuer;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		Optional<String> accessToken = authTokenTransport.readAccessToken(request);
		if (accessToken.isPresent() && SecurityContextHolder.getContext().getAuthentication() == null) {
			try {
				AccessTokenClaims claims = accessTokenIssuer.verify(accessToken.get());
				AccountPrincipal principal = new AccountPrincipal(claims.accountId());
				UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
						principal,
						null,
						principal.authorities());
				SecurityContextHolder.getContext().setAuthentication(authentication);
			}
			catch (RuntimeException ignored) {
				SecurityContextHolder.clearContext();
			}
		}
		filterChain.doFilter(request, response);
	}

}
