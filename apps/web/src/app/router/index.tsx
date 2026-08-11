import { Suspense, lazy, type ReactNode } from 'react';
import { Navigate, Route, Routes } from 'react-router-dom';

import { AppShell } from '@/app/layout/AppShell';
import {
  BootstrapGate,
  RedirectIfAuthenticated,
  RequireAuth,
  RequireBootstrapReady,
  RequireOnboardingComplete,
  RequireOnboardingIncomplete,
} from '@/app/router/guards';
import { LoadingView } from '@/core/components/LoadingView';
import { BootstrapPage } from '@/features/bootstrap/pages/BootstrapPage';
import { IncompatiblePage } from '@/features/bootstrap/pages/IncompatiblePage';
import { OnboardingLayout } from '@/features/onboarding/layout/OnboardingLayout';

const LoginPage = lazy(() =>
  import('@/features/auth/pages/LoginPage').then((m) => ({ default: m.LoginPage })),
);
const RegisterPage = lazy(() =>
  import('@/features/auth/pages/RegisterPage').then((m) => ({ default: m.RegisterPage })),
);
const VerifyEmailPage = lazy(() =>
  import('@/features/auth/pages/VerifyEmailPage').then((m) => ({ default: m.VerifyEmailPage })),
);
const HomePage = lazy(() =>
  import('@/features/home/pages/HomePage').then((m) => ({ default: m.HomePage })),
);
const OnboardingProfilePage = lazy(() =>
  import('@/features/onboarding/pages/OnboardingProfilePage').then((m) => ({ default: m.OnboardingProfilePage })),
);
const OnboardingSportsPage = lazy(() =>
  import('@/features/onboarding/pages/OnboardingSportsPage').then((m) => ({ default: m.OnboardingSportsPage })),
);
const OnboardingGoalsPage = lazy(() =>
  import('@/features/onboarding/pages/OnboardingGoalsPage').then((m) => ({ default: m.OnboardingGoalsPage })),
);
const ProfilePage = lazy(() =>
  import('@/features/profile/pages/ProfilePage').then((m) => ({ default: m.ProfilePage })),
);
const EditProfilePage = lazy(() =>
  import('@/features/profile/pages/EditProfilePage').then((m) => ({ default: m.EditProfilePage })),
);
const ManageSportsPage = lazy(() =>
  import('@/features/profile/pages/ManageSportsPage').then((m) => ({ default: m.ManageSportsPage })),
);
const ManageGoalsPage = lazy(() =>
  import('@/features/profile/pages/ManageGoalsPage').then((m) => ({ default: m.ManageGoalsPage })),
);
const TrainingLandingPage = lazy(() =>
  import('@/features/training/pages/TrainingLandingPage').then((m) => ({ default: m.TrainingLandingPage })),
);
const PlanListPage = lazy(() =>
  import('@/features/training/pages/PlanListPage').then((m) => ({ default: m.PlanListPage })),
);
const CreatePlanPage = lazy(() =>
  import('@/features/training/pages/CreatePlanPage').then((m) => ({ default: m.CreatePlanPage })),
);
const PlanBuilderPage = lazy(() =>
  import('@/features/training/planner/PlanBuilderPage').then((m) => ({ default: m.PlanBuilderPage })),
);
const PlanEditPage = lazy(() =>
  import('@/features/training/pages/PlanEditPage').then((m) => ({ default: m.PlanEditPage })),
);
const ScheduleManagementPage = lazy(() =>
  import('@/features/training/pages/ScheduleManagementPage').then((m) => ({ default: m.ScheduleManagementPage })),
);
const TrainingCalendarPage = lazy(() =>
  import('@/features/training/calendar/TrainingCalendarPage').then((m) => ({ default: m.TrainingCalendarPage })),
);
const OccurrenceDetailPage = lazy(() =>
  import('@/features/training/pages/OccurrenceDetailPage').then((m) => ({ default: m.OccurrenceDetailPage })),
);
const ExerciseCatalogPage = lazy(() =>
  import('@/features/exercises/pages/ExerciseCatalogPage').then((m) => ({ default: m.ExerciseCatalogPage })),
);
const CreateExercisePage = lazy(() =>
  import('@/features/exercises/pages/CreateExercisePage').then((m) => ({ default: m.CreateExercisePage })),
);
const ExerciseDetailPage = lazy(() =>
  import('@/features/exercises/pages/ExerciseDetailPage').then((m) => ({ default: m.ExerciseDetailPage })),
);
const EditExercisePage = lazy(() =>
  import('@/features/exercises/pages/EditExercisePage').then((m) => ({ default: m.EditExercisePage })),
);
const ExerciseSubstitutionsPage = lazy(() =>
  import('@/features/exercises/pages/ExerciseSubstitutionsPage').then((m) => ({ default: m.ExerciseSubstitutionsPage })),
);
const EnvironmentListPage = lazy(() =>
  import('@/features/environments/pages/EnvironmentListPage').then((m) => ({ default: m.EnvironmentListPage })),
);
const CreateEnvironmentPage = lazy(() =>
  import('@/features/environments/pages/CreateEnvironmentPage').then((m) => ({ default: m.CreateEnvironmentPage })),
);
const EnvironmentDetailPage = lazy(() =>
  import('@/features/environments/pages/EnvironmentDetailPage').then((m) => ({ default: m.EnvironmentDetailPage })),
);
const EditEnvironmentPage = lazy(() =>
  import('@/features/environments/pages/EditEnvironmentPage').then((m) => ({ default: m.EditEnvironmentPage })),
);

const RecoveryLandingPage = lazy(() =>
  import('@/features/recovery/pages/RecoveryLandingPage').then((m) => ({ default: m.RecoveryLandingPage })),
);
const RecoveryHistoryPage = lazy(() =>
  import('@/features/recovery/pages/RecoveryHistoryPage').then((m) => ({ default: m.RecoveryHistoryPage })),
);
const RecoveryAnalyticsPage = lazy(() =>
  import('@/features/recovery/pages/RecoveryAnalyticsPage').then((m) => ({ default: m.RecoveryAnalyticsPage })),
);
const ReadinessDetailPage = lazy(() =>
  import('@/features/recovery/pages/ReadinessDetailPage').then((m) => ({ default: m.ReadinessDetailPage })),
);
const GuidanceDetailPage = lazy(() =>
  import('@/features/recovery/pages/GuidanceDetailPage').then((m) => ({ default: m.GuidanceDetailPage })),
);
const AthleteStatePage = lazy(() =>
  import('@/features/recovery/pages/AthleteStatePage').then((m) => ({ default: m.AthleteStatePage })),
);
const AthleteStateComparePage = lazy(() =>
  import('@/features/recovery/pages/AthleteStateComparePage').then((m) => ({ default: m.AthleteStateComparePage })),
);

const PerformanceLandingPage = lazy(() =>
  import('@/features/performance/pages/PerformanceLandingPage').then((m) => ({ default: m.PerformanceLandingPage })),
);
const PersonalRecordsPage = lazy(() =>
  import('@/features/performance/pages/PersonalRecordsPage').then((m) => ({ default: m.PersonalRecordsPage })),
);
const ExercisePerformancePage = lazy(() =>
  import('@/features/performance/pages/ExercisePerformancePage').then((m) => ({ default: m.ExercisePerformancePage })),
);
const TrainingLoadPage = lazy(() =>
  import('@/features/performance/pages/TrainingLoadPage').then((m) => ({ default: m.TrainingLoadPage })),
);
const SessionPerformancePage = lazy(() =>
  import('@/features/performance/pages/SessionPerformancePage').then((m) => ({ default: m.SessionPerformancePage })),
);

function LazyPage({ children }: { children: ReactNode }) {
  return <Suspense fallback={<LoadingView message="Loading…" />}>{children}</Suspense>;
}

export function AppRouter() {
  return (
    <Suspense fallback={<LoadingView message="Loading…" />}>
      <Routes>
      <Route element={<BootstrapGate />}>
        <Route path="/" element={<BootstrapPage />} />
      </Route>

      <Route element={<RedirectIfAuthenticated />}>
        <Route path="/auth/login" element={<LoginPage />} />
        <Route path="/auth/register" element={<RegisterPage />} />
        <Route path="/auth/verify-email" element={<VerifyEmailPage />} />
      </Route>

      <Route path="/incompatible" element={<IncompatiblePage />} />

      <Route element={<RequireAuth />}>
        <Route element={<RequireBootstrapReady />}>
          <Route element={<RequireOnboardingIncomplete />}>
            <Route path="/onboarding" element={<OnboardingLayout />}>
              <Route index element={<Navigate to="profile" replace />} />
              <Route path="profile" element={<OnboardingProfilePage />} />
              <Route path="sports" element={<OnboardingSportsPage />} />
              <Route path="goals" element={<OnboardingGoalsPage />} />
            </Route>
          </Route>

          <Route element={<RequireOnboardingComplete />}>
            <Route path="/app" element={<AppShell />}>
              <Route index element={<Navigate to="home" replace />} />
              <Route path="home" element={<HomePage />} />
              <Route path="training" element={<TrainingLandingPage />} />
              <Route path="training/plans" element={<PlanListPage />} />
              <Route path="training/plans/new" element={<CreatePlanPage />} />
              <Route path="training/plans/:planId" element={<PlanBuilderPage />} />
              <Route path="training/plans/:planId/edit" element={<PlanEditPage />} />
              <Route path="training/plans/:planId/schedule" element={<ScheduleManagementPage />} />
              <Route path="training/calendar" element={<TrainingCalendarPage />} />
              <Route
                path="training/plans/:planId/days/:dayId/occurrences/:occurrenceId"
                element={<OccurrenceDetailPage />}
              />
              <Route
                path="recovery"
                element={
                  <LazyPage>
                    <RecoveryLandingPage />
                  </LazyPage>
                }
              />
              <Route
                path="recovery/history"
                element={
                  <LazyPage>
                    <RecoveryHistoryPage />
                  </LazyPage>
                }
              />
              <Route
                path="recovery/analytics"
                element={
                  <LazyPage>
                    <RecoveryAnalyticsPage />
                  </LazyPage>
                }
              />
              <Route
                path="recovery/readiness/:assessmentId"
                element={
                  <LazyPage>
                    <ReadinessDetailPage />
                  </LazyPage>
                }
              />
              <Route
                path="recovery/guidance/:recommendationId"
                element={
                  <LazyPage>
                    <GuidanceDetailPage />
                  </LazyPage>
                }
              />
              <Route
                path="recovery/state/:snapshotId"
                element={
                  <LazyPage>
                    <AthleteStatePage />
                  </LazyPage>
                }
              />
              <Route
                path="recovery/state/:snapshotId/compare"
                element={
                  <LazyPage>
                    <AthleteStateComparePage />
                  </LazyPage>
                }
              />
              <Route
                path="performance"
                element={
                  <LazyPage>
                    <PerformanceLandingPage />
                  </LazyPage>
                }
              />
              <Route
                path="performance/records"
                element={
                  <LazyPage>
                    <PersonalRecordsPage />
                  </LazyPage>
                }
              />
              <Route
                path="performance/exercises/:exercisePerformanceKey"
                element={
                  <LazyPage>
                    <ExercisePerformancePage />
                  </LazyPage>
                }
              />
              <Route
                path="performance/load"
                element={
                  <LazyPage>
                    <TrainingLoadPage />
                  </LazyPage>
                }
              />
              <Route
                path="performance/sessions/:planId/:dayId/:occurrenceId"
                element={
                  <LazyPage>
                    <SessionPerformancePage />
                  </LazyPage>
                }
              />
              <Route path="exercises" element={<ExerciseCatalogPage />} />
              <Route path="exercises/new" element={<CreateExercisePage />} />
              <Route path="exercises/:definitionId" element={<ExerciseDetailPage />} />
              <Route path="exercises/:definitionId/edit" element={<EditExercisePage />} />
              <Route
                path="exercises/:definitionId/substitutions"
                element={<ExerciseSubstitutionsPage />}
              />
              <Route path="environments" element={<EnvironmentListPage />} />
              <Route path="environments/new" element={<CreateEnvironmentPage />} />
              <Route path="environments/:environmentId" element={<EnvironmentDetailPage />} />
              <Route path="environments/:environmentId/edit" element={<EditEnvironmentPage />} />
              <Route path="profile" element={<ProfilePage />} />
              <Route path="profile/edit" element={<EditProfilePage />} />
              <Route path="profile/sports" element={<ManageSportsPage />} />
              <Route path="profile/goals" element={<ManageGoalsPage />} />
            </Route>
          </Route>
        </Route>
      </Route>

      <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </Suspense>
  );
}
