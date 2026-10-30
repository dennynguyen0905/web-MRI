import { useMemo, useState } from 'react'
import { AddStudyModal } from '../components/modals/AddStudyModal'
import { ClinicalModal } from '../components/modals/ClinicalModal'
import { UpdateFromHisModal } from '../components/modals/UpdateFromHisModal'
import { UpdateStudyModal } from '../components/modals/UpdateStudyModal'
import { Sidebar, DEFAULT_SIDEBAR_FILTERS, type SidebarFilters } from '../components/layout/Sidebar'
import { TopBar, type TopBarState } from '../components/layout/TopBar'
import { Pagination } from '../components/worklist/Pagination'
import { PatientPanel } from '../components/worklist/PatientPanel'
import { RelatedOrders } from '../components/worklist/RelatedOrders'
import { COLUMNS, WorklistTable, useOptionalColumns, type ColumnKey } from '../components/worklist/WorklistTable'
import { Button, Select, cx } from '../components/ui/controls'
import { Download, Printer } from '../components/ui/Icon'
import { ReportModal, type ReportDraft } from '../components/modals/ReportModal'
import { can, deniedReason } from '../lib/auth'
import { calcAge } from '../lib/format'
import { useWorklistStore } from '../lib/useWorklistStore'
import type { AppUser, StudyStatus, WorklistRow } from '../lib/types'

type Tab = 'PENDING' | 'DONE'

const PENDING_COLUMNS: ColumnKey[] = [
  'index', 'patientCode', 'fullName', 'dob', 'age', 'gender', 'service', 'orderingDoctor',
  'bodyPart', 'conclusion', 'groupCode', 'groupName', 'recordCode', 'specimenCode', 'archiveCode',
]

const DONE_COLUMNS: ColumnKey[] = [
  'index', 'status', 'patientInfo', 'service', 'scannedAt', 'startedAt', 'completedAt',
  'orderingDoctor', 'conclusion', 'bodyPart', 'groupCode', 'groupName', 'recordCode', 'specimenCode', 'archiveCode',
]

const DEFAULT_TOPBAR: TopBarState = {
  patientCode: '',
  patientName: '',
  visitCode: '',
  range: 'TODAY',
  from: '2026-08-09',
  to: '2026-08-09',
}

export function WorklistPage({ user, onLogout }: { user: AppUser; onLogout: () => void }) {
  const { rows: allRows, reports, history, saveDraft, transition } = useWorklistStore(user)
  const [tab, setTab] = useState<Tab>('PENDING')
  const [topbar, setTopbar] = useState<TopBarState>(DEFAULT_TOPBAR)
  const [filters, setFilters] = useState<SidebarFilters>(DEFAULT_SIDEBAR_FILTERS)
  const [selectedId, setSelectedId] = useState<number | null>(null)
  const [page, setPage] = useState(1)
  const [pageSize, setPageSize] = useState(20)
  const [timeField, setTimeField] = useState('PERFORMED')

  const { visible: optionalCols, toggle: toggleOptional } = useOptionalColumns()
  const [columnFilterMode, setColumnFilterMode] = useState(false)
  const [columnFilters, setColumnFilters] = useState<Partial<Record<ColumnKey, string>>>({})
  const [sort, setSort] = useState<{ key: ColumnKey; dir: 'asc' | 'desc' } | null>(null)

  const [modal, setModal] = useState<null | 'ADD' | 'UPDATE' | 'HIS' | 'CLINICAL' | 'REPORT'>(null)

  /* --------------------------------------------------------------- Lọc dữ liệu */

  const filtered = useMemo(() => {
    const code = topbar.patientCode.trim().toLowerCase()
    const name = topbar.patientName.trim().toLowerCase()
    const visit = topbar.visitCode.trim().toLowerCase()

    return allRows.filter(r => {
      // Tab: "Chờ" = chờ thực hiện, "Đã thực hiện" = đã có ảnh trở đi.
      if (tab === 'PENDING' && r.study.status !== 'CHO_THUC_HIEN') return false
      if (tab === 'DONE') {
        if (r.study.status === 'CHO_THUC_HIEN') return false
        if (!filters.statuses.includes(r.study.status)) return false
      }

      if (code && !r.patient.patientCode.toLowerCase().includes(code)) return false
      if (name && !r.patient.fullName.toLowerCase().includes(name)) return false
      if (visit && !(r.order.visitCode ?? '').toLowerCase().includes(visit)) return false

      if (filters.modality && r.service.modality !== filters.modality) return false
      if (filters.patientClass && r.order.patientClass !== filters.patientClass) return false
      if (filters.orderingDoctor && r.order.orderingDoctorName !== filters.orderingDoctor) return false
      if (filters.serviceId && String(r.service.id) !== filters.serviceId) return false
      if (filters.bodyPart && r.service.bodyPart !== filters.bodyPart) return false
      if (filters.conclusion === 'HAS' && !r.study.conclusion) return false
      if (filters.conclusion === 'NONE' && r.study.conclusion) return false
      if (filters.bhyt === 'VIOLATION' && !r.study.bhytViolation) return false
      if (filters.bhyt === 'OK' && r.study.bhytViolation) return false
      if (filters.hisSync && filters.hisSync !== 'ALL' && r.order.syncStatus !== filters.hisSync) return false

      // Lọc theo từng cột (chế độ "lọc theo cột").
      for (const [key, value] of Object.entries(columnFilters)) {
        if (!value) continue
        const text = columnText(r, key as ColumnKey).toLowerCase()
        if (!text.includes(value.toLowerCase())) return false
      }
      return true
    })
  }, [allRows, tab, topbar, filters, columnFilters])

  const sorted = useMemo(() => {
    if (!sort) return filtered
    const dir = sort.dir === 'asc' ? 1 : -1
    return [...filtered].sort((a, b) =>
      columnText(a, sort.key).localeCompare(columnText(b, sort.key), 'vi', { numeric: true }) * dir,
    )
  }, [filtered, sort])

  const pageRows = useMemo(
    () => sorted.slice((page - 1) * pageSize, page * pageSize),
    [sorted, page, pageSize],
  )

  const selected = useMemo(
    () => allRows.find(r => r.study.id === selectedId) ?? null,
    [allRows, selectedId],
  )

  const related = useMemo(
    () => (selected ? allRows.filter(r => r.patient.id === selected.patient.id) : []),
    [allRows, selected],
  )

  const selectedReport = selectedId != null ? reports[selectedId] ?? null : null
  const selectedHistory = selectedId != null ? history[selectedId] ?? [] : []

  /* -------------------------------------------------------------- Thao tác */

  const patch = (next: Partial<TopBarState>) => { setTopbar(s => ({ ...s, ...next })); setPage(1) }
  const patchFilters = (next: Partial<SidebarFilters>) => { setFilters(s => ({ ...s, ...next })); setPage(1) }

  const onSort = (key: ColumnKey) =>
    setSort(s => (s?.key === key ? (s.dir === 'asc' ? { key, dir: 'desc' } : null) : { key, dir: 'asc' }))

  const onColumnFilter = (key: ColumnKey, value: string) => {
    setColumnFilters(f => ({ ...f, [key]: value }))
    setPage(1)
  }

  return (
    <div className="flex h-full flex-col overflow-hidden">
      <TopBar
        state={topbar}
        onChange={patch}
        onReset={() => { setTopbar(DEFAULT_TOPBAR); setFilters(DEFAULT_SIDEBAR_FILTERS); setColumnFilters({}); setPage(1) }}
        onAddPatient={() => setModal('ADD')}
        user={user}
        onLogout={onLogout}
      />

      <div className="flex min-h-0 flex-1">
        <Sidebar filters={filters} onChange={patchFilters} visibleCount={sorted.length} />

        <main className="flex min-w-0 flex-1 flex-col overflow-hidden bg-app p-2">
          <div className="flex min-h-0 flex-1 flex-col overflow-hidden rounded-lg border border-line bg-white">
            {/* Thanh tab */}
            <div className="flex h-[38px] shrink-0 items-center gap-1.5 border-b border-line px-2">
              <TabButton active={tab === 'PENDING'} onClick={() => { setTab('PENDING'); setPage(1) }}>Chờ</TabButton>
              <TabButton active={tab === 'DONE'} onClick={() => { setTab('DONE'); setPage(1) }}>Đã thực hiện</TabButton>

              <div className="flex-1" />

              <div className="w-[150px]">
                <Select
                  value={timeField}
                  onChange={e => setTimeField(e.target.value)}
                  options={[
                    { value: 'PERFORMED', label: 'Thời gian thực hiện' },
                    { value: 'ORDERED', label: 'Thời gian chỉ định' },
                    { value: 'APPROVED', label: 'Thời gian duyệt' },
                  ]}
                />
              </div>
              <Button
                variant="ghost" icon={<Printer size={12} />}
                disabled={!can(user, 'list.print')}
                title={can(user, 'list.print') ? undefined : deniedReason('list.print')}
              >
                In danh sách
              </Button>
              <Button variant="ghost" icon={<Download size={12} />} disabled={!can(user, 'list.print')}>
                Tải danh sách
              </Button>
            </div>

            <WorklistTable
              rows={pageRows}
              columns={tab === 'PENDING' ? PENDING_COLUMNS : DONE_COLUMNS}
              selectedId={selectedId}
              onSelect={r => setSelectedId(r.study.id)}
              onOpen={r => { setSelectedId(r.study.id); setModal('CLINICAL') }}
              visibleOptional={optionalCols}
              onToggleOptional={toggleOptional}
              columnFilterMode={columnFilterMode}
              onToggleColumnFilterMode={() => setColumnFilterMode(v => !v)}
              columnFilters={columnFilters}
              onColumnFilter={onColumnFilter}
              sort={sort}
              onSort={onSort}
            />

            <Pagination
              page={page}
              pageSize={pageSize}
              total={sorted.length}
              onPage={setPage}
              onPageSize={n => { setPageSize(n); setPage(1) }}
            />

            <RelatedOrders rows={related} selectedId={selectedId} onSelect={r => setSelectedId(r.study.id)} />
          </div>
        </main>

        <PatientPanel
          row={selected}
          user={user}
          report={selectedReport}
          onUpdate={() => setModal('UPDATE')}
          onUpdateFromHis={() => setModal('HIS')}
          onAddOrder={() => setModal('ADD')}
          onClinical={() => setModal('CLINICAL')}
          onReport={() => setModal('REPORT')}
        />
      </div>

      <AddStudyModal open={modal === 'ADD'} onClose={() => setModal(null)} />
      <UpdateStudyModal open={modal === 'UPDATE'} onClose={() => setModal(null)} row={selected} />
      <UpdateFromHisModal open={modal === 'HIS'} onClose={() => setModal(null)} row={selected} />
      <ClinicalModal open={modal === 'CLINICAL'} onClose={() => setModal(null)} row={selected} />
      <ReportModal
        open={modal === 'REPORT'}
        onClose={() => setModal(null)}
        row={selected}
        user={user}
        report={selectedReport}
        history={selectedHistory}
        onSaveDraft={(draft: ReportDraft) => selectedId != null && saveDraft(selectedId, draft)}
        onTransition={(to: StudyStatus, draft: ReportDraft, reason: string | null) => {
          if (selectedId == null) return
          transition(selectedId, to, draft, reason)
        }}
      />
    </div>
  )
}

function TabButton({ active, onClick, children }: { active: boolean; onClick: () => void; children: React.ReactNode }) {
  return (
    <button
      onClick={onClick}
      className={cx(
        'focus-ring h-7 rounded-md px-3.5 text-[12px] font-semibold transition-colors',
        active ? 'bg-brand-600 text-white' : 'text-ink-3 hover:bg-line-soft hover:text-ink',
      )}
    >
      {children}
    </button>
  )
}

/** Giá trị văn bản của một ô — dùng chung cho sắp xếp và lọc theo cột. */
function columnText(r: WorklistRow, key: ColumnKey): string {
  switch (key) {
    case 'patientCode': return r.patient.patientCode
    case 'fullName':
    case 'patientInfo': return r.patient.fullName
    case 'dob': return r.patient.dateOfBirth ?? ''
    case 'age': return calcAge(r.patient.dateOfBirth, r.patient.dobPrecision)
    case 'gender': return r.patient.gender
    case 'service': return r.service.name
    case 'orderingDoctor': return r.order.orderingDoctorName
    case 'bodyPart': return r.service.bodyPart
    case 'conclusion': return r.study.conclusion ?? ''
    case 'status': return r.study.status
    case 'orderCode': return r.order.orderCode
    case 'recordCode': return r.study.accessionNumber
    case 'scannedAt': return r.study.scheduledAt
    case 'startedAt': return r.study.startedAt ?? ''
    case 'completedAt': return r.study.completedAt ?? ''
    default: return ''
  }
}

export { COLUMNS }
