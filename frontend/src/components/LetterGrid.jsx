export function LetterGrid({ guesses, wordLength, maxAttempts, shake }) {
  const rows = []
  for (let i = 0; i < maxAttempts; i++) {
    if (i < guesses.length) {
      rows.push({ type: 'filled', letters: guesses[i].letters, key: `g-${i}` })
    } else {
      rows.push({ type: 'empty', key: `e-${i}`, highlight: i === guesses.length })
    }
  }

  return (
    <div className={`letter-grid ${shake ? 'shake' : ''}`}>
      {rows.map((row) =>
        row.type === 'filled' ? (
          <div className="grid-row row-reveal" key={row.key}>
            {row.letters.map((l, j) => (
              <div
                key={j}
                className={`cell cell-${l.state}`}
                style={{ animationDelay: `${j * 0.07}s` }}
                title={l.state}
              >
                {l.letter}
              </div>
            ))}
          </div>
        ) : (
          <div className={`grid-row row-empty ${row.highlight ? 'row-next' : ''}`} key={row.key}>
            {Array.from({ length: wordLength }, (_, j) => (
              <div key={j} className={`cell cell-empty ${j === 0 && row.highlight ? 'cell-first-hint' : ''}`} />
            ))}
          </div>
        )
      )}
    </div>
  )
}

export function ColorLegend() {
  const items = [
    { cls: 'cell-FIRST', label: 'Lettre imposée' },
    { cls: 'cell-WELL_PLACED', label: 'Bien placée' },
    { cls: 'cell-MISPLACED', label: 'Mal placée' },
    { cls: 'cell-ABSENT', label: 'Absente' },
  ]
  return (
    <div className="legend">
      {items.map(({ cls, label }) => (
        <span key={cls} className="legend-item">
          <span className={`cell mini ${cls}`}>A</span>
          {label}
        </span>
      ))}
    </div>
  )
}
