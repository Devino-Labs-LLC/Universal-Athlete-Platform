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
import { DayForm } from '@/features/training/forms/DayForm';
import { CreateOccurrenceForm } from '@/features/training/forms/CreateOccurrenceForm';
import { ExercisePrescriptionForm } from '@/features/training/forms/ExercisePrescriptionForm';
import { useDayExercises, useExerciseMutations } from '@/features/training/hooks/useDayExercises';
import { useOccurrenceMutations } from '@/features/training/hooks/useOccurrences';
import { usePlan } from '@/features/training/hooks/usePlans';
import { useDayMutations, useWorkoutDays } from '@/features/training/hooks/useWorkoutDays';
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

  if (planQuery.isLoading || daysQuery.isLoading) {
    return <LoadingView message="Loading plan builder…" />;
  }

  if (planQuery.isError || !planQuery.data) {
    return <ErrorView message="Unable to load plan." onRetry={() => planQuery.refetch()} />;
  }

  const plan = planQuery.data;

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
      actions={
        <div className={styles.headerActions}>
          <Link to={`/app/training/plans/${planId}/edit`}>Edit metadata</Link>
          <Link to={`/app/training/plans/${planId}/schedule`}>Schedule</Link>
        </div>
      }
    >
      {errorMessage ? <p className="formError">{errorMessage}</p> : null}
      <div className={styles.layout}>
        <section className={styles.daysPanel} aria-label="Workout days">
          <div className={styles.panelHeader}>
            <h2>Workout days</h2>
            <Button type="button" onClick={() => setPanelMode({ kind: 'create-day' })}>
              Add day
            </Button>
          </div>
          <DayList
            days={days}
            selectedDayId={selectedDayId}
            onSelectDay={setSelectedDayId}
            onMoveUp={(dayId) => void handleReorderDays(dayId, 'up')}
            onMoveDown={(dayId) => void handleReorderDays(dayId, 'down')}
            onEditDay={(day) => setPanelMode({ kind: 'edit-day', day })}
            onDeleteDay={setDeleteDayTarget}
          />
        </section>

        <section className={styles.exercisesPanel} aria-label="Day exercises">
          {selectedDayId ? (
            <>
              <div className={styles.panelHeader}>
                <h2>Exercises</h2>
                <Button type="button" onClick={() => setChooserOpen(true)}>
                  Add exercise
                </Button>
              </div>
              {exercisesQuery.isLoading ? <LoadingView message="Loading exercises…" /> : null}
              {sortedExercises.length === 0 ? (
                <EmptyView title="No exercises" message="Add an exercise to this day." />
              ) : (
                <div className={styles.exerciseList}>
                  {sortedExercises.map((exercise) => (
                    <ExerciseRow
                      key={exercise.id}
                      exercise={exercise}
                      canMoveUp={canMoveUp(sortedExercises, exercise.id)}
                      canMoveDown={canMoveDown(sortedExercises, exercise.id)}
                      onMoveUp={() => void handleReorderExercise(exercise.id, 'up')}
                      onMoveDown={() => void handleReorderExercise(exercise.id, 'down')}
                      onEdit={() => setPanelMode({ kind: 'edit-exercise', exercise })}
                      onDelete={() => setDeleteExerciseTarget(exercise)}
                    />
                  ))}
                </div>
              )}
              {selectedDayId ? (
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

        {panelMode.kind !== 'none' ? (
          <aside className={styles.sideForm} aria-label="Editor panel">
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
                onCancel={() => setPanelMode({ kind: 'none' })}
                onSubmit={async (values) => {
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
                onCancel={() => setPanelMode({ kind: 'none' })}
                onSubmit={async (values) => {
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
        ) : null}
      </div>

      <ExerciseChooserModal
        open={chooserOpen}
        onClose={() => setChooserOpen(false)}
        onSelect={(definition) => {
          setChooserOpen(false);
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
          void dayMutations.remove.mutateAsync(deleteDayTarget.id).then(() => {
            if (selectedDayId === deleteDayTarget.id) {
              setSelectedDayId(null);
            }
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
          void exerciseMutations.remove.mutateAsync(deleteExerciseTarget.id).then(() => {
            setDeleteExerciseTarget(null);
          });
        }}
      />
    </Page>
  );
}
