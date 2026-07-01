const TOKEN_KEY = 'motus_token'
const USER_KEY = 'motus_user'
const ACTIVE_GAME_KEY = 'motus_active_game'

export function saveSession(auth) {
  if (auth?.token) {
    localStorage.setItem(TOKEN_KEY, auth.token)
    localStorage.setItem('token', auth.token)
  }
  const user = {
    playerId: auth.playerId,
    pseudo: auth.pseudo,
    role: auth.role,
  }
  localStorage.setItem(USER_KEY, JSON.stringify(user))
}

export function saveUserProfile(profile) {
  const user = normalizeUser(profile)
  localStorage.setItem(USER_KEY, JSON.stringify(user))
  return user
}

export function normalizeUser(u) {
  if (!u) return null
  return {
    playerId: u.playerId ?? u.id,
    id: u.playerId ?? u.id,
    pseudo: u.pseudo,
    role: u.role,
    email: u.email,
  }
}

export function getStoredToken() {
  return localStorage.getItem(TOKEN_KEY) || localStorage.getItem('token')
}

export function getStoredUser() {
  try {
    const raw = localStorage.getItem(USER_KEY)
    return raw ? normalizeUser(JSON.parse(raw)) : null
  } catch {
    return null
  }
}

export function saveActiveGameId(playerId, gameId) {
  if (!playerId || !gameId) return
  localStorage.setItem(ACTIVE_GAME_KEY, JSON.stringify({ playerId: String(playerId), gameId }))
}

export function getStoredActiveGameId(playerId) {
  try {
    const raw = localStorage.getItem(ACTIVE_GAME_KEY)
    if (!raw) return null
    const { playerId: storedPlayer, gameId } = JSON.parse(raw)
    if (String(storedPlayer) !== String(playerId)) return null
    return gameId
  } catch {
    return null
  }
}

export function clearActiveGame() {
  localStorage.removeItem(ACTIVE_GAME_KEY)
}

export function clearSession() {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem('token')
  localStorage.removeItem(USER_KEY)
  clearActiveGame()
}
