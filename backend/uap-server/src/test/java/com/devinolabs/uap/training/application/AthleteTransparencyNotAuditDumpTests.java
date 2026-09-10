package com.devinolabs.uap.training.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Constructor;
import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.devinolabs.uap.audit.api.SecurityAuditRecord;
import com.devinolabs.uap.audit.api.SecurityAuditWriter;
import com.devinolabs.uap.audit.application.SecurityAuditEventRepository;
import com.devinolabs.uap.training.application.GetAthleteTransparencyUseCase.TransparencyEvent;

/**
 * Guards ADR-035: athlete transparency is a curated projection, not a raw
 * security-audit dump and must not depend on audit writer/repository wiring.
 */
class AthleteTransparencyNotAuditDumpTests {

	private static final Set<String> ALLOWED_EVENT_FIELDS = Set.of(
			"type",
			"occurredAt",
			"organizationName",
			"teamName",
			"description");

	/** Audit-only payload keys that must never appear on the athlete transparency event shape. */
	private static final Set<String> RAW_AUDIT_ONLY_FIELDS = Set.of(
			"id",
			"eventType",
			"actorAccountId",
			"subjectAccountId",
			"subjectAthleteId",
			"organizationId",
			"teamId",
			"resourceType",
			"resourceId",
			"metadataJson");

	@Test
	void constructorDoesNotDependOnSecurityAuditWriterOrRepository() {
		Constructor<?>[] constructors = GetAthleteTransparencyUseCase.class.getDeclaredConstructors();
		assertThat(constructors).hasSize(1);

		Set<Class<?>> parameterTypes = Arrays.stream(constructors[0].getParameterTypes())
				.collect(Collectors.toUnmodifiableSet());

		assertThat(parameterTypes)
				.doesNotContain(SecurityAuditWriter.class)
				.doesNotContain(SecurityAuditEventRepository.class)
				.noneMatch(type -> type.getName().contains("SecurityAudit"));
	}

	@Test
	void eventAllowListExcludesRawAuditPayloadFields() {
		Set<String> eventFields = Arrays.stream(TransparencyEvent.class.getRecordComponents())
				.map(RecordComponent::getName)
				.collect(Collectors.toUnmodifiableSet());
		Set<String> auditFields = Arrays.stream(SecurityAuditRecord.class.getRecordComponents())
				.map(RecordComponent::getName)
				.collect(Collectors.toUnmodifiableSet());

		assertThat(auditFields).containsAll(RAW_AUDIT_ONLY_FIELDS);
		assertThat(eventFields).isEqualTo(ALLOWED_EVENT_FIELDS);
		assertThat(eventFields).doesNotContainAnyElementsOf(RAW_AUDIT_ONLY_FIELDS);
	}

}
