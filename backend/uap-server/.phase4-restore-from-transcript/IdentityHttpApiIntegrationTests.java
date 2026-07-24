package com.devinolabs.uap.identity.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.servlet.http.Cookie;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

import com.devinolabs.uap.TestcontainersConfiguration;
import com.devinolabs.uap.identity.domain.Account;
import com.devinolabs.uap.identity.domain.AccountId;
import com.devinolabs.uap.identity.domain.AccountStatus;
import com.devinolabs.uap.identity.domain.EmailAddress;
import com.devinolabs.uap.identity.domain.PasswordCredential;
import com.devinolabs.uap.identity.domain.PasswordHasher;
import com.devinolabs.uap.identity.application.AccountRepository;
import com.devinolabs.uap.identity.infrastructure.notification.InMemoryVerificationNotifier;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class IdentityHttpApiIntegrationTests {

	private static final String PASSWORD = "SecurePass123!";
	private static final AtomicInteger EMAIL_SEQ = new AtomicInteger();

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private InMemoryVerificationNotifier verificationNotifier;

	@Autowired
	private AccountRepository accountRepository;

	@Autowired
	private PasswordHasher passwordHasher;

	@BeforeEach
	void setUp() {
		verificationNotifier.clear();
	}

	@Test
	void registerValidationFailureReturnsStandardError() throws Exception {
		mockMvc.perform(post("/api/v1/identity/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email":"","password":""}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
				.andExpect(jsonPath("$.message").value("Request validation failed"))
				.andExpect(jsonPath("$.path").value("/api/v1/identity/register"))
				.andExpect(jsonPath("$.timestamp").exists())
				.andExpect(jsonPath("$.details").isArray());
	}

	@Test
	void passwordPolicyViolationReturnsDetailsWithoutSecrets() throws Exception {
		mockMvc.perform(post("/api/v1/identity/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email":"%s","password":"short"}
								""".formatted(uniqueEmail("policy"))))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("PASSWORD_POLICY_VIOLATION"))
				.andExpect(jsonPath("$.details[0].field").value("password"))
				.andExpect(content().string(not(containsString("$2a$"))))
				.andExpect(content().string(not(containsString("short"))));
	}

	@Test
	void duplicateRegistrationReturnsConflict() throws Exception {
		String email = uniqueEmail("dup");
		register(email, PASSWORD);

		mockMvc.perform(post("/api/v1/identity/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email":"%s","password":"%s"}
								""".formatted(email, PASSWORD)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("DUPLICATE_EMAIL"));
	}

	@Test
	void loginSuccessSetsSecureHttpOnlyCookiesWithoutTokenBodies() throws Exception {
		String email = uniqueEmail("login");
		registerVerified(email);

		MvcResult result = mockMvc.perform(post("/api/v1/identity/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email":"%s","password":"%s"}
								""".formatted(email, PASSWORD)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("AUTHENTICATED"))
				.andExpect(jsonPath("$.accessToken").doesNotExist())
				.andExpect(jsonPath("$.refreshToken").doesNotExist())
				.andExpect(cookie().exists("uap_at"))
				.andExpect(cookie().exists("uap_rt"))
				.andExpect(cookie().httpOnly("uap_at", true))
				.andExpect(cookie().httpOnly("uap_rt", true))
				.andExpect(cookie().secure("uap_at", false))
				.andExpect(cookie().secure("uap_rt", false))
				.andExpect(cookie().path("uap_at", "/api"))
				.andExpect(cookie().path("uap_rt", "/api/v1/identity"))
				.andExpect(content().string(not(containsString("$2a$"))))
				.andReturn();

		String setCookie = String.join(",", result.getResponse().getHeaders("Set-Cookie"));
		assertThat(setCookie).containsIgnoringCase("SameSite=Lax");
		assertThat(setCookie).doesNotContain("refreshToken");
	}

	@Test
	void loginFailureDoesNotRevealAccountExistence() throws Exception {
		String known = uniqueEmail("known");
		registerVerified(known);

		mockMvc.perform(post("/api/v1/identity/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email":"missing.user@example.com","password":"%s"}
								""".formatted(PASSWORD)))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
				.andExpect(jsonPath("$.message").value("Invalid credentials"));

		mockMvc.perform(post("/api/v1/identity/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email":"%s","password":"WrongPass123!"}
								""".formatted(known)))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
				.andExpect(jsonPath("$.message").value("Invalid credentials"));
	}

	@Test
	void loginMapsUnverifiedDisabledAndLockedAccounts() throws Exception {
		String pending = uniqueEmail("pending");
		register(pending, PASSWORD);
		mockMvc.perform(post("/api/v1/identity/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email":"%s","password":"%s"}
								""".formatted(pending, PASSWORD)))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("EMAIL_NOT_VERIFIED"));

		String disabledEmail = uniqueEmail("disabled");
		accountRepository.save(Account.rehydrate(
				AccountId.generate(),
				EmailAddress.of(disabledEmail),
				PasswordCredential.fromHash(passwordHasher.hash(PASSWORD)),
				AccountStatus.DISABLED,
				0,
				null,
				Instant.parse("2026-07-24T11:00:00Z"),
				Instant.parse("2026-07-24T11:00:00Z"),
				Instant.parse("2026-07-24T11:00:00Z"),
				0L));
		mockMvc.perform(post("/api/v1/identity/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email":"%s","password":"%s"}
								""".formatted(disabledEmail, PASSWORD)))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("ACCOUNT_DISABLED"));

		String lockedEmail = uniqueEmail("locked");
		registerVerified(lockedEmail);
		for (int i = 0; i < 5; i++) {
			mockMvc.perform(post("/api/v1/identity/login")
							.contentType(MediaType.APPLICATION_JSON)
							.content("""
									{"email":"%s","password":"WrongPass123!"}
									""".formatted(lockedEmail)))
					.andExpect(status().isUnauthorized());
		}
		mockMvc.perform(post("/api/v1/identity/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email":"%s","password":"%s"}
								""".formatted(lockedEmail, PASSWORD)))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("ACCOUNT_LOCKED"));
	}

	@Test
	void refreshRotatesCookiesAndInvalidatesOldRefreshToken() throws Exception {
		AuthCookies first = loginCookies(uniqueEmail("refresh"));

		MvcResult rotated = mockMvc.perform(post("/api/v1/identity/refresh")
						.cookie(first.access(), first.refresh())
						.with(csrf()))
				.andExpect(status().isNoContent())
				.andExpect(cookie().exists("uap_at"))
				.andExpect(cookie().exists("uap_rt"))
				.andReturn();

		Cookie newAccess = rotated.getResponse().getCookie("uap_at");
		Cookie newRefresh = rotated.getResponse().getCookie("uap_rt");
		assertThat(newRefresh.getValue()).isNotEqualTo(first.refresh().getValue());

		mockMvc.perform(post("/api/v1/identity/refresh")
						.cookie(first.refresh())
						.with(csrf()))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value(org.hamcrest.Matchers.anyOf(
						org.hamcrest.Matchers.is("REFRESH_TOKEN_INVALID"),
						org.hamcrest.Matchers.is("REFRESH_TOKEN_REVOKED"))));

		mockMvc.perform(get("/api/v1/identity/me").cookie(newAccess))
				.andExpect(status().isOk());
	}

	@Test
	void logoutClearsCookiesAndRevokesRefreshSession() throws Exception {
		AuthCookies cookies = loginCookies(uniqueEmail("logout"));

		mockMvc.perform(post("/api/v1/identity/logout")
						.cookie(cookies.access(), cookies.refresh())
						.with(csrf()))
				.andExpect(status().isNoContent())
				.andExpect(cookie().maxAge("uap_at", 0))
				.andExpect(cookie().maxAge("uap_rt", 0));

		mockMvc.perform(post("/api/v1/identity/refresh")
						.cookie(cookies.refresh())
						.with(csrf()))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void logoutAllRequiresAuthentication() throws Exception {
		mockMvc.perform(post("/api/v1/identity/logout-all").with(csrf()))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));

		AuthCookies cookies = loginCookies(uniqueEmail("logout-all"));
		mockMvc.perform(post("/api/v1/identity/logout-all")
						.cookie(cookies.access(), cookies.refresh())
						.with(csrf()))
				.andExpect(status().isNoContent())
				.andExpect(cookie().maxAge("uap_at", 0))
				.andExpect(cookie().maxAge("uap_rt", 0));
	}

	@Test
	void meReturnsSafeAccountData() throws Exception {
		String email = uniqueEmail("me");
		AuthCookies cookies = loginCookies(email);

		mockMvc.perform(get("/api/v1/identity/me").cookie(cookies.access()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.accountId").exists())
				.andExpect(jsonPath("$.email").value(email))
				.andExpect(jsonPath("$.status").value("ACTIVE"))
				.andExpect(jsonPath("$.emailVerifiedAt").exists())
				.andExpect(jsonPath("$.password").doesNotExist())
				.andExpect(jsonPath("$.passwordHash").doesNotExist())
				.andExpect(content().string(not(containsString("$2a$"))));
	}

	@Test
	void protectedEndpointRejectsMissingAndMalformedAccessTokens() throws Exception {
		mockMvc.perform(get("/api/v1/identity/me"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));

		mockMvc.perform(get("/api/v1/identity/me").cookie(new Cookie("uap_at", "not.a.jwt")))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));

		mockMvc.perform(get("/api/v1/identity/me")
						.cookie(new Cookie("uap_at",
								"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjMifQ.invalid")))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
	}

	@Test
	void csrfIsEnforcedOnAuthenticatedCookieMutations() throws Exception {
		AuthCookies cookies = loginCookies(uniqueEmail("csrf"));

		mockMvc.perform(post("/api/v1/identity/refresh").cookie(cookies.refresh()))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("CSRF_INVALID"));

		mockMvc.perform(post("/api/v1/identity/logout")
						.cookie(cookies.access(), cookies.refresh()))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("CSRF_INVALID"));

		mockMvc.perform(post("/api/v1/identity/logout-all")
						.cookie(cookies.access(), cookies.refresh()))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("CSRF_INVALID"));
	}

	@Test
	void corsAllowsConfiguredOriginAndRejectsOthers() throws Exception {
		mockMvc.perform(options("/api/v1/identity/login")
						.header("Origin", "http://localhost:3000")
						.header("Access-Control-Request-Method", "POST")
						.header("Access-Control-Request-Headers", "content-type"))
				.andExpect(status().isOk())
				.andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"))
				.andExpect(header().string("Access-Control-Allow-Credentials", "true"));

		mockMvc.perform(options("/api/v1/identity/login")
						.header("Origin", "https://evil.example")
						.header("Access-Control-Request-Method", "POST"))
				.andExpect(header().doesNotExist("Access-Control-Allow-Origin"));

		mockMvc.perform(post("/api/v1/identity/login")
						.header("Origin", "https://evil.example")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email":"a@b.com","password":"x"}
								"""))
				.andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
	}

	private AuthCookies loginCookies(String email) throws Exception {
		registerVerified(email);
		MvcResult result = mockMvc.perform(post("/api/v1/identity/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email":"%s","password":"%s"}
								""".formatted(email, PASSWORD)))
				.andExpect(status().isOk())
				.andReturn();
		return new AuthCookies(
				result.getResponse().getCookie("uap_at"),
				result.getResponse().getCookie("uap_rt"));
	}

	private void registerVerified(String email) throws Exception {
		register(email, PASSWORD);
		String token = verificationNotifier.lastMessage().orElseThrow().rawToken();
		mockMvc.perform(post("/api/v1/identity/verify-email")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"token":"%s"}
								""".formatted(token)))
				.andExpect(status().isNoContent());
	}

	private ResultActions register(String email, String password) throws Exception {
		return mockMvc.perform(post("/api/v1/identity/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email":"%s","password":"%s"}
								""".formatted(email, password)))
				.andExpect(status().isCreated());
	}

	private static String uniqueEmail(String prefix) {
		return prefix + "." + EMAIL_SEQ.incrementAndGet() + "." + UUID.randomUUID() + "@example.com";
	}

	private record AuthCookies(Cookie access, Cookie refresh) {
	}

}
