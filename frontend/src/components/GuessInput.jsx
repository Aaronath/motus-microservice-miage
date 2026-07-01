import { useCallback, useEffect, useRef, useState } from 'react'

/** Cases Motus : 1ère lettre fixe, curseur visible, clic pour choisir la position. */
export function GuessInput({ firstLetter, wordLength, value, onChange, disabled }) {
  const inputRef = useRef(null)
  const letter = firstLetter || ''
  const minCaret = letter ? 1 : 0
  const [caret, setCaret] = useState(minCaret)

  const parseChars = useCallback(
    (raw) => {
      const arr = Array(wordLength).fill('')
      if (letter) arr[0] = letter
      const v = (raw || letter || '').toUpperCase().replace(/[^A-Z]/g, '')
      for (let i = letter ? 1 : 0; i < wordLength; i++) {
        arr[i] = v[i] || ''
      }
      return arr
    },
    [letter, wordLength],
  )

  const charsToValue = (arr) => {
    let last = letter ? 0 : -1
    for (let i = arr.length - 1; i >= 0; i--) {
      if (arr[i]) {
        last = i
        break
      }
    }
    if (last < 0) return letter || ''
    return arr.slice(0, last + 1).join('')
  }

  const chars = parseChars(value)
  const display = charsToValue(chars)

  const focusInput = () => inputRef.current?.focus()

  const moveCaret = (pos) => {
    const clamped = Math.min(Math.max(pos, minCaret), wordLength)
    setCaret(clamped)
    requestAnimationFrame(() => {
      inputRef.current?.setSelectionRange(clamped, clamped)
    })
  }

  const commit = (nextChars, nextCaret) => {
    onChange(charsToValue(nextChars))
    moveCaret(nextCaret)
  }

  const insertLetter = (ch, at) => {
    if (at < minCaret || at >= wordLength) return
    const next = [...chars]
    next[at] = ch
    commit(next, Math.min(at + 1, wordLength))
  }

  const deleteBefore = (at) => {
    if (at <= minCaret) return
    const next = [...chars]
    for (let i = at - 1; i < wordLength - 1; i++) {
      next[i] = next[i + 1] || (i === 0 ? letter : '')
    }
    next[wordLength - 1] = ''
    if (letter) next[0] = letter
    commit(next, at - 1)
  }

  const deleteAt = (at) => {
    if (at >= wordLength) return
    const next = [...chars]
    for (let i = at; i < wordLength - 1; i++) {
      next[i] = next[i + 1] || (i === 0 ? letter : '')
    }
    next[wordLength - 1] = ''
    if (letter) next[0] = letter
    commit(next, at)
  }

  const applyPaste = (text) => {
    let v = text.toUpperCase().replace(/[^A-Z]/g, '')
    if (letter) {
      if (v[0] !== letter) {
        v = letter + v.replace(new RegExp(`^${letter}+`, 'g'), '')
      }
      v = v.slice(0, wordLength)
    } else {
      v = v.slice(0, wordLength)
    }
    const next = parseChars(v)
    commit(next, Math.min(v.length, wordLength))
  }

  useEffect(() => {
    setCaret(minCaret)
  }, [letter, wordLength, minCaret])

  useEffect(() => {
    const v = (value || '').toUpperCase()
    const atWordStart = letter ? v === letter || v === '' : v === ''
    if (atWordStart) {
      setCaret(minCaret)
      requestAnimationFrame(() => {
        inputRef.current?.setSelectionRange(minCaret, minCaret)
        if (!disabled) inputRef.current?.focus()
      })
    }
  }, [value, letter, minCaret, disabled])

  useEffect(() => {
    if (!disabled) {
      focusInput()
    }
  }, [letter, wordLength, disabled])

  const handleKeyDown = (e) => {
    if (disabled) return

    if (e.key.length === 1 && /[a-zA-Z]/.test(e.key)) {
      e.preventDefault()
      insertLetter(e.key.toUpperCase(), caret)
      return
    }

    switch (e.key) {
      case 'Backspace':
        e.preventDefault()
        deleteBefore(caret)
        break
      case 'Delete':
        e.preventDefault()
        deleteAt(caret)
        break
      case 'ArrowLeft':
        e.preventDefault()
        moveCaret(caret - 1)
        break
      case 'ArrowRight':
        e.preventDefault()
        moveCaret(caret + 1)
        break
      case 'Home':
        e.preventDefault()
        moveCaret(minCaret)
        break
      case 'End':
        e.preventDefault()
        moveCaret(Math.max(minCaret, display.length))
        break
      default:
        break
    }
  }

  const handleChange = (e) => {
    if (disabled) return
    const pasted = e.target.value
    if (pasted && pasted !== display) {
      applyPaste(pasted)
    }
    moveCaret(caret)
  }

  const handleSelect = (e) => {
    const start = e.target.selectionStart ?? minCaret
    setCaret(Math.min(Math.max(start, minCaret), wordLength))
  }

  const focusSlot = (index) => {
    if (disabled) return
    focusInput()
    moveCaret(Math.max(index, minCaret))
  }

  const caretVisible = !disabled
  const caretSlot = caretVisible
    ? Math.min(Math.max(caret, minCaret), wordLength - 1)
    : -1

  return (
    <div className="guess-input-wrap" onClick={focusInput}>
      <div className="guess-slots" role="presentation">
        {Array.from({ length: wordLength }, (_, i) => {
          const isFirst = i === 0 && !!letter
          const ch = chars[i] || ''
          const isBlank = !isFirst && !ch
          const showChar = isFirst ? letter : (ch || '_')
          const isCaretHere = i === caretSlot

          return (
            <button
              key={i}
              type="button"
              tabIndex={-1}
              disabled={disabled}
              className={[
                'guess-slot',
                isFirst ? 'guess-slot-first' : '',
                isBlank ? 'guess-slot-blank' : 'guess-slot-filled',
                isCaretHere ? 'guess-slot-caret' : '',
              ].join(' ')}
              onClick={(e) => {
                e.stopPropagation()
                focusSlot(i)
              }}
            >
              <span className="guess-slot-char">{showChar}</span>
              <span className="guess-slot-line" aria-hidden />
            </button>
          )
        })}
      </div>
      <input
        ref={inputRef}
        type="text"
        inputMode="text"
        autoCapitalize="characters"
        autoCorrect="off"
        spellCheck={false}
        value={display}
        onChange={handleChange}
        onKeyDown={handleKeyDown}
        onSelect={handleSelect}
        onClick={handleSelect}
        maxLength={wordLength}
        className="guess-input-overlay"
        autoComplete="off"
        disabled={disabled}
        aria-label={`Mot de ${wordLength} lettres commençant par ${letter}`}
      />
      <span className="char-count">{display.length}/{wordLength}</span>
    </div>
  )
}
