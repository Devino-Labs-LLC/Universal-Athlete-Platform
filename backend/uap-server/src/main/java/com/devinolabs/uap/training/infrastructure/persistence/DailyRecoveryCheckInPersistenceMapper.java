package com.devinolabs.uap.training.infrastructure.persistence;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.BodyAreaDiscomfortObservation;
import com.devinolabs.uap.training.domain.DailyRecoveryCheckIn;
import com.devinolabs.uap.training.domain.DailyRecoveryCheckInId;
import com.devinolabs.uap.training.domain.DailyRecoveryCheckInRevision;
import com.devinolabs.uap.training.domain.DailyRecoveryCheckInRevisionId;
import com.devinolabs.uap.training.domain.DiscomfortIntensity;
import com.devinolabs.uap.training.domain.DiscomfortSnapshotSide;
import com.devinolabs.uap.training.domain.FatigueRating;
import com.devinolabs.uap.training.domain.MoodRating;
import com.devinolabs.uap.training.domain.MuscleSorenessRating;
import com.devinolabs.uap.training.domain.SleepQualityRating;
import com.devinolabs.uap.training.domain.StressRating;
import com.devinolabs.uap.training.domain.TrainingMotivationRating;

final class DailyRecoveryCheckInPersistenceMapper {

	private DailyRecoveryCheckInPersistenceMapper() {
	}

	static DailyRecoveryCheckIn toDomain(DailyRecoveryCheckInJpaEntity entity) {
		List<BodyAreaDiscomfortObservation> discomfort = entity.getDiscomfort().stream()
				.sorted(Comparator.comparingInt(DailyRecoveryCheckInDiscomfortJpaEntity::getOrderIndex))
				.map(DailyRecoveryCheckInPersistenceMapper::toDiscomfortDomain)
				.toList();
		return DailyRecoveryCheckIn.rehydrate(
				DailyRecoveryCheckInId.of(entity.getId()),
				AthleteId.of(entity.getAthleteId()),
				entity.getCheckInDate(),
				entity.getSleepDurationMinutes(),
				entity.getSleepQuality() == null ? null : SleepQualityRating.of(entity.getSleepQuality()),
				FatigueRating.of(entity.getFatigue()),
				MuscleSorenessRating.of(entity.getMuscleSoreness()),
				StressRating.of(entity.getStress()),
				MoodRating.of(entity.getMood()),
				TrainingMotivationRating.of(entity.getMotivation()),
				entity.getCompleteness(),
				discomfort,
				entity.getNotes(),
				entity.getSource(),
				entity.getSubmittedAt(),
				entity.getCreatedAt(),
				entity.getUpdatedAt(),
				entity.getVersion());
	}

	static DailyRecoveryCheckInJpaEntity toEntity(DailyRecoveryCheckIn checkIn, boolean isNew) {
		DailyRecoveryCheckInJpaEntity entity = new DailyRecoveryCheckInJpaEntity();
		entity.setId(checkIn.id().value());
		entity.setAthleteId(checkIn.athleteId().value());
		entity.setCheckInDate(checkIn.checkInDate());
		entity.setSleepDurationMinutes(checkIn.sleepDurationMinutes());
		entity.setSleepQuality(checkIn.sleepQuality() == null ? null : checkIn.sleepQuality().value());
		entity.setFatigue(checkIn.fatigue().value());
		entity.setMuscleSoreness(checkIn.muscleSoreness().value());
		entity.setStress(checkIn.stress().value());
		entity.setMood(checkIn.mood().value());
		entity.setMotivation(checkIn.motivation().value());
		entity.setCompleteness(checkIn.completeness());
		entity.setNotes(checkIn.notes());
		entity.setSource(checkIn.source());
		entity.setSubmittedAt(checkIn.submittedAt());
		entity.setCreatedAt(checkIn.createdAt());
		entity.setUpdatedAt(checkIn.updatedAt());
		entity.setVersion(checkIn.version());
		entity.setNew(isNew);
		entity.getDiscomfort().clear();
		for (BodyAreaDiscomfortObservation observation : checkIn.discomfortAreas()) {
			DailyRecoveryCheckInDiscomfortJpaEntity discomfortEntity = new DailyRecoveryCheckInDiscomfortJpaEntity();
			discomfortEntity.setId(UUID.randomUUID());
			discomfortEntity.setCheckIn(entity);
			discomfortEntity.setBodyArea(observation.bodyArea());
			discomfortEntity.setBodySide(observation.side());
			discomfortEntity.setIntensity(observation.intensity().value());
			discomfortEntity.setNotes(observation.notes());
			discomfortEntity.setOrderIndex(observation.orderIndex());
			discomfortEntity.setNew(true);
			entity.getDiscomfort().add(discomfortEntity);
		}
		return entity;
	}

	static void applyMutableFields(DailyRecoveryCheckInJpaEntity entity, DailyRecoveryCheckIn checkIn) {
		entity.setSleepDurationMinutes(checkIn.sleepDurationMinutes());
		entity.setSleepQuality(checkIn.sleepQuality() == null ? null : checkIn.sleepQuality().value());
		entity.setFatigue(checkIn.fatigue().value());
		entity.setMuscleSoreness(checkIn.muscleSoreness().value());
		entity.setStress(checkIn.stress().value());
		entity.setMood(checkIn.mood().value());
		entity.setMotivation(checkIn.motivation().value());
		entity.setCompleteness(checkIn.completeness());
		entity.setNotes(checkIn.notes());
		entity.setSubmittedAt(checkIn.submittedAt());
		entity.setUpdatedAt(checkIn.updatedAt());
		entity.getDiscomfort().clear();
		for (BodyAreaDiscomfortObservation observation : checkIn.discomfortAreas()) {
			DailyRecoveryCheckInDiscomfortJpaEntity discomfortEntity = new DailyRecoveryCheckInDiscomfortJpaEntity();
			discomfortEntity.setId(UUID.randomUUID());
			discomfortEntity.setCheckIn(entity);
			discomfortEntity.setBodyArea(observation.bodyArea());
			discomfortEntity.setBodySide(observation.side());
			discomfortEntity.setIntensity(observation.intensity().value());
			discomfortEntity.setNotes(observation.notes());
			discomfortEntity.setOrderIndex(observation.orderIndex());
			discomfortEntity.setNew(true);
			entity.getDiscomfort().add(discomfortEntity);
		}
	}

	static DailyRecoveryCheckInRevision toRevisionDomain(DailyRecoveryCheckInRevisionJpaEntity entity) {
		List<BodyAreaDiscomfortObservation> prior = entity.getDiscomfort().stream()
				.filter(item -> item.getSnapshotSide() == DiscomfortSnapshotSide.PRIOR)
				.sorted(Comparator.comparingInt(DailyRecoveryCheckInRevisionDiscomfortJpaEntity::getOrderIndex))
				.map(DailyRecoveryCheckInPersistenceMapper::toRevisionDiscomfortDomain)
				.toList();
		List<BodyAreaDiscomfortObservation> updated = entity.getDiscomfort().stream()
				.filter(item -> item.getSnapshotSide() == DiscomfortSnapshotSide.NEW)
				.sorted(Comparator.comparingInt(DailyRecoveryCheckInRevisionDiscomfortJpaEntity::getOrderIndex))
				.map(DailyRecoveryCheckInPersistenceMapper::toRevisionDiscomfortDomain)
				.toList();
		return DailyRecoveryCheckInRevision.rehydrate(
				DailyRecoveryCheckInRevisionId.of(entity.getId()),
				DailyRecoveryCheckInId.of(entity.getRecoveryCheckInId()),
				AthleteId.of(entity.getAthleteId()),
				entity.getRevisionNumber(),
				entity.getPriorSleepDurationMinutes(),
				entity.getNewSleepDurationMinutes(),
				entity.getPriorSleepQuality() == null ? null : SleepQualityRating.of(entity.getPriorSleepQuality()),
				entity.getNewSleepQuality() == null ? null : SleepQualityRating.of(entity.getNewSleepQuality()),
				FatigueRating.of(entity.getPriorFatigue()),
				FatigueRating.of(entity.getNewFatigue()),
				MuscleSorenessRating.of(entity.getPriorMuscleSoreness()),
				MuscleSorenessRating.of(entity.getNewMuscleSoreness()),
				StressRating.of(entity.getPriorStress()),
				StressRating.of(entity.getNewStress()),
				MoodRating.of(entity.getPriorMood()),
				MoodRating.of(entity.getNewMood()),
				TrainingMotivationRating.of(entity.getPriorMotivation()),
				TrainingMotivationRating.of(entity.getNewMotivation()),
				entity.getPriorCompleteness(),
				entity.getNewCompleteness(),
				entity.getPriorNotes(),
				entity.getNewNotes(),
				prior,
				updated,
				entity.getChangedAt(),
				entity.getCreatedAt());
	}

	static DailyRecoveryCheckInRevisionJpaEntity toRevisionEntity(DailyRecoveryCheckInRevision revision) {
		DailyRecoveryCheckInRevisionJpaEntity entity = new DailyRecoveryCheckInRevisionJpaEntity();
		entity.setId(revision.id().value());
		entity.setRecoveryCheckInId(revision.recoveryCheckInId().value());
		entity.setAthleteId(revision.athleteId().value());
		entity.setRevisionNumber(revision.revisionNumber());
		entity.setPriorSleepDurationMinutes(revision.priorSleepDurationMinutes());
		entity.setNewSleepDurationMinutes(revision.newSleepDurationMinutes());
		entity.setPriorSleepQuality(revision.priorSleepQuality() == null ? null : revision.priorSleepQuality().value());
		entity.setNewSleepQuality(revision.newSleepQuality() == null ? null : revision.newSleepQuality().value());
		entity.setPriorFatigue(revision.priorFatigue().value());
		entity.setNewFatigue(revision.newFatigue().value());
		entity.setPriorMuscleSoreness(revision.priorMuscleSoreness().value());
		entity.setNewMuscleSoreness(revision.newMuscleSoreness().value());
		entity.setPriorStress(revision.priorStress().value());
		entity.setNewStress(revision.newStress().value());
		entity.setPriorMood(revision.priorMood().value());
		entity.setNewMood(revision.newMood().value());
		entity.setPriorMotivation(revision.priorMotivation().value());
		entity.setNewMotivation(revision.newMotivation().value());
		entity.setPriorCompleteness(revision.priorCompleteness());
		entity.setNewCompleteness(revision.newCompleteness());
		entity.setPriorNotes(revision.priorNotes());
		entity.setNewNotes(revision.newNotes());
		entity.setChangedAt(revision.changedAt());
		entity.setCreatedAt(revision.createdAt());
		entity.setNew(true);
		addRevisionDiscomfort(entity, revision.priorDiscomfort(), DiscomfortSnapshotSide.PRIOR);
		addRevisionDiscomfort(entity, revision.newDiscomfort(), DiscomfortSnapshotSide.NEW);
		return entity;
	}

	private static void addRevisionDiscomfort(
			DailyRecoveryCheckInRevisionJpaEntity entity,
			List<BodyAreaDiscomfortObservation> observations,
			DiscomfortSnapshotSide side) {
		for (BodyAreaDiscomfortObservation observation : observations) {
			DailyRecoveryCheckInRevisionDiscomfortJpaEntity discomfortEntity =
					new DailyRecoveryCheckInRevisionDiscomfortJpaEntity();
			discomfortEntity.setId(UUID.randomUUID());
			discomfortEntity.setRevision(entity);
			discomfortEntity.setSnapshotSide(side);
			discomfortEntity.setBodyArea(observation.bodyArea());
			discomfortEntity.setBodySide(observation.side());
			discomfortEntity.setIntensity(observation.intensity().value());
			discomfortEntity.setNotes(observation.notes());
			discomfortEntity.setOrderIndex(observation.orderIndex());
			discomfortEntity.setNew(true);
			entity.getDiscomfort().add(discomfortEntity);
		}
	}

	private static BodyAreaDiscomfortObservation toDiscomfortDomain(DailyRecoveryCheckInDiscomfortJpaEntity entity) {
		return BodyAreaDiscomfortObservation.of(
				entity.getBodyArea(),
				entity.getBodySide(),
				DiscomfortIntensity.of(entity.getIntensity()),
				entity.getNotes(),
				entity.getOrderIndex());
	}

	private static BodyAreaDiscomfortObservation toRevisionDiscomfortDomain(
			DailyRecoveryCheckInRevisionDiscomfortJpaEntity entity) {
		return BodyAreaDiscomfortObservation.of(
				entity.getBodyArea(),
				entity.getBodySide(),
				DiscomfortIntensity.of(entity.getIntensity()),
				entity.getNotes(),
				entity.getOrderIndex());
	}

}
