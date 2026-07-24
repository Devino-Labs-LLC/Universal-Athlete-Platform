package com.devinolabs.uap.identity.infrastructure.security;

import java.io.IOException;
import java.util.List;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.OncePerRequestFilter;

import com.devinolabs.uap.identity.domain.AccessTokenIssuer;
import com.devinolabs.uap.identity.infrastructure.IdentityAuthProperties;
import com.devinolabs.uap.identity.infrastructure.http.AuthTokenTransport;
import com.devinolabs.uap.identity.infrastructure.http.CookieAuthTokenTransport;
import com.devinolabs.uap.identity.infrastructure.http.IdentityHttpProperties;

/**
 * HTTP security boundary shared by Identity and Athlete APIs.
 *
 * <p>Cookie CSRF (double-submit), stateless sessions, access-token cookie authentication
 * via {@link AccessTokenAuthenticationFilter} producing {@link AccountPrincipal}.
 */
@Configuration
@EnableConfigurationProperties(IdentityHttpProperties.class)
class IdentitySecurityConfiguration {

	static final String REGISTER_PATH = "/api/v1/identity/register";
	static final String VERIFY_EMAIL_PATH = "/api/v1/identity/verify-email";
	static final String LOGIN_PATH = "/api/v1/identity/login";
	static final String REFRESH_PATH = "/api/v1/identity/refresh";
	static final String LOGOUT_PATH = "/api/v1/identity/logout";
	static final String LOGOUT_ALL_PATH = "/api/v1/identity/logout-all";
	static final String IDENTITY_ME_PATH = "/api/v1/identity/me";
	static final String ATHLETES_API = "/api/v1/athletes/**";
	static final String TRAINING_API = "/api/v1/training/**";

	@Bean
	AuthTokenTransport authTokenTransport(IdentityHttpProperties httpProperties, IdentityAuthProperties authProperties) {
		return new CookieAuthTokenTransport(httpProperties, authProperties);
	}

	@Bean
	AccessTokenAuthenticationFilter accessTokenAuthenticationFilter(
			AuthTokenTransport authTokenTransport,
			AccessTokenIssuer accessTokenIssuer) {
		return new AccessTokenAuthenticationFilter(authTokenTransport, accessTokenIssuer);
	}

	@Bean
	CorsConfigurationSource corsConfigurationSource(IdentityHttpProperties httpProperties) {
		IdentityHttpProperties.Cors corsProperties = httpProperties.getCors();
		if (corsProperties.isAllowCredentials() && corsProperties.getAllowedOrigins().stream().anyMatch("*"::equals)) {
			throw new IllegalStateException("CORS must not allow wildcard origins when credentials are enabled");
		}

		CorsConfiguration configuration = new CorsConfiguration();
		configuration.setAllowedOrigins(List.copyOf(corsProperties.getAllowedOrigins()));
		configuration.setAllowedMethods(List.copyOf(corsProperties.getAllowedMethods()));
		configuration.setAllowedHeaders(List.copyOf(corsProperties.getAllowedHeaders()));
		configuration.setAllowCredentials(corsProperties.isAllowCredentials());
		configuration.setExposedHeaders(List.of(httpProperties.getCsrf().getHeaderName()));

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/api/**", configuration);
		return source;
	}

	@Bean
	SecurityFilterChain identitySecurityFilterChain(
			HttpSecurity http,
			IdentityHttpProperties httpProperties,
			AccessTokenAuthenticationFilter accessTokenAuthenticationFilter) throws Exception {
		CookieCsrfTokenRepository csrfTokenRepository = CookieCsrfTokenRepository.withHttpOnlyFalse();
		csrfTokenRepository.setCookieName(httpProperties.getCsrf().getCookieName());
		csrfTokenRepository.setHeaderName(httpProperties.getCsrf().getHeaderName());
		csrfTokenRepository.setCookieCustomizer(cookie -> cookie
				.secure(httpProperties.getCookies().isSecure())
				.sameSite(sameSiteValue(httpProperties.getCookies().getSameSite()))
				.path("/"));

		http
				.csrf(csrf -> csrf
						.csrfTokenRepository(csrfTokenRepository)
						.csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler())
						.ignoringRequestMatchers(REGISTER_PATH, VERIFY_EMAIL_PATH, LOGIN_PATH))
				.cors(Customizer.withDefaults())
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.httpBasic(AbstractHttpConfigurer::disable)
				.formLogin(AbstractHttpConfigurer::disable)
				.logout(AbstractHttpConfigurer::disable)
				.exceptionHandling(exceptions -> exceptions
						.authenticationEntryPoint(IdentitySecurityProblemHandlers.authenticationEntryPoint())
						.accessDeniedHandler(IdentitySecurityProblemHandlers.accessDeniedHandler()))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers(HttpMethod.OPTIONS, "/api/**").permitAll()
						.requestMatchers(HttpMethod.POST, REGISTER_PATH, VERIFY_EMAIL_PATH, LOGIN_PATH, REFRESH_PATH)
						.permitAll()
						.requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
						.requestMatchers(HttpMethod.GET, IDENTITY_ME_PATH).authenticated()
						.requestMatchers(HttpMethod.POST, LOGOUT_PATH, LOGOUT_ALL_PATH).authenticated()
						.requestMatchers(ATHLETES_API).authenticated()
						.requestMatchers(TRAINING_API).authenticated()
						.anyRequest().denyAll())
				.addFilterBefore(accessTokenAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
				.addFilterAfter(csrfCookieFilter(), UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}

	private static OncePerRequestFilter csrfCookieFilter() {
		return new OncePerRequestFilter() {
			@Override
			protected void doFilterInternal(
					HttpServletRequest request,
					HttpServletResponse response,
					FilterChain filterChain) throws ServletException, IOException {
				CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
				if (csrfToken != null) {
					csrfToken.getToken();
				}
				filterChain.doFilter(request, response);
			}
		};
	}

	private static String sameSiteValue(IdentityHttpProperties.SameSite sameSite) {
		return sameSite.name().charAt(0) + sameSite.name().substring(1).toLowerCase();
	}

}
