import { getStoredToken } from './utils/session'

const API = import.meta.env.VITE_API_URL || ''

function headers() {
  const token = getStoredToken()
  const h = { 'Content-Type': 'application/json' }
  if (token) h.Authorization = `Bearer ${token}`
  return h
}

async function request(path, options = {}) {
  let res
  try {
    res = await fetch(`${API}${path}`, { ...options, headers: { ...headers(), ...options.headers } })
  } catch {
    throw new Error('Network error')
  }
  const text = await res.text()
  let data = null
  try {
    data = text ? JSON.parse(text) : null
  } catch {
    data = { message: text }
  }
  if (!res.ok) {
    const detail = data?.message || data?.detail || data?.title
    const generic = data?.error
    const msg = detail && detail !== generic ? detail : generic
    throw new Error(msg || res.statusText)
  }
  if (res.status === 204) return null
  return data
}

export const api = {
  register: (body) => request('/api/players/register', { method: 'POST', body: JSON.stringify(body) }),
  login: (body) => request('/api/players/login', { method: 'POST', body: JSON.stringify(body) }),
  me: () => request('/api/players/me'),
  createGame: (body) => request('/api/games', { method: 'POST', body: JSON.stringify(body || {}) }),
  getGame: (id) => request(`/api/games/${id}`),
  guess: (id, word) => request(`/api/games/${id}/guesses`, { method: 'POST', body: JSON.stringify({ word }) }),
  abandonGame: (id) => request(`/api/games/${id}/abandon`, { method: 'POST' }),
  history: () => request('/api/games/history/me'),
  leaderboard: () => request('/api/stats/leaderboard'),
  playerStats: (id) => request(`/api/stats/players/${id}`),
  globalStats: () => request('/api/stats/global'),
  adminSearchGames: (params = {}) => {
    const q = new URLSearchParams(params).toString()
    return request(`/api/games/admin/search?${q}`)
  },
  adminDeleteGame: (id) => request(`/api/games/admin/${id}`, { method: 'DELETE' }),
  adminListPlayers: () => request('/api/players/admin'),
  adminUpdatePlayerRole: (id, role) =>
    request(`/api/players/admin/${id}/role`, { method: 'PATCH', body: JSON.stringify({ role }) }),
  adminDeletePlayer: (id) => request(`/api/players/admin/${id}`, { method: 'DELETE' }),
  adminDictSearchWords: (q) =>
    request(`/api/dictionary/admin/words/search?${new URLSearchParams({ q })}`),
  adminDictGroups: () => request('/api/dictionary/admin/groups'),
  adminCreateGroup: (body) =>
    request('/api/dictionary/admin/groups', { method: 'POST', body: JSON.stringify(body) }),
  adminUpdateGroup: (id, body) =>
    request(`/api/dictionary/admin/groups/${encodeURIComponent(id)}`, {
      method: 'PUT',
      body: JSON.stringify(body),
    }),
  adminDeleteGroup: (id) =>
    request(`/api/dictionary/admin/groups/${encodeURIComponent(id)}`, { method: 'DELETE' }),
  adminDictWords: (groupId) =>
    request(`/api/dictionary/admin/groups/${encodeURIComponent(groupId)}/words`),
  adminAddWord: (groupId, body) =>
    request(`/api/dictionary/admin/groups/${encodeURIComponent(groupId)}/words`, {
      method: 'POST',
      body: JSON.stringify(body),
    }),
  adminGetWord: (wordId) => request(`/api/dictionary/admin/words/${wordId}`),
  adminListUngroupedWords: () => request('/api/dictionary/admin/words/ungrouped'),
  adminUpdateWord: (wordId, body) =>
    request(`/api/dictionary/admin/words/${wordId}`, { method: 'PUT', body: JSON.stringify(body) }),
  adminDeleteWord: (wordId) =>
    request(`/api/dictionary/admin/words/${wordId}`, { method: 'DELETE' }),
  adminDictReload: () => request('/api/dictionary/admin/reload', { method: 'POST' }),
}
