import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider, useAuth } from './context/AuthContext';
import { Layout } from './components/layout/Layout';

// Pages
import { Login } from './pages/Login';
import { Dashboard } from './pages/Dashboard';
import { Transactions } from './pages/Transactions';
import { TransactionDetails } from './pages/TransactionDetails';
import { LedgerAccounts } from './pages/LedgerAccounts';
import { JournalEntries } from './pages/JournalEntries';
import { TrialBalance } from './pages/TrialBalance';
import { ReconciliationRuns } from './pages/ReconciliationRuns';
import { NewReconciliation } from './pages/NewReconciliation';
import { ReconciliationDetails } from './pages/ReconciliationDetails';
import { Exceptions } from './pages/Exceptions';
import { ExceptionInvestigation } from './pages/ExceptionInvestigation';
import { AiInvestigationHistory } from './pages/AiInvestigationHistory';
import { ApprovalCenter } from './pages/ApprovalCenter';
import { AuditLogs } from './pages/AuditLogs';
import { UserManagement } from './pages/UserManagement';
import { SystemHealth } from './pages/SystemHealth';
import { Settings } from './pages/Settings';

const ProtectedRoute: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const { isAuthenticated } = useAuth();
  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }
  return <>{children}</>;
};

export const App: React.FC = () => {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          <Route path="/login" element={<Login />} />

          <Route
            path="/"
            element={
              <ProtectedRoute>
                <Layout />
              </ProtectedRoute>
            }
          >
            <Route index element={<Dashboard />} />
            <Route path="transactions" element={<Transactions />} />
            <Route path="transactions/:id" element={<TransactionDetails />} />
            <Route path="accounts" element={<LedgerAccounts />} />
            <Route path="journals" element={<JournalEntries />} />
            <Route path="trial-balance" element={<TrialBalance />} />
            <Route path="reconciliations" element={<ReconciliationRuns />} />
            <Route path="reconciliations/new" element={<NewReconciliation />} />
            <Route path="reconciliations/:id" element={<ReconciliationDetails />} />
            <Route path="exceptions" element={<Exceptions />} />
            <Route path="exceptions/:id" element={<ExceptionInvestigation />} />
            <Route path="investigations" element={<AiInvestigationHistory />} />
            <Route path="approvals" element={<ApprovalCenter />} />
            <Route path="audit-logs" element={<AuditLogs />} />
            <Route path="users" element={<UserManagement />} />
            <Route path="health" element={<SystemHealth />} />
            <Route path="settings" element={<Settings />} />
          </Route>

          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  );
};

export default App;
