import { useEffect } from 'react'

export function ToastStack({ toasts, onDismiss }) {
  return (
    <div className="toast-stack" aria-live="polite">
      {toasts.map((t) => (
        <Toast key={t.id} toast={t} onDismiss={() => onDismiss(t.id)} />
      ))}
    </div>
  )
}

function Toast({ toast, onDismiss }) {
  useEffect(() => {
    const t = setTimeout(onDismiss, toast.duration ?? 4500)
    return () => clearTimeout(t)
  }, [toast, onDismiss])

  return (
    <div className={`toast toast-${toast.type}`} role="alert">
      <span className="toast-icon">{toast.icon}</span>
      <span className="toast-msg">{toast.message}</span>
      <button type="button" className="toast-close" onClick={onDismiss} aria-label="Fermer">×</button>
    </div>
  )
}

let toastId = 0
export function createToast(message, type = 'error', icon = '⚠️') {
  return { id: ++toastId, message, type, icon, duration: type === 'success' ? 5000 : 4500 }
}
