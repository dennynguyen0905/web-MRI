import type { AiFinding } from '@/types/aiAnalysis'

const LEGEND_COLORS = ['var(--danger-500)', 'var(--warning-600)', 'var(--accent-cyan)', 'var(--primary-500)', 'var(--success-600)']

/**
 * Khối overlay ảnh — doc/11-ai-design.md mục 5.2.5.a. Toggle MẶC ĐỊNH TẮT (chống automation
 * bias, mục 7) — trạng thái `on` do component cha khởi tạo `false` và không đọc từ fixture.
 * OHIF tự vẽ mask (DICOM SEG) thật — khung `overlay-preview` dưới đây chỉ minh hoạ VỊ TRÍ,
 * không phải code vẽ ảnh (nhóm không code phần render, đúng ranh giới mục A).
 */
export function OverlaySection({
  findings,
  on,
  onToggle,
  opacity,
  onOpacityChange,
}: {
  findings: AiFinding[]
  on: boolean
  onToggle: () => void
  opacity: number
  onOpacityChange: (value: number) => void
}) {
  return (
    <>
      <div className="overlay-row">
        <div>
          <div className="t">Hiện vùng AI đánh dấu</div>
          <div className="s">Mặc định TẮT</div>
        </div>
        <button
          type="button"
          role="switch"
          aria-checked={on}
          aria-label="Hiện vùng AI đánh dấu"
          className={`toggle ${on ? 'on' : ''}`}
          onClick={onToggle}
        >
          <span className="knob" />
        </button>
      </div>

      {on && (
        <>
          <div className="overlay-preview">
            <div className="cap">Minh hoạ vị trí overlay — OHIF tự vẽ mask DICOM SEG thật ở đây</div>
            {findings.slice(0, 2).map((f, i) => (
              <div
                key={f.label}
                className="mask"
                style={{
                  width: 70 + i * 20,
                  height: 50,
                  left: 60 + i * 90,
                  top: 50 + i * 40,
                  background: `radial-gradient(circle, color-mix(in srgb, ${LEGEND_COLORS[i % LEGEND_COLORS.length]} 65%, transparent), transparent 70%)`,
                }}
              />
            ))}
          </div>

          <div className="opacity-row">
            <span className="lbl">Độ trong suốt</span>
            <span className="val">{opacity}%</span>
          </div>
          <div className="range-track">
            <div className="range-fill" style={{ width: `${opacity}%` }} />
            <div className="range-knob" style={{ left: `${opacity}%` }} />
            <input
              className="range-input"
              type="range"
              min={0}
              max={100}
              value={opacity}
              aria-label="Độ trong suốt overlay"
              onChange={e => onOpacityChange(Number(e.target.value))}
            />
          </div>

          <div className="legend">
            {findings.map((f, i) => (
              <div className="legend-item" key={f.label}>
                <span className="legend-dot" style={{ background: LEGEND_COLORS[i % LEGEND_COLORS.length] }} />
                {f.label}
              </div>
            ))}
          </div>
        </>
      )}
    </>
  )
}
