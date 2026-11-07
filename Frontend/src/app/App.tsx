import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { RequireAuth } from '@/features/auth/components/RequireAuth'
import { AuthProvider } from '@/features/auth/hooks/useAuth'
import { LoginPage } from '@/features/auth/pages/LoginPage'
import { WorklistPage } from '@/features/worklist/pages/WorklistPage'

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route
            path="/worklist"
            element={<RequireAuth><WorklistPage /></RequireAuth>}
          />
          <Route path="/" element={<Navigate to="/worklist" replace />} />
          <Route path="*" element={<Navigate to="/worklist" replace />} />
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  )
}
