// Port thuan JS/DOM tu ai-panel.html (xem file do o goc repo de biet nguon Figma + spec day du).
// KHONG dung framework - renderAiPanel(root, state) ve lai toan bo markup tu 1 object state,
// bindAiPanel(root, onChange) gan event delegation 1 lan va phat CustomEvent "ai-panel:*".
// Day la ly do co che nay tranh duoc loi "Invalid hook call" giua 2 ban React (OHIF vs extension)
// da gap voi vr-ris-demo-panel ban dau: khong co React o day, chi la DOM thuan.

export const ICON = {
  chevronDown: '<svg class="ai-panel__chevron" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="m6 9 6 6 6-6"/></svg>',
  chevronRight: '<svg class="ai-panel__chevron" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="m9 18 6-6-6-6"/></svg>',
  check: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M20 6 9 17l-5-5"/></svg>',
  edit: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M12 3H5a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/><path d="M18.375 2.625a1 1 0 0 1 3 3l-9.013 9.014a2 2 0 0 1-.853.505l-2.873.84a.5.5 0 0 1-.62-.62l.84-2.873a2 2 0 0 1 .506-.852z"/></svg>',
  x: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M18 6 6 18"/><path d="m6 6 12 12"/></svg>',
  refresh: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M3 12a9 9 0 0 1 9-9 9.75 9.75 0 0 1 6.74 2.74L21 8"/><path d="M21 3v5h-5"/><path d="M21 12a9 9 0 0 1-9 9 9.75 9.75 0 0 1-6.74-2.74L3 16"/><path d="M8 16H3v5"/></svg>',
  spinner: '<svg class="ai-spinner" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M21 12a9 9 0 1 1-6.219-8.56"/></svg>',
};

export const BADGE_LABEL = {
  NOT_TRIGGERED: 'Chưa xử lý',
  PENDING: 'Đang phân tích…',
  READY: 'Có gợi ý AI',
  FAILED: 'AI lỗi',
  TIMEOUT: 'AI không phản hồi',
};
export const LEVEL_LABEL = { high: 'Cao', medium: 'Trung bình', low: 'Thấp' };

const esc = (s) => String(s).replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));

function renderReadyBody(s) {
  const o = s.overlay || { on: false, opacity: 65, legend: [] };

  if (s.dismissed) {
    return `
      <div class="ai-divider"></div>
      <p class="ai-text-muted">Đã bỏ qua gợi ý AI cho lượt đọc này.</p>
      <button class="ai-link" type="button" data-act="restore">Xem lại gợi ý AI</button>`;
  }

  return `
    <div class="ai-divider"></div>

    <section class="ai-block" aria-label="Vùng AI đánh dấu">
      <div class="ai-overlay-head">
        <div class="ai-overlay-head__label">
          <strong id="ai-ovl-label">Hiện vùng AI đánh dấu</strong>
          <span>Mặc định TẮT</span>
        </div>
        <button class="ai-toggle" type="button" role="switch" aria-labelledby="ai-ovl-label"
                aria-checked="${o.on}" data-act="overlay"></button>
      </div>

      <div ${o.on ? '' : 'hidden'} data-ovl-extra>
        <div class="ai-block">
          <div class="ai-overlay-preview">
            <p class="ai-overlay-preview__note">Minh hoạ: mask DICOM SEG chồng lên ảnh gốc (OHIF tự vẽ)</p>
            <span class="ai-mask ai-mask--1" style="opacity:${o.opacity / 100}"></span>
            <span class="ai-mask ai-mask--2" style="opacity:${o.opacity / 100}"></span>
          </div>
          <div class="ai-slider">
            <div class="ai-slider__row"><span>Độ trong suốt</span><span data-opacity-val>${o.opacity}%</span></div>
            <input type="range" min="0" max="100" value="${o.opacity}" style="--fill:${o.opacity}%"
                   aria-label="Độ trong suốt vùng AI đánh dấu" data-act="opacity">
          </div>
          <div class="ai-legend">
            ${(o.legend || []).map((l) => `
              <div class="ai-legend__item"><span class="ai-legend__dot" style="background:${l.color}"></span>${esc(l.label)}</div>`).join('')}
          </div>
        </div>
      </div>
    </section>

    <div class="ai-divider"></div>

    <section class="ai-block" aria-label="Nhãn AI đề xuất">
      <h3 class="ai-section-title">Nhãn AI đề xuất</h3>
      ${(s.findings || []).map((f) => `
        <div class="ai-finding">
          <p class="ai-finding__label">${esc(f.label)}</p>
          <span class="ai-conf" data-level="${f.level}">
            <span class="ai-conf__bars" aria-hidden="true"><i></i><i></i><i></i></span>${LEVEL_LABEL[f.level]}
          </span>
        </div>`).join('')}
    </section>

    <div class="ai-divider"></div>

    <section class="ai-block" style="gap:8px" aria-label="Nội dung nháp cho báo cáo">
      <div class="ai-draft-head">
        <h3 class="ai-section-title">Nội dung nháp cho báo cáo</h3>
        <p class="ai-text-muted">Xem trước — không phải ô nhập</p>
      </div>
      <div class="ai-draft">
        ${(s.draft || []).map((d) => `
          <div class="ai-draft__item"><h4>${esc(d.title)}</h4><p>${esc(d.text)}</p></div>`).join('')}
      </div>
    </section>

    <div class="ai-warning" role="note">
      <span class="ai-warning__icon" aria-hidden="true">!</span>
      <p>${esc(s.warning)}</p>
    </div>

    <div class="ai-actions">
      <button class="ai-btn ai-btn--primary" type="button" data-act="accept">${ICON.check}Dùng gợi ý này</button>
      <div class="ai-actions__row">
        <button class="ai-btn ai-btn--outline" type="button" data-act="edit">${ICON.edit}Sửa trước khi dùng</button>
        <button class="ai-btn ai-btn--ghost" type="button" data-act="dismiss">${ICON.x}Bỏ qua gợi ý</button>
      </div>
    </div>`;
}

export function renderAiPanel(root, s) {
  const collapsed = !!s.collapsed;
  root.dataset.status = s.status;
  root.dataset.collapsed = String(collapsed);
  root.__state = s;

  let body = '';
  if (collapsed) {
    body = s.status === 'READY' ? `<p class="ai-text-muted">Bấm để mở — khối gợi ý chưa tự bung</p>` : '';
  } else if (s.status === 'NOT_TRIGGERED') {
    body = `<p class="ai-text-body">${esc(s.message)}</p><p class="ai-text-muted">${esc(s.hint)}</p>`;
  } else if (s.status === 'PENDING') {
    body = `
      <div class="ai-pending">${ICON.spinner}<p class="ai-text-body" style="width:auto">${esc(s.pendingText)}</p></div>
      <button class="ai-btn ai-btn--ghost ai-btn--sm" type="button" data-act="refresh" style="flex:none">${ICON.refresh}Làm mới</button>`;
  } else if (s.status === 'READY') {
    body = `<p class="ai-text-muted">${esc(s.metaText)}</p>` + renderReadyBody(s);
  } else {
    // FAILED | TIMEOUT
    body = `<p class="ai-text-muted">${esc(s.metaText)}</p>
            <p class="ai-text-body">${esc(s.message)}</p>
            <p class="ai-text-muted">${esc(s.hint)}</p>`;
  }

  root.innerHTML = `
    <button class="ai-panel__header" type="button" data-act="collapse" aria-expanded="${!collapsed}">
      <span class="ai-panel__title">AI Panel</span>
      ${collapsed ? ICON.chevronRight : ICON.chevronDown}
    </button>
    <span class="ai-badge" data-status="${s.status}"><span class="ai-badge__dot"></span>${BADGE_LABEL[s.status]}</span>
    ${body}`;
}

/**
 * Gan event delegation 1 lan; panel phat CustomEvent cho host app.
 * onEmit(name, detail) duoc goi cho MOI su kien ai-panel:* (kem ca nhung cai renderAiPanel da tu xu ly
 * nhu toggle-collapse/overlay-toggle) de host (OHIF extension) tu quyet dinh lam gi tiep (audit, bridge...).
 */
export function bindAiPanel(root, onEmit) {
  const emit = (name, detail = {}) => {
    root.dispatchEvent(new CustomEvent('ai-panel:' + name, { bubbles: true, detail }));
    onEmit && onEmit(name, detail);
  };

  root.addEventListener('click', (e) => {
    const el = e.target.closest('[data-act]');
    if (!el || !root.contains(el)) return;
    const s = root.__state;
    switch (el.dataset.act) {
      case 'collapse':
        s.collapsed = !s.collapsed;
        emit('toggle-collapse', { collapsed: s.collapsed });
        if (!s.collapsed && s.status === 'READY' && !s.__viewed) {
          s.__viewed = true;
          emit('suggestion-viewed');
        }
        renderAiPanel(root, s);
        break;
      case 'overlay':
        s.overlay.on = !s.overlay.on;
        emit('overlay-toggle', { on: s.overlay.on });
        renderAiPanel(root, s);
        break;
      case 'refresh':
        emit('refresh');
        break;
      case 'accept':
        // Kem s.draft (co key) de host (index.js) dung lam fields cho AI_DRAFT_APPLY - xem risBridge.js.
        emit('accept', { draft: s.draft });
        break;
      case 'edit':
        emit('edit', { draft: s.draft });
        break;
      case 'dismiss':
        s.dismissed = true;
        emit('dismiss');
        renderAiPanel(root, s);
        break;
      case 'restore':
        s.dismissed = false;
        emit('suggestion-viewed');
        renderAiPanel(root, s);
        break;
      default:
        break;
    }
  });

  root.addEventListener('input', (e) => {
    if (e.target.dataset.act !== 'opacity') return;
    const v = +e.target.value;
    const s = root.__state;
    s.overlay.opacity = v;
    e.target.style.setProperty('--fill', v + '%');
    const valEl = root.querySelector('[data-opacity-val]');
    if (valEl) valEl.textContent = v + '%';
    root.querySelectorAll('.ai-mask').forEach((m) => (m.style.opacity = v / 100));
    emit('opacity-change', { value: v });
  });
}
