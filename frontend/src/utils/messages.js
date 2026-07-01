const ERROR_MAP = [
  [/mot doit contenir|doit faire exactement|même longueur/i, (ctx) =>
    `Il faut un mot de ${ctx?.wordLength ?? 7} lettres exactement.`],
  [/première lettre doit être/i, (ctx) =>
    `Ton mot doit commencer par « ${ctx?.firstLetter ?? '?'} » (comme indiqué).`],
  [/absent du dictionnaire/i, () =>
    "Ce mot n'existe pas dans notre dictionnaire — essaie un autre !"],
  [/partie terminée/i, () => 'Cette partie est déjà finie. Lance-en une nouvelle !'],
  [/identifiants invalides|unauthorized/i, () => 'Pseudo ou mot de passe incorrect.'],
  [/pseudo déjà utilisé/i, () => 'Ce pseudo est déjà pris, choisis-en un autre.'],
  [/email déjà utilisé/i, () => 'Cet email est déjà enregistré.'],
  [/forbidden|accès refusé/i, () => "Tu n'as pas accès à cette ressource."],
  [/network|failed to fetch/i, () => 'Connexion impossible — vérifie que le serveur tourne.'],
  [/bad request/i, () => 'Proposition invalide — vérifie la longueur et la première lettre.'],
]

export function friendlyError(raw, ctx = {}) {
  const msg = String(raw || 'Une erreur est survenue')
  for (const [re, fn] of ERROR_MAP) {
    if (re.test(msg)) return fn(ctx)
  }
  return msg.length > 120 ? 'Oups, quelque chose a mal tourné. Réessaie !' : msg
}

export const STATUS = {
  IN_PROGRESS: { label: 'En cours', emoji: '🎯', className: 'status-progress' },
  WON: { label: 'Victoire !', emoji: '🏆', className: 'status-won' },
  LOST: { label: 'Dommage…', emoji: '💫', className: 'status-lost' },
  ABANDONED: { label: 'Abandonnée', emoji: '🚪', className: 'status-abandoned' },
}

export const HISTORY_STATUS = {
  IN_PROGRESS: { label: 'En cours', emoji: '🎯', className: 'progress' },
  WON: { label: 'Victoire', emoji: '✅', className: 'won' },
  LOST: { label: 'Défaite', emoji: '❌', className: 'lost' },
  ABANDONED: { label: 'Abandonnée', emoji: '🚪', className: 'abandoned' },
}

export const LETTER_HELP = {
  FIRST: { label: 'Donnée', color: 'var(--first)' },
  WELL_PLACED: { label: 'Bien placée', color: 'var(--well)' },
  MISPLACED: { label: 'Mal placée', color: 'var(--mis)' },
  ABSENT: { label: 'Absente', color: 'var(--abs)' },
}
