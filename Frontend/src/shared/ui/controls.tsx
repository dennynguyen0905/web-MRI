import {
  type ButtonHTMLAttributes, type InputHTMLAttributes, type ReactNode, type Ref,
  type SelectHTMLAttributes, type TextareaHTMLAttributes, useEffect, useRef, useState,
} from 'react'
import { ChevronDown, Search, X } from './Icon'

export const cx = (...v: (string | false | null | undefined)[]) => v.filter(Boolean).join(' ')

/* ------------------------------------------------------------------ Button */

type Variant = 'primary' | 'outline' | 'danger' | 'ghost'
type Size = 'sm' | 'md'

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: Variant
  size?: Size
  icon?: ReactNode
  block?: boolean
}

const VARIANT: Record<Variant, string> = {
  primary: 'bg-brand-600 text-white border-brand-600 hover:bg-brand-700 hover:border-brand-700',
  outline: 'bg-white text-brand-600 border-brand-200 hover:bg-brand-50 hover:border-brand-300',
  danger: 'bg-white text-[#e02424] border-[#f7cfcf] hover:bg-[#fdf2f2]',
  ghost: 'bg-white text-ink-2 border-line hover:bg-line-soft',
}

export function Button({ variant = 'primary', size = 'sm', icon, block, className, children, ...p }: ButtonProps) {
  return (
    <button
      {...p}
      className={cx(
        'focus-ring inline-flex items-center justify-center gap-1.5 rounded-md border font-medium',
        'transition-colors disabled:cursor-not-allowed disabled:opacity-50 whitespace-nowrap',
        size === 'sm' ? 'h-7 px-2.5 text-[12px]' : 'h-8 px-3 text-[13px]',
        VARIANT[variant], block && 'w-full', className,
      )}
    >
      {icon}
      {children}
    </button>
  )
}

/* ------------------------------------------------------------- IconButton */

export function IconButton({ className, children, ...p }: ButtonHTMLAttributes<HTMLButtonElement>) {
  return (
    <button
      {...p}
      className={cx(
        'focus-ring inline-flex h-7 w-7 shrink-0 items-center justify-center rounded-md',
        'border border-line bg-white text-ink-3 transition-colors hover:bg-line-soft hover:text-ink',
        className,
      )}
    >
      {children}
    </button>
  )
}

/* ------------------------------------------------------------------- Input */

interface InputProps extends InputHTMLAttributes<HTMLInputElement> {
  withSearchIcon?: boolean
}

export function Input({ withSearchIcon, className, ...p }: InputProps) {
  return (
    <div className="relative w-full">
      {withSearchIcon && (
        <Search size={13} className="pointer-events-none absolute left-2.5 top-1/2 -translate-y-1/2 text-ink-4" />
      )}
      <input
        {...p}
        className={cx(
          'focus-ring h-7 w-full rounded-md border border-line bg-white text-[12px] text-ink',
          'placeholder:text-ink-4 transition-colors',
          withSearchIcon ? 'pl-7 pr-2.5' : 'px-2.5',
          className,
        )}
      />
    </div>
  )
}

/* ---------------------------------------------------------------- Textarea */

interface TextareaProps extends TextareaHTMLAttributes<HTMLTextAreaElement> {
  ref?: Ref<HTMLTextAreaElement>
}

export function Textarea({ className, ...p }: TextareaProps) {
  return (
    <textarea
      {...p}
      className={cx(
        'focus-ring w-full resize-y rounded-md border border-line bg-white px-2.5 py-1.5',
        'text-[12px] leading-relaxed text-ink placeholder:text-ink-4 transition-colors',
        'disabled:cursor-not-allowed disabled:bg-line-soft disabled:text-ink-3',
        className,
      )}
    />
  )
}

/* ------------------------------------------------------------------ Select */

interface SelectProps extends SelectHTMLAttributes<HTMLSelectElement> {
  options: { value: string; label: string }[]
  placeholder?: string
}

export function Select({ options, placeholder, className, ...p }: SelectProps) {
  return (
    <div className="relative w-full">
      <select
        {...p}
        className={cx(
          'focus-ring h-7 w-full appearance-none rounded-md border border-line bg-white pl-2.5 pr-7',
          'text-[12px] text-ink transition-colors',
          !p.value && placeholder && 'text-ink-4',
          className,
        )}
      >
        {placeholder && <option value="">{placeholder}</option>}
        {options.map(o => <option key={o.value} value={o.value}>{o.label}</option>)}
      </select>
      <ChevronDown size={13} className="pointer-events-none absolute right-2 top-1/2 -translate-y-1/2 text-ink-4" />
    </div>
  )
}

/* ---------------------------------------------------------------- Checkbox */

interface CheckboxProps {
  checked: boolean
  onChange: (v: boolean) => void
  label?: ReactNode
  disabled?: boolean
  className?: string
}

export function Checkbox({ checked, onChange, label, disabled, className }: CheckboxProps) {
  return (
    <label className={cx('inline-flex cursor-pointer items-center gap-2 select-none', disabled && 'cursor-not-allowed opacity-50', className)}>
      <span className="relative flex h-3.5 w-3.5 shrink-0">
        <input
          type="checkbox"
          checked={checked}
          disabled={disabled}
          onChange={e => onChange(e.target.checked)}
          className="peer h-3.5 w-3.5 cursor-pointer appearance-none rounded-[3px] border border-[#cbd5e1] bg-white transition-colors checked:border-brand-600 checked:bg-brand-600 focus-visible:outline-none focus-visible:ring-3 focus-visible:ring-brand-100 disabled:cursor-not-allowed"
        />
        <svg viewBox="0 0 16 16" className="pointer-events-none absolute inset-0 hidden h-3.5 w-3.5 text-white peer-checked:block">
          <path d="m3.5 8.2 3 3 6-6.4" fill="none" stroke="currentColor" strokeWidth="2.1" strokeLinecap="round" strokeLinejoin="round" />
        </svg>
      </span>
      {label != null && <span className="text-[12px] leading-none">{label}</span>}
    </label>
  )
}

/* -------------------------------------------------------------------- Radio */

export function Radio({ checked, onChange, label }: { checked: boolean; onChange: () => void; label: ReactNode }) {
  return (
    <label className="inline-flex cursor-pointer items-center gap-2 select-none">
      <span className="relative flex h-3.5 w-3.5 shrink-0">
        <input
          type="radio"
          checked={checked}
          onChange={onChange}
          className="peer h-3.5 w-3.5 cursor-pointer appearance-none rounded-full border border-[#cbd5e1] bg-white checked:border-brand-600 focus-visible:outline-none focus-visible:ring-3 focus-visible:ring-brand-100"
        />
        <span className="pointer-events-none absolute left-1/2 top-1/2 hidden h-1.5 w-1.5 -translate-x-1/2 -translate-y-1/2 rounded-full bg-brand-600 peer-checked:block" />
      </span>
      <span className="text-[12px] leading-none">{label}</span>
    </label>
  )
}

/* ----------------------------------------------------------------- Toggle2 */

/** Cặp nút hai trạng thái dùng trong popover "Cấu hình hệ thống". */
export function Toggle2({ value, options, onChange }: {
  value: string
  options: [string, string]
  onChange: (v: string) => void
}) {
  return (
    <div className="inline-flex overflow-hidden rounded-md border border-line">
      {options.map((o, i) => (
        <button
          key={o}
          onClick={() => onChange(o)}
          className={cx(
            'h-6 min-w-[44px] px-2.5 text-[11px] font-medium transition-colors',
            i === 1 && 'border-l border-line',
            value === o ? 'bg-brand-600 text-white' : 'bg-white text-ink-3 hover:bg-line-soft',
          )}
        >
          {o}
        </button>
      ))}
    </div>
  )
}

/* --------------------------------------------------------------------- Tag */

type TagTone = 'blue' | 'green' | 'red' | 'gray' | 'amber'

const TAG_TONE: Record<TagTone, string> = {
  blue: 'bg-brand-50 text-brand-700 border-brand-100',
  green: 'bg-[#e6f5ef] text-[#057a55] border-[#c7e9da]',
  red: 'bg-[#fdecec] text-[#c81e1e] border-[#f8d4d4]',
  gray: 'bg-[#f3f4f6] text-ink-3 border-line',
  amber: 'bg-[#fdf4e3] text-[#b45309] border-[#f6e0b5]',
}

export function Tag({ tone = 'blue', children }: { tone?: TagTone; children: ReactNode }) {
  return (
    <span className={cx('inline-flex h-[17px] items-center rounded-[3px] border px-1.5 text-[10px] font-semibold tracking-wide', TAG_TONE[tone])}>
      {children}
    </span>
  )
}

/* ----------------------------------------------------------------- Popover */

export function Popover({ open, onClose, children, className, align = 'right' }: {
  open: boolean
  onClose: () => void
  children: ReactNode
  className?: string
  align?: 'left' | 'right'
}) {
  const ref = useRef<HTMLDivElement>(null)
  useEffect(() => {
    if (!open) return
    const onDown = (e: MouseEvent) => {
      if (ref.current && !ref.current.contains(e.target as Node)) onClose()
    }
    const onKey = (e: KeyboardEvent) => { if (e.key === 'Escape') onClose() }
    document.addEventListener('mousedown', onDown)
    document.addEventListener('keydown', onKey)
    return () => {
      document.removeEventListener('mousedown', onDown)
      document.removeEventListener('keydown', onKey)
    }
  }, [open, onClose])

  if (!open) return null
  return (
    <div
      ref={ref}
      className={cx(
        'absolute z-40 mt-1.5 rounded-lg border border-line bg-white shadow-[0_8px_28px_rgba(16,24,40,.14)]',
        align === 'right' ? 'right-0' : 'left-0',
        className,
      )}
    >
      {children}
    </div>
  )
}

export function usePopover() {
  const [open, setOpen] = useState(false)
  return { open, setOpen, toggle: () => setOpen(o => !o), close: () => setOpen(false) }
}

/* -------------------------------------------------------------- EmptyState */

export function EmptyState({ title, hint, icon }: { title: string; hint?: string; icon?: ReactNode }) {
  return (
    <div className="flex flex-1 flex-col items-center justify-center gap-2.5 px-6 py-10 text-center">
      <div className="flex h-9 w-9 items-center justify-center rounded-full bg-brand-50 text-brand-500">
        {icon ?? <Search size={16} />}
      </div>
      <div className="text-[12.5px] font-semibold text-ink-2">{title}</div>
      {hint && <div className="max-w-[300px] text-[11.5px] leading-relaxed text-ink-4">{hint}</div>}
    </div>
  )
}

/* ------------------------------------------------------------------- Modal */

export function Modal({ open, onClose, title, icon, width = 520, children, footer, bodyClass }: {
  open: boolean
  onClose: () => void
  title: ReactNode
  icon?: ReactNode
  width?: number | string
  children: ReactNode
  footer?: ReactNode
  bodyClass?: string
}) {
  useEffect(() => {
    if (!open) return
    const onKey = (e: KeyboardEvent) => { if (e.key === 'Escape') onClose() }
    document.addEventListener('keydown', onKey)
    return () => document.removeEventListener('keydown', onKey)
  }, [open, onClose])

  if (!open) return null
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-[rgba(16,24,40,.45)] p-4 desktop:p-6" onMouseDown={onClose}>
      <div
        onMouseDown={e => e.stopPropagation()}
        style={{ width, maxWidth: '100%' }}
        className="flex max-h-full flex-col overflow-hidden rounded-lg bg-white shadow-[0_20px_60px_rgba(16,24,40,.3)]"
      >
        <div className="flex h-11 shrink-0 items-center gap-2 border-b border-line px-4 desktop:h-12">
          {icon}
          <div className="flex-1 text-[13px] font-semibold text-ink">{title}</div>
          <button onClick={onClose} className="focus-ring rounded p-1 text-ink-4 transition-colors hover:bg-line-soft hover:text-ink">
            <X size={15} />
          </button>
        </div>
        <div className={cx('min-h-0 flex-1 overflow-auto', bodyClass ?? 'p-4 desktop:p-5')}>{children}</div>
        {footer && <div className="flex shrink-0 items-center justify-end gap-2 border-t border-line px-4 py-2.5 desktop:py-3">{footer}</div>}
      </div>
    </div>
  )
}
