import { Navigate, Route, Routes } from 'react-router-dom';

import { AppShell } from '@/app/layout/AppShell';
import {
  BootstrapGate,
  RedirectIfAuthenticated,
  RequireAuth,
  RequireBootstrapReady,
} from '@/app/router/guards';
import { LoginPage } from '@/features/auth/pages/LoginPage';
import { RegisterPage } from '@/features/auth/pages/RegisterPage';
import { VerifyEmailPage } from '@/features/auth/pages/VerifyEmailPage';
import { BootstrapPage } from '@/features/bootstrap/pages/BootstrapPage';
import { IncompatiblePage } from '@/features/bootstrap/pages/IncompatiblePage';
import { EnvironmentsPlaceholderPage } from '@/features/environments/pages/EnvironmentsPlaceholderPage';
import { HomeDiagnosticPage } from '@/features/home/pages/HomeDiagnosticPage';
import { PerformancePlaceholderPage } from '@/features/performance/pages/PerformancePlaceholderPage';
import { ProfilePlaceholderPage } from '@/features/profile/pages/ProfilePlaceholderPage';
import { RecoveryPlaceholderPage } from '@/features/recovery/pages/RecoveryPlaceholderPage';
import { TrainingPlaceholderPage } from '@/features/training/pages/TrainingPlaceholderPage';

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
          <Route path="/app" element={<AppShell />}>
            <Route index element={<Navigate to="home" replace />} />
            <Route path="home" element={<HomeDiagnosticPage />} />
            <Route path="training" element={<TrainingPlaceholderPage />} />
            <Route path="recovery" element={<RecoveryPlaceholderPage />} />
            <Route path="performance" element={<PerformancePlaceholderPage />} />
            <Route path="environments" element={<EnvironmentsPlaceholderPage />} />
            <Route path="profile" element={<ProfilePlaceholderPage />} />
          </Route>
        </Route>
      </Route>

      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
