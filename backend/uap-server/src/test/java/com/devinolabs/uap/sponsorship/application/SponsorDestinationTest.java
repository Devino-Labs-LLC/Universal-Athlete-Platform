package com.devinolabs.uap.sponsorship.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class SponsorDestinationTest {

	@Test
	void acceptsHttpsDestinations() {
		assertThat(SponsorDestination.requireHttps("https://partners.example.com/offer", "destinationUrl"))
				.isEqualTo("https://partners.example.com/offer");
	}

	@Test
	void rejectsUnsafeSchemesAndLocalHosts() {
		assertThatThrownBy(() -> SponsorDestination.requireHttps("http://partners.example.com", "destinationUrl"))
				.isInstanceOf(SponsorInputInvalidException.class);
		assertThatThrownBy(() -> SponsorDestination.requireHttps("javascript:alert(1)", "destinationUrl"))
				.isInstanceOf(SponsorInputInvalidException.class);
		assertThatThrownBy(() -> SponsorDestination.requireHttps("https://localhost/path", "logoUrl"))
				.isInstanceOf(SponsorInputInvalidException.class);
		assertThatThrownBy(() -> SponsorDestination.optionalHttps("https://user:pass@example.com", "logoUrl"))
				.isInstanceOf(SponsorInputInvalidException.class);
	}

	@Test
	void blankOptionalUrlIsOmitted() {
		assertThat(SponsorDestination.optionalHttps("  ", "logoUrl")).isNull();
	}
}
