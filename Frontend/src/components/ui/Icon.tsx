import type { SVGProps } from 'react'

type P = SVGProps<SVGSVGElement> & { size?: number }

/** Bộ icon nét mảnh dùng chung, theo trang "DS / Icons" trong bản thiết kế. */
function base({ size = 16, ...p }: P) {
  return {
    width: size,
    height: size,
    viewBox: '0 0 24 24',
    fill: 'none',
    stroke: 'currentColor',
    strokeWidth: 1.7,
    strokeLinecap: 'round' as const,
    strokeLinejoin: 'round' as const,
    ...p,
  }
}

export const Search = (p: P) => (
  <svg {...base(p)}><circle cx="11" cy="11" r="7" /><path d="m20 20-3.5-3.5" /></svg>
)
export const X = (p: P) => (
  <svg {...base(p)}><path d="M18 6 6 18M6 6l12 12" /></svg>
)
export const ChevronDown = (p: P) => (
  <svg {...base(p)}><path d="m6 9 6 6 6-6" /></svg>
)
export const ChevronUp = (p: P) => (
  <svg {...base(p)}><path d="m6 15 6-6 6 6" /></svg>
)
export const ChevronRight = (p: P) => (
  <svg {...base(p)}><path d="m9 6 6 6-6 6" /></svg>
)
export const ChevronLeft = (p: P) => (
  <svg {...base(p)}><path d="m15 6-6 6 6 6" /></svg>
)
export const Refresh = (p: P) => (
  <svg {...base(p)}><path d="M3 12a9 9 0 0 1 15.3-6.4L21 8" /><path d="M21 3v5h-5" /><path d="M21 12a9 9 0 0 1-15.3 6.4L3 16" /><path d="M3 21v-5h5" /></svg>
)
export const Settings = (p: P) => (
  <svg {...base(p)}><circle cx="12" cy="12" r="3" /><path d="M19.4 15a1.6 1.6 0 0 0 .3 1.8l.1.1a2 2 0 1 1-2.8 2.8l-.1-.1a1.6 1.6 0 0 0-1.8-.3 1.6 1.6 0 0 0-1 1.5V21a2 2 0 1 1-4 0v-.1A1.6 1.6 0 0 0 9 19.4a1.6 1.6 0 0 0-1.8.3l-.1.1a2 2 0 1 1-2.8-2.8l.1-.1a1.6 1.6 0 0 0 .3-1.8 1.6 1.6 0 0 0-1.5-1H3a2 2 0 1 1 0-4h.1A1.6 1.6 0 0 0 4.6 9a1.6 1.6 0 0 0-.3-1.8l-.1-.1a2 2 0 1 1 2.8-2.8l.1.1a1.6 1.6 0 0 0 1.8.3H9a1.6 1.6 0 0 0 1-1.5V3a2 2 0 1 1 4 0v.1a1.6 1.6 0 0 0 1 1.5 1.6 1.6 0 0 0 1.8-.3l.1-.1a2 2 0 1 1 2.8 2.8l-.1.1a1.6 1.6 0 0 0-.3 1.8V9a1.6 1.6 0 0 0 1.5 1H21a2 2 0 1 1 0 4h-.1a1.6 1.6 0 0 0-1.5 1Z" /></svg>
)
export const SlidersH = (p: P) => (
  <svg {...base(p)}><path d="M3 7h12M19 7h2M3 17h2M9 17h12" /><circle cx="17" cy="7" r="2" /><circle cx="7" cy="17" r="2" /></svg>
)
export const User = (p: P) => (
  <svg {...base(p)}><circle cx="12" cy="8" r="3.5" /><path d="M5 20a7 7 0 0 1 14 0" /></svg>
)
export const UserPlus = (p: P) => (
  <svg {...base(p)}><circle cx="10" cy="8" r="3.5" /><path d="M3 20a7 7 0 0 1 12.3-4.6" /><path d="M18 14v6M15 17h6" /></svg>
)
export const Plus = (p: P) => (
  <svg {...base(p)}><path d="M12 5v14M5 12h14" /></svg>
)
export const Folder = (p: P) => (
  <svg {...base(p)}><path d="M3 7a2 2 0 0 1 2-2h3.6l2 2H19a2 2 0 0 1 2 2v8a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2Z" /></svg>
)
export const Lock = (p: P) => (
  <svg {...base(p)}><rect x="4.5" y="10.5" width="15" height="9.5" rx="2" /><path d="M8 10.5V7.8a4 4 0 0 1 8 0v2.7" /></svg>
)
export const Monitor = (p: P) => (
  <svg {...base(p)}><rect x="3" y="4" width="18" height="12" rx="2" /><path d="M9 20h6M12 16v4" /></svg>
)
export const Printer = (p: P) => (
  <svg {...base(p)}><path d="M7 9V3h10v6" /><rect x="3" y="9" width="18" height="7" rx="2" /><path d="M7 14h10v7H7z" /></svg>
)
export const Download = (p: P) => (
  <svg {...base(p)}><path d="M12 3v12" /><path d="m7.5 10.5 4.5 4.5 4.5-4.5" /><path d="M4 20h16" /></svg>
)
export const Check = (p: P) => (
  <svg {...base(p)}><path d="m5 12.5 4.5 4.5L19 7" /></svg>
)
export const Clock = (p: P) => (
  <svg {...base(p)}><circle cx="12" cy="12" r="8.5" /><path d="M12 7.5V12l3 2" /></svg>
)
export const Calendar = (p: P) => (
  <svg {...base(p)}><rect x="3.5" y="5" width="17" height="15.5" rx="2" /><path d="M3.5 10h17M8 3v4M16 3v4" /></svg>
)
export const FileText = (p: P) => (
  <svg {...base(p)}><path d="M14 3H7a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V8Z" /><path d="M14 3v5h5M9 13h6M9 17h4" /></svg>
)
export const Edit = (p: P) => (
  <svg {...base(p)}><path d="M12 20h8" /><path d="M16.5 3.5a2.1 2.1 0 0 1 3 3L7.5 18.5 3 20l1.5-4.5Z" /></svg>
)
export const Stethoscope = (p: P) => (
  <svg {...base(p)}><path d="M6 3v5a4 4 0 0 0 8 0V3" /><path d="M4 3h3M13 3h3" /><path d="M10 12v2a5 5 0 0 0 10 0v-1" /><circle cx="20" cy="11" r="2" /></svg>
)
export const Sync = (p: P) => (
  <svg {...base(p)}><path d="M4 11a8 8 0 0 1 13.6-5.7L20 7" /><path d="M20 3v4h-4" /><path d="M20 13a8 8 0 0 1-13.6 5.7L4 17" /><path d="M4 21v-4h4" /></svg>
)
export const Warning = (p: P) => (
  <svg {...base(p)}><path d="M10.3 3.9 2.6 17a2 2 0 0 0 1.7 3h15.4a2 2 0 0 0 1.7-3L13.7 3.9a2 2 0 0 0-3.4 0Z" /><path d="M12 9v4.5M12 17h.01" /></svg>
)
export const Trash = (p: P) => (
  <svg {...base(p)}><path d="M4 7h16M10 11v6M14 11v6" /><path d="M6 7l1 13h10l1-13" /><path d="M9 7V4h6v3" /></svg>
)
export const Eye = (p: P) => (
  <svg {...base(p)}><path d="M2 12s3.6-6.5 10-6.5S22 12 22 12s-3.6 6.5-10 6.5S2 12 2 12Z" /><circle cx="12" cy="12" r="2.8" /></svg>
)
export const Grid = (p: P) => (
  <svg {...base(p)}><rect x="3.5" y="3.5" width="7" height="7" rx="1.5" /><rect x="13.5" y="3.5" width="7" height="7" rx="1.5" /><rect x="3.5" y="13.5" width="7" height="7" rx="1.5" /><rect x="13.5" y="13.5" width="7" height="7" rx="1.5" /></svg>
)
export const Pin = (p: P) => (
  <svg {...base(p)}><path d="M9 3h6l-1 6 4 3v2H6v-2l4-3-1-6Z" /><path d="M12 14v7" /></svg>
)
export const Sort = (p: P) => (
  <svg {...base(p)} strokeWidth={1.4}><path d="M8 9 12 5l4 4M8 15l4 4 4-4" /></svg>
)
