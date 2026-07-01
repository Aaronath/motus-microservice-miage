import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { api } from '../api'

const UNGROUPED = '__ungrouped__'

function matchesText(value, query) {
  if (!query) return true
  return String(value ?? '').toLowerCase().includes(query)
}

export function AdminPanel({ onError, onSuccess }) {
  const [tab, setTab] = useState('games')
  const [games, setGames] = useState([])
  const [players, setPlayers] = useState([])
  const [groups, setGroups] = useState([])
  const [selectedGroup, setSelectedGroup] = useState(null)
  const [words, setWords] = useState([])
  const [globalWordHits, setGlobalWordHits] = useState([])
  const [loading, setLoading] = useState(false)
  const [gameSearch, setGameSearch] = useState({ q: '', status: '' })
  const [playerQuery, setPlayerQuery] = useState('')
  const [groupQuery, setGroupQuery] = useState('')
  const [wordQuery, setWordQuery] = useState('')

  const [newGroup, setNewGroup] = useState({ name: '', length: 7, firstLetter: 'A' })
  const [newWord, setNewWord] = useState({ word: '', secretWord: false })
  const [editWord, setEditWord] = useState(null)
  const editPanelRef = useRef(null)

  const notify = (msg, type = 'success') => {
    if (type === 'success' && onSuccess) onSuccess(msg)
    else if (onError) onError(msg)
  }

  const loadGames = useCallback(async (filters = gameSearch) => {
    const params = {}
    if (filters.q?.trim()) params.q = filters.q.trim()
    if (filters.status) params.status = filters.status
    setGames(await api.adminSearchGames(params))
  }, [gameSearch])

  const loadPlayers = useCallback(async () => {
    setPlayers(await api.adminListPlayers())
  }, [])

  const loadGroups = useCallback(async () => {
    const g = await api.adminDictGroups()
    setGroups(g)
    if (g.length > 0 && !selectedGroup) {
      setSelectedGroup(g[0].id)
    }
  }, [selectedGroup])

  const loadWords = useCallback(async (groupId) => {
    if (!groupId) {
      setWords([])
      return
    }
    if (groupId === UNGROUPED) {
      setWords(await api.adminListUngroupedWords())
      return
    }
    setWords(await api.adminDictWords(groupId))
  }, [])

  const startEditWord = useCallback(async (w) => {
    try {
      const fresh = await api.adminGetWord(w.id)
      setEditWord({
        id: fresh.id,
        word: fresh.word,
        secretWord: !!fresh.secretWord,
        groupCode: fresh.groupCode || UNGROUPED,
      })
      requestAnimationFrame(() => {
        editPanelRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' })
      })
    } catch (err) {
      notify(err.message, 'error')
    }
  }, [])

  const refresh = useCallback(async () => {
    setLoading(true)
    try {
      if (tab === 'games') await loadGames()
      if (tab === 'players') await loadPlayers()
      if (tab === 'dict') {
        await loadGroups()
        if (selectedGroup) await loadWords(selectedGroup)
      }
    } catch (err) {
      notify(err.message, 'error')
    } finally {
      setLoading(false)
    }
  }, [tab, selectedGroup, loadGames, loadPlayers, loadGroups, loadWords])

  useEffect(() => {
    refresh()
  }, [tab])

  useEffect(() => {
    if (tab === 'dict' && selectedGroup) {
      loadWords(selectedGroup).catch((e) => notify(e.message, 'error'))
    }
  }, [selectedGroup, tab])

  const playerFilter = playerQuery.trim().toLowerCase()
  const filteredPlayers = useMemo(() => {
    if (!playerFilter) return players
    return players.filter(
      (p) =>
        matchesText(p.pseudo, playerFilter) ||
        matchesText(p.email, playerFilter) ||
        matchesText(p.id, playerFilter),
    )
  }, [players, playerFilter])

  const groupFilter = groupQuery.trim().toLowerCase()
  const filteredGroups = useMemo(() => {
    if (!groupFilter) return groups
    return groups.filter(
      (g) =>
        matchesText(g.name, groupFilter) ||
        matchesText(g.firstLetter, groupFilter) ||
        matchesText(g.length, groupFilter) ||
        matchesText(g.id, groupFilter),
    )
  }, [groups, groupFilter])

  const wordFilter = wordQuery.trim().toLowerCase()
  const filteredWords = useMemo(() => {
    if (!wordFilter) return words
    return words.filter((w) => matchesText(w.word, wordFilter))
  }, [words, wordFilter])

  useEffect(() => {
    if (tab !== 'dict' || wordFilter.length < 2) {
      setGlobalWordHits([])
      return undefined
    }
    const timer = setTimeout(() => {
      api
        .adminDictSearchWords(wordFilter)
        .then(setGlobalWordHits)
        .catch((e) => notify(e.message, 'error'))
    }, 350)
    return () => clearTimeout(timer)
  }, [wordFilter, tab])

  const deleteGame = async (id) => {
    if (!confirm(`Supprimer la partie #${id} ?`)) return
    try {
      await api.adminDeleteGame(id)
      notify('Partie supprimée')
      await loadGames()
    } catch (err) {
      notify(err.message, 'error')
    }
  }

  const toggleRole = async (p) => {
    const next = p.role === 'ADMIN' ? 'JOUEUR' : 'ADMIN'
    try {
      await api.adminUpdatePlayerRole(p.id, next)
      notify(`Rôle de ${p.pseudo} → ${next}`)
      await loadPlayers()
    } catch (err) {
      notify(err.message, 'error')
    }
  }

  const deletePlayer = async (p) => {
    if (!confirm(`Supprimer le joueur ${p.pseudo} ?`)) return
    try {
      await api.adminDeletePlayer(p.id)
      notify('Joueur supprimé')
      await loadPlayers()
    } catch (err) {
      notify(err.message, 'error')
    }
  }

  const createGroup = async (e) => {
    e.preventDefault()
    try {
      const created = await api.adminCreateGroup(newGroup)
      notify(`Groupe créé : ${created.name}`)
      setNewGroup({ name: '', length: 7, firstLetter: 'A' })
      setSelectedGroup(created.id)
      await loadGroups()
      await loadWords(created.id)
    } catch (err) {
      notify(err.message, 'error')
    }
  }

  const deleteGroup = async (id, name) => {
    if (!confirm(`Supprimer le groupe « ${name} » et tous ses mots ?`)) return
    try {
      await api.adminDeleteGroup(id)
      notify('Groupe supprimé')
      if (selectedGroup === id) setSelectedGroup(null)
      await loadGroups()
      setWords([])
    } catch (err) {
      notify(err.message, 'error')
    }
  }

  const addWord = async (e) => {
    e.preventDefault()
    if (!selectedGroup) return
    try {
      await api.adminAddWord(selectedGroup, newWord)
      notify('Mot ajouté')
      setNewWord({ word: '', secretWord: false })
      await loadWords(selectedGroup)
      await loadGroups()
    } catch (err) {
      notify(err.message, 'error')
    }
  }

  const saveWord = async (e) => {
    e.preventDefault()
    if (!editWord) return
    try {
      const updated = await api.adminUpdateWord(editWord.id, {
        word: editWord.word,
        secretWord: editWord.secretWord,
        groupCode: editWord.groupCode || UNGROUPED,
      })
      notify('Mot mis à jour')
      const nextGroup = updated.groupCode || UNGROUPED
      setEditWord(null)
      setSelectedGroup(nextGroup)
      await loadGroups()
      await loadWords(nextGroup)
    } catch (err) {
      notify(err.message, 'error')
    }
  }

  const deleteWord = async (w) => {
    if (!confirm(`Supprimer le mot ${w.word} ?`)) return
    try {
      await api.adminDeleteWord(w.id)
      notify('Mot supprimé')
      await loadWords(selectedGroup)
      await loadGroups()
    } catch (err) {
      notify(err.message, 'error')
    }
  }

  const reloadDict = async () => {
    if (!confirm('Réimporter tout le dictionnaire depuis word-groups.json ?')) return
    setLoading(true)
    try {
      const res = await api.adminDictReload()
      notify(`${res.imported} mots importés`)
      await loadGroups()
      if (selectedGroup) await loadWords(selectedGroup)
    } catch (err) {
      notify(err.message, 'error')
    } finally {
      setLoading(false)
    }
  }

  const openWordFromSearch = async (hit) => {
    setGroupQuery('')
    setWordQuery('')
    setGlobalWordHits([])
    const groupKey = hit.groupCode || UNGROUPED
    setSelectedGroup(groupKey)
    try {
      await loadWords(groupKey)
      await startEditWord(hit)
    } catch (err) {
      notify(err.message, 'error')
    }
  }

  const selectedGroupMeta = selectedGroup === UNGROUPED
    ? { id: UNGROUPED, name: 'Mots sans groupe' }
    : groups.find((g) => g.id === selectedGroup)

  return (
    <section className="panel glass pop-in admin-panel">
      <div className="panel-head">
        <h2>⚙️ Administration</h2>
        <div className="admin-actions">
          <button type="button" className="btn btn-secondary btn-sm" onClick={refresh} disabled={loading}>
            {loading ? '…' : 'Rafraîchir'}
          </button>
          {tab === 'dict' && (
            <button type="button" className="btn btn-secondary btn-sm" onClick={reloadDict} disabled={loading}>
              Réimporter JSON
            </button>
          )}
        </div>
      </div>

      <div className="admin-tabs">
        {[
          ['games', 'Parties'],
          ['players', 'Joueurs'],
          ['dict', 'Dictionnaire'],
        ].map(([id, label]) => (
          <button
            key={id}
            type="button"
            className={`admin-tab ${tab === id ? 'active' : ''}`}
            onClick={() => setTab(id)}
          >
            {label}
          </button>
        ))}
      </div>

      {tab === 'games' && (
        <>
          <form
            className="admin-form admin-search-bar"
            onSubmit={(e) => {
              e.preventDefault()
              loadGames(gameSearch)
            }}
          >
            <input
              type="search"
              placeholder="Pseudo, mot secret ou n° de partie…"
              value={gameSearch.q}
              onChange={(e) => setGameSearch({ ...gameSearch, q: e.target.value })}
            />
            <select
              value={gameSearch.status}
              onChange={(e) => setGameSearch({ ...gameSearch, status: e.target.value })}
            >
              <option value="">Tous les statuts</option>
              <option value="IN_PROGRESS">En cours</option>
              <option value="WON">Gagnée</option>
              <option value="LOST">Perdue</option>
              <option value="ABANDONED">Abandonnée</option>
            </select>
            <button type="submit" className="btn btn-glow btn-sm">Rechercher</button>
            <button
              type="button"
              className="btn btn-secondary btn-sm"
              onClick={() => {
                const empty = { q: '', status: '' }
                setGameSearch(empty)
                loadGames(empty)
              }}
            >
              Tout afficher
            </button>
          </form>
          <p className="admin-hint">
            {games.length} partie{games.length !== 1 ? 's' : ''}
            {gameSearch.q.trim() ? ` pour « ${gameSearch.q.trim()} »` : ''}
          </p>
          <div className="admin-table-wrap">
            <table className="data-table">
              <thead>
                <tr>
                  <th>#</th><th>Joueur</th><th>Mot</th><th>Statut</th><th>Essais</th><th></th>
                </tr>
              </thead>
              <tbody>
                {games.length === 0 ? (
                  <tr>
                    <td colSpan={6} className="empty-row">Aucune partie trouvée</td>
                  </tr>
                ) : (
                  games.map((g) => (
                    <tr key={g.id}>
                      <td>{g.id}</td>
                      <td>{g.playerPseudo}</td>
                      <td><code>{g.secretWord}</code></td>
                      <td><span className={`mini-chip ${g.status.toLowerCase()}`}>{g.status}</span></td>
                      <td>{g.attemptsUsed}/{g.maxAttempts}</td>
                      <td>
                        <button type="button" className="btn-icon danger" onClick={() => deleteGame(g.id)} title="Supprimer">
                          🗑
                        </button>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </>
      )}

      {tab === 'players' && (
        <>
          <div className="admin-form admin-search-bar">
            <input
              type="search"
              placeholder="Rechercher par pseudo ou email…"
              value={playerQuery}
              onChange={(e) => setPlayerQuery(e.target.value)}
            />
            {playerQuery.trim() && (
              <button type="button" className="btn btn-secondary btn-sm" onClick={() => setPlayerQuery('')}>
                Effacer
              </button>
            )}
          </div>
          <p className="admin-hint">
            {filteredPlayers.length} joueur{filteredPlayers.length !== 1 ? 's' : ''}
            {playerFilter ? ` sur ${players.length}` : ''}
          </p>
          <div className="admin-table-wrap">
            <table className="data-table">
              <thead>
                <tr><th>#</th><th>Pseudo</th><th>Email</th><th>Rôle</th><th></th></tr>
              </thead>
              <tbody>
                {filteredPlayers.length === 0 ? (
                  <tr>
                    <td colSpan={5} className="empty-row">Aucun joueur trouvé</td>
                  </tr>
                ) : (
                  filteredPlayers.map((p) => (
                    <tr key={p.id}>
                      <td>{p.id}</td>
                      <td>{p.pseudo}</td>
                      <td>{p.email}</td>
                      <td><span className={`mini-chip ${p.role === 'ADMIN' ? 'won' : ''}`}>{p.role}</span></td>
                      <td className="row-actions">
                        <button type="button" className="btn btn-secondary btn-sm" onClick={() => toggleRole(p)}>
                          {p.role === 'ADMIN' ? 'Rétrograder' : 'Promouvoir'}
                        </button>
                        <button type="button" className="btn-icon danger" onClick={() => deletePlayer(p)} title="Supprimer">
                          🗑
                        </button>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </>
      )}

      {tab === 'dict' && (
        <div className="dict-admin">
          <form className="admin-form inline-form" onSubmit={createGroup}>
            <input
              placeholder="Nom du groupe (ex. Animaux)"
              value={newGroup.name}
              onChange={(e) => setNewGroup({ ...newGroup, name: e.target.value })}
              required
            />
            <input
              type="number"
              min={5}
              max={9}
              title="Longueur des mots"
              value={newGroup.length}
              onChange={(e) => setNewGroup({ ...newGroup, length: Number(e.target.value) })}
            />
            <input
              maxLength={1}
              title="Première lettre"
              value={newGroup.firstLetter}
              onChange={(e) => setNewGroup({ ...newGroup, firstLetter: e.target.value.toUpperCase().slice(0, 1) })}
            />
            <button type="submit" className="btn btn-glow btn-sm">+ Groupe</button>
          </form>
          <p className="admin-hint">L’identifiant technique du groupe est créé automatiquement à partir du nom.</p>

          <div className="admin-form admin-search-bar">
            <input
              type="search"
              placeholder="Filtrer les groupes (nom, lettre, longueur)…"
              value={groupQuery}
              onChange={(e) => setGroupQuery(e.target.value)}
            />
            <input
              type="search"
              placeholder="Rechercher un mot dans tout le dictionnaire…"
              value={wordQuery}
              onChange={(e) => setWordQuery(e.target.value)}
            />
          </div>

          {editWord && (
            <form
              ref={editPanelRef}
              className="admin-form inline-form edit-bar word-edit-panel"
              onSubmit={saveWord}
            >
              <strong className="edit-panel-title">Modifier le mot</strong>
              <input
                value={editWord.word}
                onChange={(e) => setEditWord({ ...editWord, word: e.target.value.toUpperCase() })}
                placeholder="Mot"
                required
              />
              <label className="check-label">
                <input
                  type="checkbox"
                  checked={editWord.secretWord}
                  onChange={(e) => setEditWord({ ...editWord, secretWord: e.target.checked })}
                />
                Mot mystère
              </label>
              <select
                value={editWord.groupCode || UNGROUPED}
                onChange={(e) => setEditWord({ ...editWord, groupCode: e.target.value })}
                title="Groupe"
              >
                <option value={UNGROUPED}>Sans groupe</option>
                {groups.map((g) => (
                  <option key={g.id} value={g.id}>
                    {g.name} ({g.length}L, {g.firstLetter})
                  </option>
                ))}
              </select>
              <button type="submit" className="btn btn-glow btn-sm">Enregistrer</button>
              <button type="button" className="btn btn-secondary btn-sm" onClick={() => setEditWord(null)}>
                Annuler
              </button>
            </form>
          )}

          {globalWordHits.length > 0 && (
            <div className="search-hits glass">
              <p className="admin-hint">Mots trouvés ({globalWordHits.length}) — cliquer pour modifier</p>
              <ul className="search-hit-list">
                {globalWordHits.map((hit) => (
                  <li key={hit.id}>
                    <button type="button" className="search-hit-btn" onClick={() => openWordFromSearch(hit)}>
                      <code>{hit.word}</code>
                      <span>{hit.groupName} · {hit.secretWord ? 'mystère' : 'proposition'}</span>
                    </button>
                  </li>
                ))}
              </ul>
            </div>
          )}

          <div className="dict-split">
            <div className="group-panel">
              <label className="admin-field-label" htmlFor="group-select">Groupe</label>
              <select
                id="group-select"
                className="group-select"
                value={selectedGroup || ''}
                onChange={(e) => setSelectedGroup(e.target.value || null)}
              >
                <option value="">Choisir un groupe…</option>
                <option value={UNGROUPED}>📂 Mots sans groupe</option>
                {filteredGroups.map((g) => (
                  <option key={g.id} value={g.id}>
                    {g.name} — {g.length} lettres, {g.firstLetter} ({g.secretWordCount} mystères)
                  </option>
                ))}
              </select>
              <ul className="group-list">
                <li className={selectedGroup === UNGROUPED ? 'active' : ''}>
                  <button type="button" className="group-item" onClick={() => setSelectedGroup(UNGROUPED)}>
                    <strong>Mots sans groupe</strong>
                    <span>Lexique importé / tirage aléatoire</span>
                  </button>
                </li>
                {filteredGroups.map((g) => (
                  <li key={g.id} className={selectedGroup === g.id ? 'active' : ''}>
                    <button type="button" className="group-item" onClick={() => setSelectedGroup(g.id)}>
                      <strong>{g.name}</strong>
                      <span>{g.length} lettres · commence par {g.firstLetter}</span>
                      <span className="group-meta">{g.secretWordCount} mystères / {g.guessWordCount} mots</span>
                    </button>
                    <button
                      type="button"
                      className="btn-icon danger"
                      onClick={() => deleteGroup(g.id, g.name)}
                      title="Supprimer"
                    >
                      🗑
                    </button>
                  </li>
                ))}
              </ul>
              {filteredGroups.length === 0 && (
                <p className="admin-hint">Aucun groupe ne correspond à votre recherche.</p>
              )}
            </div>

            {selectedGroup && (
              <div className="words-panel">
                {selectedGroupMeta && (
                  <p className="admin-hint words-panel-title">
                    Groupe <strong>{selectedGroupMeta.name}</strong>
                    {wordFilter && ` — ${filteredWords.length} mot(s) affiché(s)`}
                  </p>
                )}
                {selectedGroup !== UNGROUPED && (
                  <form className="admin-form inline-form" onSubmit={addWord}>
                    <input
                      placeholder="Mot"
                      value={newWord.word}
                      onChange={(e) => setNewWord({ ...newWord, word: e.target.value.toUpperCase() })}
                      required
                    />
                    <label className="check-label">
                      <input
                        type="checkbox"
                        checked={newWord.secretWord}
                        onChange={(e) => setNewWord({ ...newWord, secretWord: e.target.checked })}
                      />
                      Mot mystère
                    </label>
                    <button type="submit" className="btn btn-glow btn-sm">+ Mot</button>
                  </form>
                )}

                <div className="admin-table-wrap">
                  <table className="data-table">
                    <thead>
                      <tr><th>Mot</th><th>Type</th><th></th></tr>
                    </thead>
                    <tbody>
                      {filteredWords.length === 0 ? (
                        <tr>
                          <td colSpan={3} className="empty-row">Aucun mot dans ce groupe</td>
                        </tr>
                      ) : (
                        filteredWords.map((w) => (
                          <tr key={w.id}>
                            <td><code>{w.word}</code></td>
                            <td>{w.secretWord ? '🔒 Mystère' : 'Proposition'}</td>
                            <td className="row-actions">
                              <button
                                type="button"
                                className="btn btn-secondary btn-sm"
                                onClick={() => startEditWord(w)}
                                title="Modifier"
                              >
                                ✏️ Modifier
                              </button>
                              <button type="button" className="btn-icon danger" onClick={() => deleteWord(w)}>
                                🗑
                              </button>
                            </td>
                          </tr>
                        ))
                      )}
                    </tbody>
                  </table>
                </div>
              </div>
            )}
          </div>
        </div>
      )}
    </section>
  )
}
