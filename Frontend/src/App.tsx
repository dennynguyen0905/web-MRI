import { useState } from 'react'
import { LoginPage } from './pages/LoginPage'
import { WorklistPage } from './pages/WorklistPage'
import type { AppUser } from './lib/types'

export default function App() {
  const [user, setUser] = useState<AppUser | null>(null)

  if (!user) return <LoginPage onLogin={setUser} />
  return <WorklistPage user={user} onLogout={() => setUser(null)} />
}
