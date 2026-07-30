package com.devinolabs.uap.training.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import com.devinolabs.uap.TestcontainersConfiguration;
import com.devinolabs.uap.athlete.application.CreateAthleteProfileUseCase;
import com.devinolabs.uap.athlete.domain.DominantFoot;
import com.devinolabs.uap.athlete.domain.DominantHand;
import com.devinolabs.uap.athlete.domain.Height;
import com.devinolabs.uap.athlete.domain.Sex;
import com.devinolabs.uap.athlete.domain.Weight;
import com.devinolabs.uap.training.domain.AccountId;
import com.devinolabs.uap.training.domain.TrainingPlanStatusAction;
import com.devinolabs.uap.training.domain.TrainingPlanType;
import com.devinolabs.uap.training.domain.WorkoutDayStatus;
import com.devinolabs.uap.training.domain.WorkoutDayStatusAction;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class WorkoutDayUseCaseIntegrationTests {

	@Autowired
	private CreateAthleteProfileUseCase createAthleteProfileUseCase;

	@Autowired
	private CreateTrainingPlanUseCase createTrainingPlanUseCase;

	@Autowired
	private ChangeTrainingPlanStatusUseCase changeTrainingPlanStatusUseCase;

	@Autowired
	private CreateWorkoutDayUseCase createWorkoutDayUseCase;

	@Autowired
	private ListWorkoutDaysUseCase listWorkoutDaysUseCase;

	@Autowired
	private GetWorkoutDayUseCase getWorkoutDayUseCase;

	@Autowired
	private UpdateWorkoutDayUseCase updateWorkoutDayUseCase;

	@Autowired
	private ReorderWorkoutDaysUseCase reorderWorkoutDaysUseCase;

	@Autowired
	private ChangeWorkoutDayStatusUseCase changeWorkoutDayStatusUseCase;

	@Autowired
	private DeleteWorkoutDayUseCase deleteWorkoutDayUseCase;

	@Test
	void createsListsOrdersUpdatesDeletesAndLifecycle() {
		AccountId accountId = AccountId.generate();
		createAthlete(accountId);
		TrainingPlanResult plan = createTrainingPlanUseCase.execute(
				accountId, TrainingPlanType.STRENGTH, null, "Strength", null,
				LocalDate.of(2026, 6, 1), LocalDate.of(2026, 8, 31), null, null);

		WorkoutDayResult monday = createWorkoutDayUseCase.execute(
				accountId, plan.id(), "  Lower   Body  ", "Squats", 1, DayOfWeek.MONDAY,
				LocalTime.of(9, 0), 60, null);
		assertThat(monday.displayOrder()).isZero();
		assertThat(monday.title()).isEqualTo("Lower   Body");
		assertThat(monday.status()).isEqualTo(WorkoutDayStatus.PLANNED);

		WorkoutDayResult wednesday = createWorkoutDayUseCase.execute(
				accountId, plan.id(), "Upper Body", null, 1, DayOfWeek.WEDNESDAY, null, null, null);
		assertThat(wednesday.displayOrder()).isEqualTo(1);

		WorkoutDayResult inserted = createWorkoutDayUseCase.execute(
				accountId, plan.id(), "Conditioning", null, 1, DayOfWeek.TUESDAY, null, 45, 1);
		assertThat(inserted.displayOrder()).isEqualTo(1);

		List<WorkoutDayResult> listed = listWorkoutDaysUseCase.execute(accountId, plan.id());
		assertThat(listed).extracting(WorkoutDayResult::title)
				.containsExactly("Lower   Body", "Conditioning", "Upper Body");
		assertThat(listed).extracting(WorkoutDayResult::displayOrder).containsExactly(0, 1, 2);

		assertThatThrownBy(() -> createWorkoutDayUseCase.execute(
				accountId, plan.id(), "lower body", null, 1, DayOfWeek.FRIDAY, null, null, null))
				.isInstanceOf(DuplicateWorkoutDayException.class);

		List<WorkoutDayResult> reordered = reorderWorkoutDaysUseCase.execute(
				accountId,
				plan.id(),
				List.of(wednesday.id().value(), monday.id().value(), inserted.id().value()));
		assertThat(reordered).extracting(r -> r.id().value())
				.containsExactly(wednesday.id().value(), monday.id().value(), inserted.id().value());
		assertThat(reordered).extracting(WorkoutDayResult::displayOrder).containsExactly(0, 1, 2);

		WorkoutDayResult updated = updateWorkoutDayUseCase.execute(
				accountId,
				plan.id(),
				monday.id(),
				new UpdateWorkoutDayCommand(
						"Lower Body Power", true,
						null, true,
						2, true,
						DayOfWeek.THURSDAY, true,
						null, true,
						75, true,
						0, true,
						null, false));
		assertThat(updated.title()).isEqualTo("Lower Body Power");
		assertThat(updated.description()).isNull();
		assertThat(updated.planWeekNumber()).isEqualTo(2);
		assertThat(updated.scheduledDayOfWeek()).isEqualTo(DayOfWeek.THURSDAY);
		assertThat(updated.plannedStartTime()).isNull();
		assertThat(updated.expectedDurationMinutes()).isEqualTo(75);

		changeWorkoutDayStatusUseCase.execute(
				accountId, plan.id(), wednesday.id(), WorkoutDayStatusAction.ACTIVATE);
		assertThatThrownBy(() -> deleteWorkoutDayUseCase.execute(accountId, plan.id(), wednesday.id()))
				.isInstanceOf(WorkoutDayDeleteNotAllowedException.class);

		changeWorkoutDayStatusUseCase.execute(
				accountId, plan.id(), wednesday.id(), WorkoutDayStatusAction.COMPLETE);
		assertThat(getWorkoutDayUseCase.execute(accountId, plan.id(), wednesday.id()).status())
				.isEqualTo(WorkoutDayStatus.COMPLETED);

		deleteWorkoutDayUseCase.execute(accountId, plan.id(), inserted.id());
		List<WorkoutDayResult> afterDelete = listWorkoutDaysUseCase.execute(accountId, plan.id());
		assertThat(afterDelete).extracting(WorkoutDayResult::displayOrder).containsExactly(0, 1);
	}

	@Test
	void rejectsArchivedPlanInvalidStatusAndCrossAccount() {
		AccountId owner = AccountId.generate();
		AccountId other = AccountId.generate();
		createAthlete(owner);
		createAthlete(other);

		TrainingPlanResult plan = createTrainingPlanUseCase.execute(
				owner, TrainingPlanType.GENERAL, null, "General", null,
				LocalDate.of(2026, 1, 1), LocalDate.of(2026, 2, 1), null, null);
		WorkoutDayResult day = createWorkoutDayUseCase.execute(
				owner, plan.id(), "Skills", null, 1, DayOfWeek.SATURDAY, null, null, null);

		assertThatThrownBy(() -> changeWorkoutDayStatusUseCase.execute(
				owner, plan.id(), day.id(), WorkoutDayStatusAction.COMPLETE))
				.isInstanceOf(InvalidWorkoutDayStatusException.class);

		assertThatThrownBy(() -> listWorkoutDaysUseCase.execute(other, plan.id()))
				.isInstanceOf(TrainingPlanNotFoundException.class);
		assertThatThrownBy(() -> getWorkoutDayUseCase.execute(other, plan.id(), day.id()))
				.isInstanceOf(TrainingPlanNotFoundException.class);

		changeTrainingPlanStatusUseCase.execute(owner, plan.id(), TrainingPlanStatusAction.ARCHIVE);
		assertThatThrownBy(() -> createWorkoutDayUseCase.execute(
				owner, plan.id(), "New Day", null, 1, DayOfWeek.SUNDAY, null, null, null))
				.isInstanceOf(TrainingPlanArchivedException.class);

		assertThatThrownBy(() -> reorderWorkoutDaysUseCase.execute(
				owner, plan.id(), List.of(day.id().value(), UUID.randomUUID())))
				.isInstanceOf(TrainingPlanArchivedException.class);
	}

	private void createAthlete(AccountId accountId) {
		createAthleteProfileUseCase.execute(
				com.devinolabs.uap.athlete.domain.AccountId.of(accountId.value()),
				"Jordan",
				"Lee",
				LocalDate.of(1998, 5, 12),
				Sex.FEMALE,
				Height.ofCentimeters(175),
				Weight.ofKilograms(68),
				DominantHand.RIGHT,
				DominantFoot.RIGHT);
	}

}
