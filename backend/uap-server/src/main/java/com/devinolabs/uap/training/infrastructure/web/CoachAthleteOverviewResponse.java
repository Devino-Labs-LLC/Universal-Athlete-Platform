package com.devinolabs.uap.training.infrastructure.web;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.devinolabs.uap.training.application.CoachAthleteOverviewResult;
import com.devinolabs.uap.training.application.CoachAthleteOverviewResult.CoachOverviewSection;
import com.devinolabs.uap.training.application.CoachAthleteOverviewResult.DiscomfortData;
import com.devinolabs.uap.training.application.CoachAthleteOverviewResult.LimitingDimensionsData;
import com.devinolabs.uap.training.application.CoachAthleteOverviewResult.PerformanceHistoryData;
import com.devinolabs.uap.training.application.CoachAthleteOverviewResult.PerformanceHistoryEntry;
import com.devinolabs.uap.training.application.CoachAthleteOverviewResult.RatingData;
import com.devinolabs.uap.training.application.CoachAthleteOverviewResult.ReadinessCategoryData;
import com.devinolabs.uap.training.application.CoachAthleteOverviewResult.ReadinessScoreData;
import com.devinolabs.uap.training.application.CoachAthleteOverviewResult.RecoveryCheckInData;
import com.devinolabs.uap.training.application.CoachAthleteOverviewResult.TrainingAdherenceData;
import com.devinolabs.uap.training.application.CoachOverviewSectionStatus;

record CoachAthleteOverviewResponse(
		UUID teamId,
		UUID organizationId,
		UUID athleteId,
		UUID membershipId,
		String displayName,
		String role,
		LocalDate viewDate,
		Set<String> effectiveScopes,
		SectionResponse<Void> availability,
		SectionResponse<ReadinessCategoryPayload> readinessCategory,
		SectionResponse<ReadinessScorePayload> readinessScore,
		SectionResponse<LimitingDimensionsPayload> limitingDimensions,
		SectionResponse<RecoveryCheckInPayload> recoveryCheckIn,
		SectionResponse<TrainingAdherencePayload> trainingAdherence,
		SectionResponse<PerformanceHistoryPayload> performanceHistory) {

	static CoachAthleteOverviewResponse from(CoachAthleteOverviewResult result) {
		return new CoachAthleteOverviewResponse(
				result.teamId(),
				result.organizationId(),
				result.athleteId(),
				result.membershipId(),
				result.displayName(),
				result.role(),
				result.viewDate(),
				result.effectiveScopes(),
				SectionResponse.from(result.availability(), ignored -> null),
				SectionResponse.from(result.readinessCategory(), ReadinessCategoryPayload::from),
				SectionResponse.from(result.readinessScore(), ReadinessScorePayload::from),
				SectionResponse.from(result.limitingDimensions(), LimitingDimensionsPayload::from),
				SectionResponse.from(result.recoveryCheckIn(), RecoveryCheckInPayload::from),
				SectionResponse.from(result.trainingAdherence(), TrainingAdherencePayload::from),
				SectionResponse.from(result.performanceHistory(), PerformanceHistoryPayload::from));
	}

	record SectionResponse<T>(CoachOverviewSectionStatus status, T data) {

		static <S, T> SectionResponse<T> from(CoachOverviewSection<S> section, java.util.function.Function<S, T> mapper) {
			if (section.status() != CoachOverviewSectionStatus.AVAILABLE || section.data() == null) {
				return new SectionResponse<>(section.status(), null);
			}
			return new SectionResponse<>(section.status(), mapper.apply(section.data()));
		}
	}

	record ReadinessCategoryPayload(String readinessBand, String dataSufficiency) {

		static ReadinessCategoryPayload from(ReadinessCategoryData data) {
			return new ReadinessCategoryPayload(data.readinessBand(), data.dataSufficiency());
		}
	}

	record ReadinessScorePayload(
			BigDecimal readinessScore,
			String dataSufficiency,
			String summaryReasonCode) {

		static ReadinessScorePayload from(ReadinessScoreData data) {
			return new ReadinessScorePayload(
					data.readinessScore(),
					data.dataSufficiency(),
					data.summaryReasonCode());
		}
	}

	record LimitingDimensionsPayload(List<String> limitingDimensions) {

		static LimitingDimensionsPayload from(LimitingDimensionsData data) {
			return new LimitingDimensionsPayload(List.copyOf(data.limitingDimensions()));
		}
	}

	record RatingPayload(int value, String label) {

		static RatingPayload from(RatingData data) {
			return data == null ? null : new RatingPayload(data.value(), data.label());
		}
	}

	record DiscomfortPayload(
			String bodyArea,
			String side,
			RatingPayload intensity,
			String notes,
			int orderIndex) {

		static DiscomfortPayload from(DiscomfortData data) {
			return new DiscomfortPayload(
					data.bodyArea(),
					data.side(),
					RatingPayload.from(data.intensity()),
					data.notes(),
					data.orderIndex());
		}
	}

	record RecoveryCheckInPayload(
			LocalDate checkInDate,
			RatingPayload sleepQuality,
			RatingPayload mood,
			RatingPayload fatigue,
			RatingPayload muscleSoreness,
			RatingPayload stress,
			String notes,
			String completeness,
			List<DiscomfortPayload> discomfortAreas) {

		static RecoveryCheckInPayload from(RecoveryCheckInData data) {
			return new RecoveryCheckInPayload(
					data.checkInDate(),
					RatingPayload.from(data.sleepQuality()),
					RatingPayload.from(data.mood()),
					RatingPayload.from(data.fatigue()),
					RatingPayload.from(data.muscleSoreness()),
					RatingPayload.from(data.stress()),
					data.notes(),
					data.completeness(),
					data.discomfortAreas().stream().map(DiscomfortPayload::from).toList());
		}
	}

	record TrainingAdherencePayload(
			int scheduledCount,
			int completedCount,
			int skippedCount,
			int inProgressCount,
			int cancelledCount) {

		static TrainingAdherencePayload from(TrainingAdherenceData data) {
			return new TrainingAdherencePayload(
					data.scheduledCount(),
					data.completedCount(),
					data.skippedCount(),
					data.inProgressCount(),
					data.cancelledCount());
		}
	}

	record PerformanceHistoryEntryPayload(
			String exerciseName,
			String recordType,
			String recordQualifier,
			Instant achievedAt,
			LocalDate scheduledDate) {

		static PerformanceHistoryEntryPayload from(PerformanceHistoryEntry entry) {
			return new PerformanceHistoryEntryPayload(
					entry.exerciseName(),
					entry.recordType(),
					entry.recordQualifier(),
					entry.achievedAt(),
					entry.scheduledDate());
		}
	}

	record PerformanceHistoryPayload(List<PerformanceHistoryEntryPayload> recentRecords) {

		static PerformanceHistoryPayload from(PerformanceHistoryData data) {
			return new PerformanceHistoryPayload(
					data.recentRecords().stream().map(PerformanceHistoryEntryPayload::from).toList());
		}
	}

}
