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
import { LoginPage } from '@/features/auth/pages/LoginPage';
import { RegisterPage } from '@/features/auth/pages/RegisterPage';
import { VerifyEmailPage } from '@/features/auth/pages/VerifyEmailPage';
import { BootstrapPage } from '@/features/bootstrap/pages/BootstrapPage';
import { IncompatiblePage } from '@/features/bootstrap/pages/IncompatiblePage';
import { EnvironmentsPlaceholderPage } from '@/features/environments/pages/EnvironmentsPlaceholderPage';
import { HomePage } from '@/features/home/pages/HomePage';
import { OnboardingLayout } from '@/features/onboarding/layout/OnboardingLayout';
import { OnboardingGoalsPage } from '@/features/onboarding/pages/OnboardingGoalsPage';
import { OnboardingProfilePage } from '@/features/onboarding/pages/OnboardingProfilePage';
import { OnboardingSportsPage } from '@/features/onboarding/pages/OnboardingSportsPage';
import { PerformancePlaceholderPage } from '@/features/performance/pages/PerformancePlaceholderPage';
import { EditProfilePage } from '@/features/profile/pages/EditProfilePage';
import { ManageGoalsPage } from '@/features/profile/pages/ManageGoalsPage';
import { ManageSportsPage } from '@/features/profile/pages/ManageSportsPage';
import { ProfilePage } from '@/features/profile/pages/ProfilePage';
import { RecoveryPlaceholderPage } from '@/features/recovery/pages/RecoveryPlaceholderPage';
import { TrainingCalendarPage } from '@/features/training/calendar/TrainingCalendarPage';
import { CreatePlanPage } from '@/features/training/pages/CreatePlanPage';
import { OccurrenceDetailPage } from '@/features/training/pages/OccurrenceDetailPage';
import { PlanEditPage } from '@/features/training/pages/PlanEditPage';
import { PlanListPage } from '@/features/training/pages/PlanListPage';
import { ScheduleManagementPage } from '@/features/training/pages/ScheduleManagementPage';
import { TrainingLandingPage } from '@/features/training/pages/TrainingLandingPage';
import { PlanBuilderPage } from '@/features/training/planner/PlanBuilderPage';

export function AppRouter() {
  return (
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
              <Route path="recovery" element={<RecoveryPlaceholderPage />} />
              <Route path="performance" element={<PerformancePlaceholderPage />} />
              <Route path="environments" element={<EnvironmentsPlaceholderPage />} />
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
  );
}
