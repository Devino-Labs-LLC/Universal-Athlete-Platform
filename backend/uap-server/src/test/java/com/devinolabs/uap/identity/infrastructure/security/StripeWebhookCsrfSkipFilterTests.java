package com.devinolabs.uap.identity.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;

class StripeWebhookCsrfSkipFilterTests {

	private static final String CSRF_SKIP_ATTRIBUTE = "SHOULD_NOT_FILTER" + CsrfFilter.class.getName();

	private final IdentitySecurityConfiguration.StripeWebhookCsrfSkipFilter skipFilter =
			new IdentitySecurityConfiguration.StripeWebhookCsrfSkipFilter();

	@Test
	void stripeWebhookPostIsSkippedSoCsrfFilterDoesNotRequireAToken() throws Exception {
		MockHttpServletRequest request = request(HttpMethod.POST, IdentitySecurityConfiguration.STRIPE_WEBHOOK_PATH);
		MockHttpServletResponse response = new MockHttpServletResponse();

		skipFilter.doFilter(request, response, new MockFilterChain());

		assertThat(request.getAttribute(CSRF_SKIP_ATTRIBUTE)).isEqualTo(Boolean.TRUE);

		CsrfFilter csrfFilter = new CsrfFilter(CookieCsrfTokenRepository.withHttpOnlyFalse());
		MockFilterChain remaining = new MockFilterChain();
		csrfFilter.doFilter(request, response, remaining);

		assertThat(remaining.getRequest()).isSameAs(request);
		assertThat(response.getStatus()).isNotEqualTo(403);
	}

	@Test
	void billingCheckoutPostIsNotSkipped() throws Exception {
		MockHttpServletRequest request = request(
				HttpMethod.POST,
				"/api/v1/billing/organizations/aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee/checkout-sessions");

		skipFilter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

		assertThat(request.getAttribute(CSRF_SKIP_ATTRIBUTE)).isNull();
	}

	private static MockHttpServletRequest request(HttpMethod method, String path) {
		MockHttpServletRequest request = new MockHttpServletRequest(method.name(), path);
		request.setServletPath(path);
		return request;
	}

}
