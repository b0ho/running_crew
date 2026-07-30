import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import { AuthProvider } from './auth/AuthProvider';
import { AuthPage } from './auth/AuthPage';
import { RequireAuth } from './auth/RequireAuth';
import { AdminPage } from './admin/AdminPage';
import { CohortDetailPage } from './cohorts/CohortDetailPage';
import { CohortFormPage } from './cohorts/CohortFormPage';
import { ExplorePage } from './enrollment/ExplorePage';
import { MyApplicationsPage } from './enrollment/MyApplicationsPage';
import { DashboardPage } from './pages/DashboardPage';

/** 앱 루트 — 라우팅 + 인증 컨텍스트 (frontend-components §1). */
export function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          <Route path="/auth" element={<AuthPage />} />
          <Route
            path="/dashboard"
            element={
              <RequireAuth>
                <DashboardPage />
              </RequireAuth>
            }
          />
          <Route
            path="/explore"
            element={
              <RequireAuth>
                <ExplorePage />
              </RequireAuth>
            }
          />
          <Route
            path="/my/applications"
            element={
              <RequireAuth>
                <MyApplicationsPage />
              </RequireAuth>
            }
          />
          <Route
            path="/admin"
            element={
              <RequireAuth>
                <AdminPage />
              </RequireAuth>
            }
          />
          <Route
            path="/cohorts/new"
            element={
              <RequireAuth>
                <CohortFormPage />
              </RequireAuth>
            }
          />
          <Route
            path="/cohorts/:id"
            element={
              <RequireAuth>
                <CohortDetailPage />
              </RequireAuth>
            }
          />
          <Route
            path="/cohorts/:id/edit"
            element={
              <RequireAuth>
                <CohortFormPage />
              </RequireAuth>
            }
          />
          <Route path="/" element={<Navigate to="/dashboard" replace />} />
          <Route path="*" element={<Navigate to="/dashboard" replace />} />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  );
}
