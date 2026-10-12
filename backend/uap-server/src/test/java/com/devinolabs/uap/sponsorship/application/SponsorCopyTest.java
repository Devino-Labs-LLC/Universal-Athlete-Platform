package com.devinolabs.uap.sponsorship.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class SponsorCopyTest {

	@Test
	void trimsPlainText() {
		assertThat(SponsorCopy.requirePlain("  Partner hydration  ", "headline", 160))
				.isEqualTo("Partner hydration");
	}

	@Test
	void rejectsMarkupAndScripts() {
		assertThatThrownBy(() -> SponsorCopy.requirePlain("<script>x</script>", "body", 500))
				.isInstanceOf(SponsorInputInvalidException.class);
		assertThatThrownBy(() -> SponsorCopy.requirePlain("click javascript:void(0)", "ctaLabel", 40))
				.isInstanceOf(SponsorInputInvalidException.class);
	}
}
