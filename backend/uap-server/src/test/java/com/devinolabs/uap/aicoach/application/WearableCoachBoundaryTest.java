package com.devinolabs.uap.aicoach.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

import com.devinolabs.uap.integrations.application.WearableProviderAdapter;

class WearableCoachBoundaryTest {

	@Test
	void coachCannotIssueDeviceOrBleCommands() {
		assertThat(Arrays.stream(CoachContextBuilder.class.getDeclaredFields()).map(Field::getType))
				.doesNotContain(WearableProviderAdapter.class);
		assertThat(Arrays.stream(CoachContextBuilder.class.getDeclaredMethods()).map(method -> method.getName()))
				.noneMatch(name -> name.toLowerCase().contains("ble")
						|| name.toLowerCase().contains("connect")
						|| name.toLowerCase().contains("disconnect"));
		assertThat(CoachPromptComposer.class.getDeclaredFields())
				.extracting(Field::getType)
				.doesNotContain(WearableProviderAdapter.class);
	}
}
