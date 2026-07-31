package com.devinolabs.uap.training.infrastructure.persistence;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.EquipmentType;
import com.devinolabs.uap.training.domain.ExerciseDefinitionId;
import com.devinolabs.uap.training.domain.ExercisePerformanceKey;
import com.devinolabs.uap.training.domain.ExerciseSubstitutionRelationshipId;
import com.devinolabs.uap.training.domain.FeasibilityEnvironmentContextSource;
import com.devinolabs.uap.training.domain.TrainingEnvironmentId;
import com.devinolabs.uap.training.domain.TrainingPlanId;
import com.devinolabs.uap.training.domain.WorkoutAdaptationAction;
import com.devinolabs.uap.training.domain.WorkoutAdaptationAlternative;
import com.devinolabs.uap.training.domain.WorkoutAdaptationAlternativeId;
import com.devinolabs.uap.training.domain.WorkoutAdaptationDecision;
import com.devinolabs.uap.training.domain.WorkoutAdaptationFeasibilityFingerprint;
import com.devinolabs.uap.training.domain.WorkoutAdaptationProposal;
import com.devinolabs.uap.training.domain.WorkoutAdaptationProposalId;
import com.devinolabs.uap.training.domain.WorkoutAdaptationProposalItem;
import com.devinolabs.uap.training.domain.WorkoutAdaptationProposalItemId;
import com.devinolabs.uap.training.domain.WorkoutAdaptationProposalStatus;
import com.devinolabs.uap.training.domain.WorkoutDayId;
import com.devinolabs.uap.training.domain.WorkoutExerciseExecutionId;
import com.devinolabs.uap.training.domain.WorkoutExerciseId;
import com.devinolabs.uap.training.domain.WorkoutOccurrenceId;

final class WorkoutAdaptationProposalPersistenceMapper {

	private WorkoutAdaptationProposalPersistenceMapper() {
	}

	static WorkoutAdaptationProposalJpaEntity toEntity(WorkoutAdaptationProposal proposal) {
		WorkoutAdaptationProposalJpaEntity entity = new WorkoutAdaptationProposalJpaEntity();
		applyProposalFields(entity, proposal);
		entity.setItems(toItemEntities(proposal.items(), entity));
		return entity;
	}

	static WorkoutAdaptationProposalJpaEntity toEntity(
			WorkoutAdaptationProposal proposal,
			WorkoutAdaptationProposalJpaEntity existing) {
		applyProposalFields(existing, proposal);
		syncItems(existing, proposal.items());
		return existing;
	}

	static WorkoutAdaptationProposal toDomain(WorkoutAdaptationProposalJpaEntity entity) {
		List<WorkoutAdaptationProposalItem> items = entity.getItems().stream()
				.map(WorkoutAdaptationProposalPersistenceMapper::toItemDomain)
				.toList();
		WorkoutAdaptationProposal proposal = WorkoutAdaptationProposal.rehydrate(
				WorkoutAdaptationProposalId.of(entity.getId()),
				AthleteId.of(entity.getAthleteId()),
				TrainingPlanId.of(entity.getTrainingPlanId()),
				WorkoutDayId.of(entity.getWorkoutDayId()),
				WorkoutOccurrenceId.of(entity.getWorkoutOccurrenceId()),
				entity.getEnvironmentContextSource(),
				entity.getTrainingEnvironmentId() == null
						? null
						: TrainingEnvironmentId.of(entity.getTrainingEnvironmentId()),
				entity.getEnvironmentNameSnapshot(),
				sortedEquipment(entity.getAvailableEquipmentSnapshot()),
				entity.getOccurrenceVersionAtGeneration(),
				entity.getOccurrenceUpdatedAtAtGeneration(),
				WorkoutAdaptationFeasibilityFingerprint.of(entity.getFeasibilityFingerprint()),
				entity.getStatus(),
				entity.getTotalExecutions(),
				entity.getAlreadyFeasibleExecutions(),
				entity.getProposedSubstitutions(),
				entity.getUnresolvedExecutions(),
				entity.getExcludedExecutions(),
				entity.getExpectedFeasibleExecutions(),
				entity.getExpectedFeasibilityPercentage(),
				0,
				0,
				0,
				entity.getGeneratedAt(),
				entity.getExpiresAt(),
				entity.getAppliedAt(),
				entity.getCancelledAt(),
				items,
				entity.getCreatedAt(),
				entity.getUpdatedAt(),
				entity.getVersion());
		proposal.refreshSummary();
		return proposal;
	}

	private static void applyProposalFields(
			WorkoutAdaptationProposalJpaEntity entity,
			WorkoutAdaptationProposal proposal) {
		entity.setId(proposal.id().value());
		entity.setAthleteId(proposal.athleteId().value());
		entity.setTrainingPlanId(proposal.trainingPlanId().value());
		entity.setWorkoutDayId(proposal.workoutDayId().value());
		entity.setWorkoutOccurrenceId(proposal.workoutOccurrenceId().value());
		entity.setEnvironmentContextSource(proposal.environmentContextSource());
		entity.setTrainingEnvironmentId(proposal.trainingEnvironmentId()
				.map(TrainingEnvironmentId::value)
				.orElse(null));
		entity.setEnvironmentNameSnapshot(proposal.environmentNameSnapshot());
		entity.setAvailableEquipmentSnapshot(new LinkedHashSet<>(proposal.availableEquipmentSnapshot()));
		entity.setOccurrenceVersionAtGeneration(proposal.occurrenceVersionAtGeneration());
		entity.setOccurrenceUpdatedAtAtGeneration(proposal.occurrenceUpdatedAtAtGeneration());
		entity.setFeasibilityFingerprint(proposal.feasibilityFingerprint().value());
		entity.setStatus(proposal.status());
		entity.setTotalExecutions(proposal.totalExecutions());
		entity.setAlreadyFeasibleExecutions(proposal.alreadyFeasibleExecutions());
		entity.setProposedSubstitutions(proposal.proposedSubstitutions());
		entity.setUnresolvedExecutions(proposal.unresolvedExecutions());
		entity.setExcludedExecutions(proposal.excludedExecutions());
		entity.setExpectedFeasibleExecutions(proposal.expectedFeasibleExecutions());
		entity.setExpectedFeasibilityPercentage(proposal.expectedFeasibilityPercentage());
		entity.setGeneratedAt(proposal.generatedAt());
		entity.setExpiresAt(proposal.expiresAt());
		entity.setAppliedAt(proposal.appliedAt());
		entity.setCancelledAt(proposal.cancelledAt());
		entity.setCreatedAt(proposal.createdAt());
		entity.setUpdatedAt(proposal.updatedAt());
		entity.setVersion(proposal.version());
	}

	private static void syncItems(
			WorkoutAdaptationProposalJpaEntity proposalEntity,
			List<WorkoutAdaptationProposalItem> items) {
		Map<java.util.UUID, WorkoutAdaptationProposalItemJpaEntity> existingById = new LinkedHashMap<>();
		for (WorkoutAdaptationProposalItemJpaEntity existingItem : proposalEntity.getItems()) {
			existingById.put(existingItem.getId(), existingItem);
		}
		List<WorkoutAdaptationProposalItemJpaEntity> synced = new ArrayList<>(items.size());
		for (WorkoutAdaptationProposalItem item : items) {
			WorkoutAdaptationProposalItemJpaEntity entity = existingById.remove(item.id().value());
			if (entity == null) {
				entity = new WorkoutAdaptationProposalItemJpaEntity();
				entity.setId(item.id().value());
				entity.setProposal(proposalEntity);
			}
			applyItemFields(entity, item);
			synced.add(entity);
		}
		proposalEntity.getItems().clear();
		proposalEntity.getItems().addAll(synced);
	}

	private static void applyItemFields(
			WorkoutAdaptationProposalItemJpaEntity entity,
			WorkoutAdaptationProposalItem item) {
		entity.setWorkoutExerciseExecutionId(item.workoutExerciseExecutionId().value());
		entity.setSourceWorkoutExerciseId(item.sourceWorkoutExerciseId().value());
		entity.setExecutionOrder(item.executionOrder());
		entity.setPrescribedExerciseDefinitionId(item.prescribedExerciseDefinitionId().value());
		entity.setPrescribedNameSnapshot(item.prescribedNameSnapshot());
		entity.setCurrentPerformedExerciseDefinitionId(item.currentPerformedExerciseDefinitionId().value());
		entity.setCurrentPerformedNameSnapshot(item.currentPerformedNameSnapshot());
		entity.setExercisePerformanceKeyAtGeneration(item.exercisePerformanceKeyAtGeneration().value());
		entity.setCurrentFeasible(item.currentFeasible());
		entity.setPrescribedFeasible(item.prescribedFeasible());
		entity.setPerformedFeasible(item.performedFeasible());
		entity.getMissingRequiredEquipment().clear();
		entity.getMissingRequiredEquipment().addAll(item.missingRequiredEquipment());
		entity.setAnalysisReasonCode(item.analysisReasonCode());
		entity.setAction(item.action());
		entity.setGeneratedTargetExerciseDefinitionId(
				item.generatedTargetExerciseDefinitionId() == null
						? null
						: item.generatedTargetExerciseDefinitionId().value());
		entity.setGeneratedTargetNameSnapshot(item.generatedTargetNameSnapshot());
		entity.setGeneratedRelationshipId(
				item.generatedRelationshipId() == null ? null : item.generatedRelationshipId().value());
		entity.setGeneratedRelationshipTypeSnapshot(item.generatedRelationshipTypeSnapshot());
		entity.setGeneratedCompatibilitySnapshot(item.generatedCompatibilitySnapshot());
		entity.setGeneratedRationaleSnapshot(item.generatedRationaleSnapshot());
		entity.setSelectedTargetExerciseDefinitionId(
				item.selectedTargetExerciseDefinitionId() == null
						? null
						: item.selectedTargetExerciseDefinitionId().value());
		entity.setSelectedRelationshipId(
				item.selectedRelationshipId() == null ? null : item.selectedRelationshipId().value());
		entity.setAthleteDecision(item.athleteDecision());
		entity.setAthleteNotes(item.athleteNotes());
		entity.setCreatedAt(item.createdAt());
		entity.setUpdatedAt(item.updatedAt());
		entity.setVersion(item.version());
		syncAlternatives(entity, item.alternatives());
	}

	private static void syncAlternatives(
			WorkoutAdaptationProposalItemJpaEntity itemEntity,
			List<WorkoutAdaptationAlternative> alternatives) {
		Map<java.util.UUID, WorkoutAdaptationProposalItemAlternativeJpaEntity> existingById = new LinkedHashMap<>();
		for (WorkoutAdaptationProposalItemAlternativeJpaEntity existingAlternative : itemEntity.getAlternatives()) {
			existingById.put(existingAlternative.getId(), existingAlternative);
		}
		List<WorkoutAdaptationProposalItemAlternativeJpaEntity> synced = new ArrayList<>(alternatives.size());
		for (WorkoutAdaptationAlternative alternative : alternatives) {
			WorkoutAdaptationProposalItemAlternativeJpaEntity entity = existingById.remove(alternative.id().value());
			if (entity == null) {
				entity = new WorkoutAdaptationProposalItemAlternativeJpaEntity();
				entity.setId(alternative.id().value());
				entity.setProposalItem(itemEntity);
			}
			applyAlternativeFields(entity, alternative);
			synced.add(entity);
		}
		itemEntity.getAlternatives().clear();
		itemEntity.getAlternatives().addAll(synced);
	}

	private static void applyAlternativeFields(
			WorkoutAdaptationProposalItemAlternativeJpaEntity entity,
			WorkoutAdaptationAlternative alternative) {
		entity.setRankPosition(alternative.rankPosition());
		entity.setRelationshipId(
				alternative.relationshipId() == null ? null : alternative.relationshipId().value());
		entity.setTargetExerciseDefinitionId(alternative.targetExerciseDefinitionId().value());
		entity.setTargetNameSnapshot(alternative.targetNameSnapshot());
		entity.setRelationshipTypeSnapshot(alternative.relationshipTypeSnapshot());
		entity.setCompatibilitySnapshot(alternative.compatibilitySnapshot());
		entity.setRationaleSnapshot(alternative.rationaleSnapshot());
		entity.setTargetDifficultySnapshot(alternative.targetDifficultySnapshot());
		entity.setTargetImpactLevelSnapshot(alternative.targetImpactLevelSnapshot());
		entity.setSelectedDefault(alternative.selectedDefault());
		entity.getRequiredEquipment().clear();
		entity.getRequiredEquipment().addAll(alternative.requiredEquipment());
	}

	private static List<WorkoutAdaptationProposalItemJpaEntity> toItemEntities(
			List<WorkoutAdaptationProposalItem> items,
			WorkoutAdaptationProposalJpaEntity proposalEntity) {
		List<WorkoutAdaptationProposalItemJpaEntity> entities = new ArrayList<>(items.size());
		for (WorkoutAdaptationProposalItem item : items) {
			WorkoutAdaptationProposalItemJpaEntity entity = new WorkoutAdaptationProposalItemJpaEntity();
			entity.setId(item.id().value());
			entity.setProposal(proposalEntity);
			applyItemFields(entity, item);
			entities.add(entity);
		}
		return entities;
	}

	private static WorkoutAdaptationProposalItem toItemDomain(WorkoutAdaptationProposalItemJpaEntity entity) {
		return WorkoutAdaptationProposalItem.rehydrate(
				WorkoutAdaptationProposalItemId.of(entity.getId()),
				WorkoutAdaptationProposalId.of(entity.getProposal().getId()),
				WorkoutExerciseExecutionId.of(entity.getWorkoutExerciseExecutionId()),
				WorkoutExerciseId.of(entity.getSourceWorkoutExerciseId()),
				entity.getExecutionOrder(),
				ExerciseDefinitionId.of(entity.getPrescribedExerciseDefinitionId()),
				entity.getPrescribedNameSnapshot(),
				ExerciseDefinitionId.of(entity.getCurrentPerformedExerciseDefinitionId()),
				entity.getCurrentPerformedNameSnapshot(),
				ExercisePerformanceKey.of(entity.getExercisePerformanceKeyAtGeneration()),
				entity.isCurrentFeasible(),
				entity.isPrescribedFeasible(),
				entity.isPerformedFeasible(),
				sortedEquipment(entity.getMissingRequiredEquipment()),
				entity.getAnalysisReasonCode(),
				entity.getAction(),
				entity.getGeneratedTargetExerciseDefinitionId() == null
						? null
						: ExerciseDefinitionId.of(entity.getGeneratedTargetExerciseDefinitionId()),
				entity.getGeneratedTargetNameSnapshot(),
				entity.getGeneratedRelationshipId() == null
						? null
						: ExerciseSubstitutionRelationshipId.of(entity.getGeneratedRelationshipId()),
				entity.getGeneratedRelationshipTypeSnapshot(),
				entity.getGeneratedCompatibilitySnapshot(),
				entity.getGeneratedRationaleSnapshot(),
				entity.getSelectedTargetExerciseDefinitionId() == null
						? null
						: ExerciseDefinitionId.of(entity.getSelectedTargetExerciseDefinitionId()),
				entity.getSelectedRelationshipId() == null
						? null
						: ExerciseSubstitutionRelationshipId.of(entity.getSelectedRelationshipId()),
				entity.getAthleteDecision(),
				entity.getAthleteNotes(),
				entity.getAlternatives().stream()
						.map(WorkoutAdaptationProposalPersistenceMapper::toAlternativeDomain)
						.toList(),
				entity.getCreatedAt(),
				entity.getUpdatedAt(),
				entity.getVersion());
	}

	private static WorkoutAdaptationAlternative toAlternativeDomain(
			WorkoutAdaptationProposalItemAlternativeJpaEntity entity) {
		return new WorkoutAdaptationAlternative(
				WorkoutAdaptationAlternativeId.of(entity.getId()),
				entity.getRankPosition(),
				entity.getRelationshipId() == null
						? null
						: ExerciseSubstitutionRelationshipId.of(entity.getRelationshipId()),
				ExerciseDefinitionId.of(entity.getTargetExerciseDefinitionId()),
				entity.getTargetNameSnapshot(),
				entity.getRelationshipTypeSnapshot(),
				entity.getCompatibilitySnapshot(),
				entity.getRationaleSnapshot(),
				entity.getTargetDifficultySnapshot(),
				entity.getTargetImpactLevelSnapshot(),
				sortedEquipment(entity.getRequiredEquipment()),
				entity.isSelectedDefault());
	}

	private static List<EquipmentType> sortedEquipment(Set<EquipmentType> equipment) {
		List<EquipmentType> ordered = new ArrayList<>(equipment);
		ordered.sort(Comparator.comparingInt(Enum::ordinal));
		return List.copyOf(ordered);
	}

}
