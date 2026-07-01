import { useEffect, useState } from 'react'

export function Confetti({ active }) {
  const [pieces] = useState(() =>
    Array.from({ length: 48 }, (_, i) => ({
      id: i,
      left: Math.random() * 100,
      delay: Math.random() * 0.8,
      duration: 1.8 + Math.random() * 1.2,
      color: ['#ffd93d', '#6bcb77', '#4d96ff', '#ff6b9d', '#c9b1ff'][i % 5],
      size: 6 + Math.random() * 8,
    }))
  )

  if (!active) return null

  return (
    <div className="confetti-layer" aria-hidden="true">
      {pieces.map((p) => (
        <span
          key={p.id}
          className="confetti-piece"
          style={{
            left: `${p.left}%`,
            animationDelay: `${p.delay}s`,
            animationDuration: `${p.duration}s`,
            background: p.color,
            width: p.size,
            height: p.size,
          }}
        />
      ))}
    </div>
  )
}

export function WinOverlay({ show, secretWord, attempts, onPlayAgain }) {
  if (!show) return null
  return (
    <div className="overlay overlay-win">
      <div className="overlay-card pop-in">
        <div className="overlay-emoji bounce">🏆</div>
        <h2>Bravo !</h2>
        <p>Tu as trouvé <strong>{secretWord}</strong></p>
        <p className="overlay-sub">en {attempts} essai{attempts > 1 ? 's' : ''}</p>
        <button type="button" className="btn btn-glow" onClick={onPlayAgain}>
          Rejouer
        </button>
      </div>
    </div>
  )
}

export function LoseOverlay({ show, secretWord, onPlayAgain }) {
  if (!show) return null
  return (
    <div className="overlay overlay-lose">
      <div className="overlay-card pop-in">
        <div className="overlay-emoji">💫</div>
        <h2>Presque…</h2>
        <p>Le mot était <strong>{secretWord}</strong></p>
        <p className="overlay-sub">Tu feras mieux la prochaine fois !</p>
        <button type="button" className="btn btn-glow" onClick={onPlayAgain}>
          Nouvelle partie
        </button>
      </div>
    </div>
  )
}
