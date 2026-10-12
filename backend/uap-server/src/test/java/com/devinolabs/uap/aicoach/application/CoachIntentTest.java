package com.devinolabs.uap.aicoach.application;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class CoachIntentTest {

	@Test
	void classifiesTheSupportedQuestions() {
		assertEquals(CoachIntent.TODAY_STATUS, CoachIntent.classify("How am I looking today?"));
		assertEquals(CoachIntent.READINESS_EXPLANATION, CoachIntent.classify("Why is my readiness lower today?"));
		assertEquals(CoachIntent.RECOMMENDATION_EXPLANATION, CoachIntent.classify("Why did UAP recommend this?"));
		assertEquals(CoachIntent.TODAY_FOCUS, CoachIntent.classify("What should I focus on today?"));
		assertEquals(CoachIntent.RECENT_TRAINING, CoachIntent.classify("How has my recent training affected today?"));
		assertEquals(CoachIntent.SHORT_HORIZON, CoachIntent.classify("What should I keep in mind for the next few days?"));
	}

}
