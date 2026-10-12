package com.devinolabs.uap.aicoach.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

import com.devinolabs.uap.sponsorship.application.PlacementService;
import com.devinolabs.uap.sponsorship.application.SponsorshipCatalogService;
import com.devinolabs.uap.sponsorship.application.SponsorshipReportingService;

class SponsorshipIsolationFromCoachTest {

	@Test
	void coachContextAndPromptPathHaveNoSponsorFieldsOrDependencies() {
		assertThat(Arrays.stream(CoachContext.class.getDeclaredFields()).map(Field::getName))
				.noneMatch(name -> name.toLowerCase().contains("sponsor")
						|| name.toLowerCase().contains("campaign")
						|| name.toLowerCase().contains("placement"));
		assertThat(CoachContextBuilder.class.getDeclaredFields())
				.extracting(Field::getType)
				.doesNotContain(PlacementService.class, SponsorshipCatalogService.class, SponsorshipReportingService.class);
		assertThat(CoachPromptComposer.class.getDeclaredFields())
				.extracting(Field::getType)
				.doesNotContain(PlacementService.class);
	}
}
