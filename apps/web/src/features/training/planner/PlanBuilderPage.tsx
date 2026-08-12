import { useEffect, useMemo, useState } from 'react';
import { Link, useParams } from 'react-router-dom';

import { Button } from '@/core/components/Button';
import { EmptyView } from '@/core/components/EmptyView';
import { ErrorView } from '@/core/components/ErrorView';
import { LoadingView } from '@/core/components/LoadingView';
import { Page } from '@/core/components/Page';
import { ConfirmationDialog } from '@/features/profile/components/ConfirmationDialog';
import { DayList } from '@/features/training/components/DayList';
import { ExerciseRow } from '@/features/training/components/ExerciseRow';
import { TrainingStatusBadge } from '@/features/training/components/TrainingStatusBadge';
import { DayForm } from '@/features/training/forms/DayForm';
import { CreateOccurrenceForm } from '@/features/training/forms/CreateOccurrenceForm';
import { ExercisePrescriptionForm } from '@/features/training/forms/ExercisePrescriptionForm';
import { useDayExercises, useExerciseMutations } from '@/features/training/hooks/useDayExercises';
import { useOccurrenceMutations } from '@/features/training/hooks/useOccurrences';
import { usePlan } from '@/features/training/hooks/usePlans';
import { useDayMutations, useWorkoutDays } from '@/features/training/hooks/useWorkoutDays';
import { DAY_OF_WEEK_LABELS, planTypeLabel } from '@/features/training/models/labels';
import { trainingErrorMessage } from '@/features/training/models/trainingErrors';
import type {
  CreateWorkoutDayRequest,
  CreateWorkoutExerciseRequest,
  ExerciseDefinition,
  UpdateWorkoutDayRequest,
  UpdateWorkoutExerciseRequest,
  WorkoutDay,
  WorkoutExercise,
} from '@/features/training/models/schemas';
import { ExerciseChooserModal } from '@/features/training/planner/ExerciseChooserModal';
import {
  canMoveDown,
  canMoveUp,
  moveItemDown,
  moveItemUp,
  toOrderedIds,
} from '@/features/training/utils/reorderList';
import styles from '@/features/training/planner/PlanBuilderPage.module.scss';

type PanelMode =
  | { kind: 'none' }
  | { kind: 'create-day' }
  | { kind: 'edit-day'; day: WorkoutDay }
  | { kind: 'add-exercise'; definition: ExerciseDefinition }
  | { kind: 'edit-exercise'; exercise: WorkoutExercise };

export function PlanBuilderPage() {
  const { planId = '' } = useParams();
  const planQuery = usePlan(planId);
  const daysQuery = useWorkoutDays(planId);
  const dayMutations = useDayMutations(planId);

  const [selectedDayId, setSelectedDayId] = useState<string | null>(null);
  const [panelMode, setPanelMode] = useState<PanelMode>({ kind: 'none' });
  const [chooserOpen, setChooserOpen] = useState(false);
  const [deleteDayTarget, setDeleteDayTarget] = useState<WorkoutDay | null>(null);
  const [deleteExerciseTarget, setDeleteExerciseTarget] = useState<WorkoutExercise | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const days = useMemo(() => daysQuery.data ?? [], [daysQuery.data]);

  useEffect(() => {
    if (!selectedDayId && days.length > 0) {
      setSelectedDayId(days[0]!.id);
    }
  }, [days, selectedDayId]);

  const exercisesQuery = useDayExercises(planId, selectedDayId);
  const exerciseMutations = useExerciseMutations(planId, selectedDayId ?? '');
  const occurrenceMutations = useOccurrenceMutations(planId, selectedDayId ?? '');

  const sortedExercises = useMemo(
    () => [...(exercisesQuery.data ?? [])].sort((a, b) => a.displayOrder - b.displayOrder),
    [exercisesQuery.data],
  );

  const selectedDay = useMemo(
    () => days.find((day) => day.id === selectedDayId) ?? null,
    [days, selectedDayId],
  );

  if (planQuery.isLoading || daysQuery.isLoading) {
    return <LoadingView message="Loading plan builder…" />;
  }

  if (planQuery.isError || !planQuery.data) {
    return <ErrorView message="Unable to load plan." onRetry={() => planQuery.refetch()} />;
  }

  const plan = planQuery.data;
  const readOnly = plan.status === 'ARCHIVED';
  const showEditor = !readOnly && panelMode.kind !== 'none';

  const handleReorderDays = async (dayId: string, direction: 'up' | 'down') => {
    const reordered =
      direction === 'up' ? moveItemUp(days, dayId) : moveItemDown(days, dayId);
    try {
      await dayMutations.reorder.mutateAsync(toOrderedIds(reordered));
    } catch (error) {
      setErrorMessage(trainingErrorMessage(error));
    }
  };

  const handleReorderExercise = async (exerciseId: string, direction: 'up' | 'down') => {
    const reordered =
      direction === 'up'
        ? moveItemUp(sortedExercises, exerciseId)
        : moveItemDown(sortedExercises, exerciseId);
    try {
      await exerciseMutations.reorder.mutateAsync(toOrderedIds(reordered));
    } catch (error) {
      setErrorMessage(trainingErrorMessage(error));
    }
  };

  return (
    <Page
      title={plan.name}
      description="Build workout days and exercise prescriptions."
      width="wide"
      actions={
        <div className={styles.headerActions}>
          {!readOnly ? (
            <Link to={`/app/training/plans/${planId}/edit`}>
              <Button type="button" variant="ghost">
                Edit metadata
              </Button>
            </Link>
          ) : null}
          <Link to={`/app/training/plans/${planId}/schedule`}>
            <Button type="button" variant="secondary">
              Schedule
            </Button>
          </Link>
        </div>
      }
    >
      {errorMessage ? <p className="formError">{errorMessage}</p> : null}
      <div className={styles.statusRow}>
        <TrainingStatusBadge kind="plan" status={plan.status} />
        {plan.scheduleStatus ? (
          <TrainingStatusBadge kind="schedule" status={plan.scheduleStatus} />
        ) : null}
        <span className={styles.panelLabel}>{planTypeLabel(plan.type, plan.customTypeName)}</span>
      </div>
      {readOnly ? (
        <p className={styles.archivedNotice} role="status">
          This plan is archived and is available for reference only.
        </p>
      ) : null}
      <div className={styles.layout}>
        <section className={styles.daysPanel} aria-label="Workout days">
          <div className={styles.panelHeader}>
            <div>
              <p className={styles.panelLabel}>Plan context</p>
              <h2>Workout days</h2>
            </div>
            {!readOnly ? (
              <Button type="button" onClick={() => setPanelMode({ kind: 'create-day' })}>
                Add day
              </Button>
            ) : null}
          </div>
          <DayList
            days={days}
            selectedDayId={selectedDayId}
            onSelectDay={setSelectedDayId}
            onMoveUp={(dayId) => void handleReorderDays(dayId, 'up')}
            onMoveDown={(dayId) => void handleReorderDays(dayId, 'down')}
            onEditDay={(day) => setPanelMode({ kind: 'edit-day', day })}
            onDeleteDay={setDeleteDayTarget}
            readOnly={readOnly}
          />
        </section>

        <section className={styles.exercisesPanel} aria-label="Day exercises">
          {selectedDayId ? (
            <>
              <div className={styles.panelHeader}>
                <div>
                  <p className={styles.panelLabel}>Selected day</p>
                  <h2>{selectedDay?.title ?? 'Exercises'}</h2>
                </div>
                {!readOnly ? (
                  <Button type="button" onClick={() => setChooserOpen(true)}>
                    Add exercise
                  </Button>
                ) : null}
              </div>
              {exercisesQuery.isLoading ? <LoadingView message="Loading exercises…" /> : null}
              {sortedExercises.length === 0 ? (
                <EmptyView title="No exercises" message="Add an exercise to this day." />
              ) : (
                <div className={styles.exerciseList}>
                  {sortedExercises.map((exercise, index) => (
                    <ExerciseRow
                      key={exercise.id}
                      exercise={exercise}
                      order={index + 1}
                      canMoveUp={canMoveUp(sortedExercises, exercise.id)}
                      canMoveDown={canMoveDown(sortedExercises, exercise.id)}
                      onMoveUp={() => void handleReorderExercise(exercise.id, 'up')}
                      onMoveDown={() => void handleReorderExercise(exercise.id, 'down')}
                      onEdit={() => setPanelMode({ kind: 'edit-exercise', exercise })}
                      onDelete={() => setDeleteExerciseTarget(exercise)}
                      readOnly={readOnly}
                    />
                  ))}
                </div>
              )}
              {selectedDayId && !readOnly ? (
                <div className={styles.manualOccurrence}>
                  <h3>Manual occurrence</h3>
                  <CreateOccurrenceForm
                    onSubmit={async (values) => {
                      try {
                        await occurrenceMutations.create.mutateAsync(values);
                      } catch (error) {
                        setErrorMessage(trainingErrorMessage(error));
                      }
                    }}
                  />
                </div>
              ) : null}
            </>
          ) : (
            <EmptyView title="Select a day" message="Choose a workout day to manage exercises." />
          )}
        </section>

        {showEditor ? (
          <aside className={styles.sideForm} aria-label="Editor panel">
            <p className={styles.panelLabel}>Editor</p>
            {panelMode.kind === 'create-day' ? (
              <DayForm
                mode="create"
                onCancel={() => setPanelMode({ kind: 'none' })}
                onSubmit={async (values) => {
                  try {
                    const day = await dayMutations.create.mutateAsync(values as CreateWorkoutDayRequest);
                    setSelectedDayId(day.id);
                    setPanelMode({ kind: 'none' });
                  } catch (error) {
                    setErrorMessage(trainingErrorMessage(error));
                  }
                }}
              />
            ) : null}
            {panelMode.kind === 'edit-day' ? (
              <DayForm
                mode="edit"
                initialDay={panelMode.day}
                onCancel={() => setPanelMode({ kind: 'none' })}
                onSubmit={async (values) => {
                  try {
                    await dayMutations.update.mutateAsync({
                      dayId: panelMode.day.id,
                      request: values as UpdateWorkoutDayRequest,
                    });
                    setPanelMode({ kind: 'none' });
                  } catch (error) {
                    setErrorMessage(trainingErrorMessage(error));
                  }
                }}
              />
            ) : null}
            {panelMode.kind === 'add-exercise' ? (
              <ExercisePrescriptionForm
                mode="create"
                definition={panelMode.definition}
                serverError={errorMessage}
                onCancel={() => {
                  setErrorMessage(null);
                  setPanelMode({ kind: 'none' });
                }}
                onSubmit={async (values) => {
                  setErrorMessage(null);
                  try {
                    await exerciseMutations.create.mutateAsync({
                      ...(values as CreateWorkoutExerciseRequest),
                      exerciseDefinitionId: panelMode.definition.id,
                    });
                    setPanelMode({ kind: 'none' });
                  } catch (error) {
                    setErrorMessage(trainingErrorMessage(error));
                  }
                }}
              />
            ) : null}
            {panelMode.kind === 'edit-exercise' ? (
              <ExercisePrescriptionForm
                mode="edit"
                initialExercise={panelMode.exercise}
                serverError={errorMessage}
                onCancel={() => {
                  setErrorMessage(null);
                  setPanelMode({ kind: 'none' });
                }}
                onSubmit={async (values) => {
                  setErrorMessage(null);
                  try {
                    await exerciseMutations.update.mutateAsync({
                      exerciseId: panelMode.exercise.id,
                      request: values as UpdateWorkoutExerciseRequest,
                    });
                    setPanelMode({ kind: 'none' });
                  } catch (error) {
                    setErrorMessage(trainingErrorMessage(error));
                  }
                }}
              />
            ) : null}
          </aside>
        ) : (
          <aside className={styles.contextPanel} aria-label="Day and plan context">
            <p className={styles.panelLabel}>Context</p>
            <dl className={styles.contextList}>
              <div>
                <dt>Selected day</dt>
                <dd>{selectedDay?.title ?? '—'}</dd>
              </div>
              <div>
                <dt>Placement</dt>
                <dd>
                  {selectedDay
                    ? `Week ${selectedDay.planWeekNumber ?? 1}${
                        selectedDay.scheduledDayOfWeek
                          ? ` · ${DAY_OF_WEEK_LABELS[selectedDay.scheduledDayOfWeek]}`
                          : ''
                      }`
                    : '—'}
                </dd>
              </div>
              <div>
                <dt>Environment override</dt>
                <dd>{selectedDay?.trainingEnvironmentOverrideId ? 'Set' : 'None'}</dd>
              </div>
              <div>
                <dt>Exercises</dt>
                <dd>{sortedExercises.length}</dd>
              </div>
              <div>
                <dt>Schedule</dt>
                <dd>{plan.scheduleStatus ?? 'Not configured'}</dd>
              </div>
              <div>
                <dt>Generated through</dt>
                <dd>{plan.scheduleGeneratedThrough ?? '—'}</dd>
              </div>
            </dl>
          </aside>
        )}
      </div>

      <ExerciseChooserModal
        open={!readOnly && chooserOpen}
        onClose={() => setChooserOpen(false)}
        onSelect={(definition) => {
          setChooserOpen(false);
          setErrorMessage(null);
          setPanelMode({ kind: 'add-exercise', definition });
        }}
      />

      <ConfirmationDialog
        open={Boolean(deleteDayTarget)}
        title="Delete workout day?"
        message="This removes the day and its exercises from the plan."
        confirmLabel="Delete"
        onCancel={() => setDeleteDayTarget(null)}
        onConfirm={() => {
          if (!deleteDayTarget) {
            return;
          }
          void dayMutations.remove
            .mutateAsync(deleteDayTarget.id)
            .then(() => {
              if (selectedDayId === deleteDayTarget.id) {
                setSelectedDayId(null);
              }
              setDeleteDayTarget(null);
            })
            .catch((error: unknown) => {
              setErrorMessage(trainingErrorMessage(error));
              setDeleteDayTarget(null);
            });
        }}
      />

      <ConfirmationDialog
        open={Boolean(deleteExerciseTarget)}
        title="Delete exercise?"
        message="Prescription changes apply to future workout occurrences only."
        confirmLabel="Delete"
        onCancel={() => setDeleteExerciseTarget(null)}
        onConfirm={() => {
          if (!deleteExerciseTarget) {
            return;
          }
          void exerciseMutations.remove
            .mutateAsync(deleteExerciseTarget.id)
            .then(() => {
              setDeleteExerciseTarget(null);
            })
            .catch((error: unknown) => {
              setErrorMessage(trainingErrorMessage(error));
              setDeleteExerciseTarget(null);
            });
        }}
      />
    </Page>
  );
}
