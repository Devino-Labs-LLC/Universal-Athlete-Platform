package com.devinolabs.uap.athlete.infrastructure.persistence;

import com.devinolabs.uap.athlete.domain.AccountId;
import com.devinolabs.uap.athlete.domain.Athlete;
import com.devinolabs.uap.athlete.domain.AthleteId;
import com.devinolabs.uap.athlete.domain.Height;
import com.devinolabs.uap.athlete.domain.Weight;

final class AthletePersistenceMapper {

	private AthletePersistenceMapper() {
	}

	static AthleteJpaEntity toEntity(Athlete athlete, boolean isNew) {
		return new AthleteJpaEntity(
				athlete.id().value(),
				athlete.accountId().value(),
				athlete.firstName(),
				athlete.lastName(),
				athlete.dateOfBirth(),
				athlete.sex(),
				athlete.height().centimeters(),
				athlete.weight().kilograms(),
				athlete.dominantHand(),
				athlete.dominantFoot(),
				athlete.status(),
				athlete.createdAt(),
				athlete.updatedAt(),
				athlete.version(),
				isNew);
	}

	static Athlete toDomain(AthleteJpaEntity entity) {
		return Athlete.rehydrate(
				AthleteId.of(entity.getId()),
				AccountId.of(entity.getAccountId()),
				entity.getFirstName(),
				entity.getLastName(),
				entity.getDateOfBirth(),
				entity.getSex(),
				Height.ofCentimeters(entity.getHeightCm()),
				Weight.ofKilograms(entity.getWeightKg()),
				entity.getDominantHand(),
				entity.getDominantFoot(),
				entity.getStatus(),
				entity.getCreatedAt(),
				entity.getUpdatedAt(),
				entity.getVersion());
	}

}
