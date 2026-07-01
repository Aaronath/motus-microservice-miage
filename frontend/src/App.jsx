import { useState, useEffect, useCallback } from 'react'
import { api } from './api'
import { ToastStack, createToast } from './components/Toast'
import { LetterGrid, ColorLegend } from './components/LetterGrid'
import { GuessInput } from './components/GuessInput'
import { Confetti, WinOverlay, LoseOverlay } from './components/Confetti'
import { AdminPanel } from './components/AdminPanel'
import { friendlyError, STATUS, HISTORY_STATUS } from './utils/messages'
import {
  saveSession,
  saveUserProfile,
  getStoredToken,
  getStoredUser,
  clearSession,
  normalizeUser,
  saveActiveGameId,
  getStoredActiveGameId,
  clearActiveGame,
} from './utils/session'

function AttemptBar({ used, max }) {
  return (
    <div className="attempt-bar">
      {Array.from({ length: max }, (_, i) => (
        <div key={i} className={`attempt-pip ${i < used ? 'used' : i === used ? 'next' : ''}`} />
      ))}
      <span className="attempt-label">{max - used} essai{(max - used) !== 1 ? 's' : ''} restant{(max - used) !== 1 ? 's' : ''}</span>
    </div>
  )
}

function AuthScreen({ onAuth, toasts, addToast, clearToasts }) {
  const [tab, setTab] = useState('login')
  const [loading, setLoading] = useState(false)

  const submit = async (e) => {
    e.preventDefault()
    clearToasts()
    setLoading(true)
    const fd = new FormData(e.target)
    try {
      const res = tab === 'login'
        ? await api.login({ pseudo: fd.get('pseudo'), password: fd.get('password') })
        : await api.register({
            pseudo: fd.get('pseudo'),
            email: fd.get('email'),
            password: fd.get('password'),
            adminCode: fd.get('adminCode') || undefined,
          })
      saveSession(res)
      addToast(createToast(`Bienvenue ${res.pseudo} !`, 'success', '👋'))
      onAuth(normalizeUser(res))
    } catch (err) {
      addToast(createToast(friendlyError(err.message), 'error', '😅'))
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="auth-screen">
      <header className="hero">
        <div className="hero-badge">MIAGE SITN 2026</div>
        <h1 className="hero-title">
          <span className="hero-m">M</span>otus
        </h1>
        <p className="hero-tagline">Devine le mot mystère en 6 coups — la première lettre est offerte !</p>
      </header>

      <div className="auth-card glass">
        <div className="auth-tabs">
          <button type="button" className={tab === 'login' ? 'active' : ''} onClick={() => setTab('login')}>
            Connexion
          </button>
          <button type="button" className={tab === 'register' ? 'active' : ''} onClick={() => setTab('register')}>
            Inscription
          </button>
        </div>

        <form onSubmit={submit} className="auth-form">
          <label>
            Pseudo {tab === 'login' && <span className="label-hint">ou email</span>}
            <input name="pseudo" placeholder="Ton pseudo" required autoComplete="username" />
          </label>
          {tab === 'register' && (
            <label>
              Email
              <input name="email" type="email" placeholder="email@exemple.fr" required />
            </label>
          )}
          <label>
            Mot de passe
            <input name="password" type="password" placeholder="6 caractères minimum" required minLength={6} />
          </label>
          {tab === 'register' && (
            <label>
              Code admin <span className="label-hint">optionnel</span>
              <input name="adminCode" placeholder="MIAGE-ADMIN-2026" />
            </label>
          )}
          <button type="submit" className="btn btn-glow btn-block" disabled={loading}>
            {loading ? 'Patience…' : tab === 'login' ? 'Entrer dans le jeu' : 'Créer mon compte'}
          </button>
        </form>
      </div>

      <ColorLegend />
      <ToastStack toasts={toasts} onDismiss={(id) => clearToasts(id)} />
    </div>
  )
}

export default function App() {
  const [user, setUser] = useState(null)
  const [page, setPage] = useState('play')
  const [game, setGame] = useState(null)
  const [guess, setGuess] = useState('')
  const [toasts, setToasts] = useState([])
  const [shake, setShake] = useState(false)
  const [loading, setLoading] = useState(false)
  const [leaderboard, setLeaderboard] = useState([])
  const [globalStats, setGlobalStats] = useState(null)
  const [myStats, setMyStats] = useState(null)
  const [wordLength, setWordLength] = useState(7)
  const [sessionReady, setSessionReady] = useState(false)
  const [pendingGame, setPendingGame] = useState(null)
  const [ongoingGames, setOngoingGames] = useState([])
  const addToast = useCallback((t) => setToasts((prev) => [...prev, t]), [])
  const clearToasts = useCallback((id) => {
    setToasts((prev) => (id ? prev.filter((t) => t.id !== id) : []))
  }, [])

  const notifyError = useCallback((msg, ctx) => {
    addToast(createToast(friendlyError(msg, ctx), 'error', '🎯'))
    setShake(true)
    setTimeout(() => setShake(false), 500)
  }, [addToast])

  useEffect(() => {
    const token = getStoredToken()
    if (!token) {
      setSessionReady(true)
      return
    }
    const cached = getStoredUser()
    if (cached) setUser(cached)
    api.me()
      .then((profile) => {
        const u = saveUserProfile(profile)
        setUser(u)
      })
      .catch(() => {
        clearSession()
        setUser(null)
      })
      .finally(() => setSessionReady(true))
  }, [])

  const playerId = user?.playerId ?? user?.id

  const syncActiveGame = useCallback((g) => {
    if (!playerId || !g) return
    if (g.status === 'IN_PROGRESS') saveActiveGameId(playerId, g.id)
    else clearActiveGame()
  }, [playerId])

  const findPendingGame = useCallback(async () => {
    if (!playerId) return null
    try {
      const storedId = getStoredActiveGameId(playerId)
      if (storedId) {
        const g = await api.getGame(storedId)
        if (g.status === 'IN_PROGRESS') return g
        clearActiveGame()
      }
      const hist = await api.history()
      const ongoing = hist.find((h) => h.status === 'IN_PROGRESS')
      if (ongoing) return api.getGame(ongoing.id)
    } catch {
      /* ignore */
    }
    return null
  }, [playerId])

  useEffect(() => {
    if (!user || !sessionReady) return
    findPendingGame().then((g) => setPendingGame(g))
  }, [user, sessionReady, findPendingGame])

  useEffect(() => {
    if (!user || !sessionReady || game || !playerId) return
    const flag = `motus_auto_restore_${playerId}`
    if (sessionStorage.getItem(flag)) return
    const storedId = getStoredActiveGameId(playerId)
    if (!storedId) return
    sessionStorage.setItem(flag, '1')
    api.getGame(storedId)
      .then((g) => {
        if (g.status === 'IN_PROGRESS') {
          setGame(g)
          setGuess(g.firstLetter || '')
          setPage('play')
        } else {
          clearActiveGame()
          setPendingGame(null)
        }
      })
      .catch(() => clearActiveGame())
  }, [user, sessionReady, game, playerId])

  const resumeGame = async (gameId) => {
    setLoading(true)
    clearToasts()
    try {
      const g = await api.getGame(gameId)
      if (g.status !== 'IN_PROGRESS') {
        clearActiveGame()
        setPendingGame(null)
        notifyError('Cette partie est déjà terminée.')
        return
      }
      setGame(g)
      setGuess(g.firstLetter || '')
      syncActiveGame(g)
      setPage('play')
      addToast(createToast('Partie reprise — bonne chance !', 'info', '▶️'))
    } catch (err) {
      notifyError(err.message)
    } finally {
      setLoading(false)
    }
  }

  const goHome = () => {
    setPage('play')
    setGame(null)
    setGuess('')
    clearToasts()
  }

  const logout = () => {
    clearSession()
    setUser(null)
    setGame(null)
    setPage('play')
  }

  const abandonGame = async () => {
    if (!game || game.status !== 'IN_PROGRESS') return
    if (!confirm('Abandonner cette partie ? Le mot mystère sera révélé.')) return
    setLoading(true)
    try {
      const g = await api.abandonGame(game.id)
      setGame(g)
      syncActiveGame(g)
      setPendingGame(null)
      addToast(createToast(`Partie abandonnée — mot : ${g.secretWord}`, 'info', '🚪'))
    } catch (err) {
      notifyError(err.message)
    } finally {
      setLoading(false)
    }
  }

  const newGame = async () => {
    clearToasts()
    setLoading(true)
    try {
      const g = await api.createGame({ wordLength, maxAttempts: 6 })
      setGame(g)
      setGuess(g.firstLetter || '')
      syncActiveGame(g)
      setPendingGame(g)
      addToast(createToast('Nouvelle partie ! Bonne chance 🍀', 'info', '✨'))
    } catch (err) {
      notifyError(err.message)
    } finally {
      setLoading(false)
    }
  }

  const submitGuess = async (e) => {
    e.preventDefault()
    clearToasts()
    const word = guess.trim().toUpperCase()
    const ctx = { wordLength: game.wordLength, firstLetter: game.firstLetter }

    if (word.length !== game.wordLength) {
      notifyError(`Le mot doit contenir ${game.wordLength} lettres`, ctx)
      return
    }
    if (word.charAt(0) !== game.firstLetter) {
      notifyError(`La première lettre doit être '${game.firstLetter}'`, ctx)
      return
    }

    setLoading(true)
    try {
      await api.guess(game.id, word)
      const g = await api.getGame(game.id)
      setGame(g)
      setGuess(g.firstLetter || '')
      syncActiveGame(g)
      if (g.status === 'IN_PROGRESS') setPendingGame(g)
      else setPendingGame(null)

      if (g.status === 'WON') {
        addToast(createToast('Incroyable, tu as gagné ! 🎉', 'success', '🏆'))
      } else if (g.status === 'LOST') {
        addToast(createToast(`Le mot était ${g.secretWord}`, 'info', '💫'))
      }
    } catch (err) {
      notifyError(err.message, ctx)
    } finally {
      setLoading(false)
    }
  }

  const loadLeaderboard = async () => {
    try {
      const [board, global] = await Promise.all([api.leaderboard(), api.globalStats()])
      setLeaderboard(board)
      setGlobalStats(global)
    } catch (err) {
      notifyError(err.message)
    }
  }

  const loadHistory = async () => {
    if (!playerId) return
    try {
      const [stats, games] = await Promise.all([
        api.playerStats(playerId),
        api.history().catch(() => []),
      ])
      setMyStats(stats)
      const ongoing = games.filter((g) => g.status === 'IN_PROGRESS')
      setOngoingGames(ongoing)
      if (ongoing[0] && !game) setPendingGame(ongoing[0])
    } catch (err) {
      notifyError(err.message)
    }
  }

  if (!sessionReady) {
    return (
      <div className="loading-screen glass">
        <span className="brand-icon loading-logo">M</span>
        <p>Chargement de ta session…</p>
      </div>
    )
  }

  if (!user) {
    return (
      <AuthScreen
        onAuth={(u) => setUser(normalizeUser(u))}
        toasts={toasts}
        addToast={addToast}
        clearToasts={clearToasts}
      />
    )
  }

  const st = STATUS[game?.status] || STATUS.IN_PROGRESS

  return (
    <div className={`app-shell ${page === 'admin' ? 'wide' : ''}`}>
      <Confetti active={game?.status === 'WON'} />

      <header className="topbar">
        <button type="button" className="topbar-brand" onClick={goHome} title="Accueil">
          <span className="brand-icon">M</span>
          <span>Motus</span>
        </button>
        <div className="topbar-user">
          <span className="user-pill">{user.pseudo}</span>
          {user.role === 'ADMIN' && <span className="admin-badge">Admin</span>}
        </div>
      </header>

      <nav className="main-nav">
        {[
          ['play', '🎮', 'Jouer'],
          ['rank', '🏅', 'Classement'],
          ['hist', '📜', 'Historique'],
          ...(user.role === 'ADMIN' ? [['admin', '⚙️', 'Admin']] : []),
        ].map(([id, icon, label]) => (
          <button
            key={id}
            type="button"
            className={`nav-pill ${page === id ? 'active' : ''}`}
            onClick={() => {
              setPage(id)
              if (id === 'rank') loadLeaderboard()
              if (id === 'hist') loadHistory()
            }}
          >
            <span>{icon}</span> {label}
          </button>
        ))}
        <button type="button" className="nav-pill nav-logout" onClick={logout}>
          Déconnexion
        </button>
      </nav>

      <main className="main-content">
        {page === 'play' && (
          <section className="game-section">
            {!game ? (
              <div className="welcome-card glass pop-in">
                <h2>Prêt à jouer ?</h2>
                <p className="welcome-sub">Choisis la taille du mot · 6 essais · 1ère lettre révélée</p>
                <div className="length-picker" role="group" aria-label="Nombre de lettres">
                  {[5, 6, 7, 8, 9].map((n) => (
                    <button
                      key={n}
                      type="button"
                      className={`length-pill ${wordLength === n ? 'active' : ''}`}
                      onClick={() => setWordLength(n)}
                    >
                      {n}
                    </button>
                  ))}
                </div>
                <p className="length-hint">{wordLength} lettres</p>
                <ul className="rules-list">
                  <li><span className="dot well" /> Vert = bien placée</li>
                  <li><span className="dot mis" /> Orange = mal placée</li>
                  <li><span className="dot abs" /> Gris = absente</li>
                </ul>
                {pendingGame && (
                  <button
                    type="button"
                    className="btn btn-secondary btn-lg btn-block resume-btn"
                    onClick={() => resumeGame(pendingGame.id)}
                    disabled={loading}
                  >
                    ▶️ Continuer la partie #{pendingGame.id}
                    {pendingGame.wordLength ? ` (${pendingGame.wordLength} lettres)` : ''}
                  </button>
                )}
                <button type="button" className="btn btn-glow btn-lg" onClick={newGame} disabled={loading}>
                  {loading ? 'Chargement…' : pendingGame ? 'Nouvelle partie' : `Partie à ${wordLength} lettres`}
                </button>
              </div>
            ) : (
              <div className="game-panel glass">
                <div className="game-header">
                  <span className={`status-chip ${st.className}`}>{st.emoji} {st.label}</span>
                  <span className="game-meta">Partie #{game.id}</span>
                </div>

                <div className="first-letter-hint">
                  Commence par <strong>{game.firstLetter}</strong>
                  <span className="hint-blanks">{'_'.repeat(Math.max(0, game.wordLength - 1))}</span>
                </div>

                <AttemptBar used={game.attemptsUsed} max={game.maxAttempts} />

                <LetterGrid
                  guesses={game.guesses}
                  wordLength={game.wordLength}
                  maxAttempts={game.maxAttempts}
                  shake={shake}
                />

                {game.status === 'IN_PROGRESS' && (
                  <>
                    <form className="guess-form" onSubmit={submitGuess}>
                      <GuessInput
                        key={`${game.id}-${game.guesses?.length ?? 0}`}
                        firstLetter={game.firstLetter}
                        wordLength={game.wordLength}
                        value={guess}
                        onChange={setGuess}
                        disabled={loading}
                      />
                      <button type="submit" className="btn btn-glow" disabled={loading || guess.length !== game.wordLength}>
                        {loading ? '…' : 'Valider'}
                      </button>
                    </form>
                    <button
                      type="button"
                      className="btn btn-secondary btn-block abandon-btn"
                      onClick={abandonGame}
                      disabled={loading}
                    >
                      Abandonner la partie
                    </button>
                  </>
                )}

                <ColorLegend />

                {game.status !== 'IN_PROGRESS' && (
                  <button type="button" className="btn btn-secondary btn-block" onClick={goHome}>
                    Nouvelle partie
                  </button>
                )}
              </div>
            )}

            <WinOverlay
              show={game?.status === 'WON'}
              secretWord={game?.secretWord}
              attempts={game?.attemptsUsed}
              onPlayAgain={goHome}
            />
            <LoseOverlay
              show={game?.status === 'LOST'}
              secretWord={game?.secretWord}
              onPlayAgain={goHome}
            />
          </section>
        )}

        {page === 'rank' && (
          <section className="panel glass pop-in">
            <h2>🏅 Classement & statistiques</h2>
            {globalStats && (
              <div className="stats-banner">
                <span><strong>{globalStats.totalGames}</strong> parties jouées</span>
                <span><strong>{globalStats.wins}</strong> victoires</span>
                <span><strong>{globalStats.losses}</strong> défaites</span>
              </div>
            )}
            {leaderboard.length === 0 ? (
              <p className="empty-msg">Aucun score pour l'instant — sois le premier !</p>
            ) : (
              <div className="rank-list">
                {leaderboard.map((e, i) => (
                  <div key={e.playerId} className={`rank-row ${i < 3 ? `rank-${i + 1}` : ''}`}>
                    <span className="rank-pos">{i === 0 ? '🥇' : i === 1 ? '🥈' : i === 2 ? '🥉' : i + 1}</span>
                    <span className="rank-name">{e.pseudo}</span>
                    <span className="rank-stat">{e.wins} victoire{e.wins !== 1 ? 's' : ''}</span>
                    <span className="rank-score">{e.totalScore} pts</span>
                  </div>
                ))}
              </div>
            )}
          </section>
        )}

        {page === 'hist' && (
          <section className="panel glass pop-in">
            <h2>📜 Historique de tes résultats</h2>
            {!myStats && ongoingGames.length === 0 ? (
              <p className="empty-msg">Pas encore de résultat enregistré — termine une partie !</p>
            ) : (
              <>
                {myStats && (
                  <div className="stats-banner personal">
                    <span><strong>{myStats.totalGames}</strong> parties</span>
                    <span><strong>{myStats.wins}</strong> victoires</span>
                    <span><strong>{myStats.totalScore}</strong> points</span>
                  </div>
                )}
                {ongoingGames.length > 0 && (
                  <div className="hist-list ongoing-block">
                    <h3 className="hist-section-title">Parties en cours</h3>
                    {ongoingGames.map((g) => (
                      <article key={g.id} className="hist-card progress">
                        <header className="hist-card-head">
                          <span>🎯 En cours</span>
                        </header>
                        <p className="hist-card-title">Partie #{g.id} · {g.wordLength} lettres</p>
                        <p className="hist-meta">Essais : {g.attemptsUsed}/{g.maxAttempts}</p>
                        <button
                          type="button"
                          className="btn btn-glow btn-sm hist-resume"
                          onClick={() => resumeGame(g.id)}
                        >
                          ▶️ Reprendre la partie
                        </button>
                      </article>
                    ))}
                  </div>
                )}
                {myStats && myStats.history.length > 0 && (
                <div className="hist-list">
                  <h3 className="hist-section-title">Parties terminées</h3>
                  {myStats.history.map((h) => {
                    const st = HISTORY_STATUS[h.status] || HISTORY_STATUS.LOST
                    return (
                      <article key={h.gameId} className={`hist-card ${st.className}`}>
                        <header className="hist-card-head">
                          <span>{st.emoji} {st.label}</span>
                          <span className="hist-score">{h.score > 0 ? `+${h.score} pts` : '0 pt'}</span>
                        </header>
                        <p className="hist-card-title">Partie #{h.gameId}</p>
                        <dl className="hist-details">
                          <div><dt>Mot mystère</dt><dd><code>{h.secretWord || '—'}</code></dd></div>
                          <div><dt>Lettres</dt><dd>{h.wordLength} ({h.firstLetter}…)</dd></div>
                          <div><dt>Essais</dt><dd>{h.attempts} / {h.maxAttempts}</dd></div>
                          <div><dt>Date</dt><dd>
                            {h.finishedAt
                              ? new Date(h.finishedAt).toLocaleString('fr-FR', {
                                  dateStyle: 'medium',
                                  timeStyle: 'short',
                                })
                              : '—'}
                          </dd></div>
                        </dl>
                      </article>
                    )
                  })}
                </div>
                )}
                {myStats?.history.length === 0 && ongoingGames.length === 0 && (
                  <p className="empty-msg">Pas encore de résultat enregistré — termine une partie !</p>
                )}
              </>
            )}
          </section>
        )}

        {page === 'admin' && user.role === 'ADMIN' && (
          <AdminPanel
            onError={(msg) => notifyError(msg)}
            onSuccess={(msg) => addToast(createToast(msg, 'success', '✅'))}
          />
        )}
      </main>

      <ToastStack toasts={toasts} onDismiss={clearToasts} />
    </div>
  )
}
