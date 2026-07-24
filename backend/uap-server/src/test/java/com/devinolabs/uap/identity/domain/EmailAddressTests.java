package com.devinolabs.uap.identity.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class EmailAddressTests {

	@Test
	void normalizesEmailByTrimmingAndLowercasing() {
		EmailAddress email = EmailAddress.of("  Alex.Rider@Example.COM ");

		assertThat(email.value()).isEqualTo("alex.rider@example.com");
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = { " ", "\t", "not-an-email", "missing-domain@", "@missing-local.com", "a@b" })
	void rejectsBlankOrMalformedEmails(String rawEmail) {
		assertThatThrownBy(() -> EmailAddress.of(rawEmail))
				.isInstanceOf(IllegalArgumentException.class);
	}

}
