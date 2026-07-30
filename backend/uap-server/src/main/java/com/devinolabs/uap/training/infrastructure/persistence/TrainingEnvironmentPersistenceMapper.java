package com.devinolabs.uap.training.infrastructure.persistence;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;

import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.EquipmentType;
import com.devinolabs.uap.training.domain.TrainingEnvironment;
import com.devinolabs.uap.training.domain.TrainingEnvironmentId;

final class TrainingEnvironmentPersistenceMapper {

	private TrainingEnvironmentPersistenceMapper() {
	}

	static TrainingEnvironmentJpaEntity toEntity(TrainingEnvironment environment, boolean isNew) {
		return new TrainingEnvironmentJpaEntity(
				environment.id().value(),
				environment.athleteId().value(),
				environment.name(),
				environment.normalizedName(),
				environment.type(),
				environment.description(),
				environment.facilityNotes(),
				environment.defaultEnvironment(),
				environment.active(),
				environment.archivedAt(),
				environment.createdAt(),
				environment.updatedAt(),
				environment.version(),
				new LinkedHashSet<>(environment.availableEquipment()),
				isNew);
	}

	static TrainingEnvironment toDomain(TrainingEnvironmentJpaEntity entity) {
		List<EquipmentType> equipment = new ArrayList<>(entity.getAvailableEquipment());
		equipment.sort(Comparator.comparingInt(Enum::ordinal));
		return TrainingEnvironment.rehydrate(
				TrainingEnvironmentId.of(entity.getId()),
				AthleteId.of(entity.getAthleteId()),
				entity.getName(),
				entity.getNormalizedName(),
				entity.getEnvironmentType(),
				equipment,
				entity.getDescription(),
				entity.getFacilityNotes(),
				entity.isDefaultEnvironment(),
				entity.isActive(),
				entity.getArchivedAt(),
				entity.getCreatedAt(),
				entity.getUpdatedAt(),
				entity.getVersion());
	}

}
