import { describe, it, expect, vi, beforeEach } from 'vitest';
import { renderAiPanel, bindAiPanel } from './aiPanelCore';

function makeReadyState() {
  return {
    status: 'READY',
    collapsed: false,
    metaText: 'MedAI-ChestCT v1.2 · hoàn tất 09/08/2026 23:02',
    overlay: {
      on: false,
      opacity: 65,
      legend: [{ label: 'Nốt mờ thuỳ trên phổi (P)', color: 'var(--ai-overlay-coral)' }],
    },
    findings: [{ label: 'Nốt mờ thuỳ trên phổi (P), ~8mm', level: 'high' }],
    draft: [{ title: 'Kỹ thuật', text: 'Chụp X-quang ngực thẳng.' }],
    warning: 'Đây là gợi ý AI, không phải chẩn đoán chính thức.',
    dismissed: false,
  };
}

describe('renderAiPanel — render theo tung status', () => {
  let root;
  beforeEach(() => {
    root = document.createElement('div');
  });

  it('NOT_TRIGGERED: hien dung badge va message/hint, khong co nut hanh dong', () => {
    renderAiPanel(root, { status: 'NOT_TRIGGERED', collapsed: false, message: 'Chưa có kết quả AI cho ca này.', hint: 'Loại ảnh này chưa được AI hỗ trợ cho ca này.' });
    expect(root.dataset.status).toBe('NOT_TRIGGERED');
    expect(root.querySelector('.ai-badge').textContent).toContain('Chưa xử lý');
    expect(root.textContent).toContain('Chưa có kết quả AI cho ca này.');
    expect(root.querySelectorAll('[data-act="accept"]').length).toBe(0);
  });

  it('PENDING: hien spinner, pendingText va nut Lam moi', () => {
    renderAiPanel(root, { status: 'PENDING', collapsed: false, pendingText: 'Đang phân tích ảnh (ước tính còn vài phút)' });
    expect(root.querySelector('.ai-badge').textContent).toContain('Đang phân tích');
    expect(root.querySelector('.ai-spinner')).not.toBeNull();
    expect(root.querySelector('[data-act="refresh"]')).not.toBeNull();
  });

  it('READY (mo rong): hien du khoi overlay/findings/draft/warning va 3 nut hanh dong, KHONG co nut Luu', () => {
    renderAiPanel(root, makeReadyState());
    expect(root.querySelector('[data-act="overlay"]')).not.toBeNull();
    expect(root.querySelectorAll('.ai-finding').length).toBe(1);
    expect(root.querySelectorAll('.ai-draft__item').length).toBe(1);
    expect(root.querySelector('.ai-warning')).not.toBeNull();
    expect(root.querySelector('[data-act="accept"]')).not.toBeNull();
    expect(root.querySelector('[data-act="edit"]')).not.toBeNull();
    expect(root.querySelector('[data-act="dismiss"]')).not.toBeNull();
    expect(root.textContent).not.toContain('Lưu');
  });

  it('READY: muc do tin cay hien theo MUC chu khong hien so thap phan', () => {
    renderAiPanel(root, makeReadyState());
    const conf = root.querySelector('.ai-conf');
    expect(conf.textContent).toContain('Cao');
    expect(conf.textContent).not.toMatch(/\d+\.\d+/);
  });

  it('READY da dismiss: hien dong "da bo qua" + nut "Xem lai", AN toan bo khoi gop y', () => {
    const s = makeReadyState();
    s.dismissed = true;
    renderAiPanel(root, s);
    expect(root.textContent).toContain('Đã bỏ qua gợi ý AI');
    expect(root.querySelector('[data-act="restore"]')).not.toBeNull();
    expect(root.querySelector('[data-act="accept"]')).toBeNull();
  });

  it('FAILED va TIMEOUT: khong co nut "thu lai", khong hien ma loi ky thuat', () => {
    renderAiPanel(root, { status: 'FAILED', collapsed: false, metaText: 'x', message: 'AI không trả được kết quả cho ca này.', hint: 'Vui lòng đọc ảnh như bình thường.' });
    expect(root.querySelectorAll('[data-act="retry"]').length).toBe(0);
    expect(root.textContent).not.toMatch(/KHONG_LAY_DUOC_ANH|SUY_LUAN_LOI/);

    renderAiPanel(root, { status: 'TIMEOUT', collapsed: false, metaText: 'x', message: 'AI không phản hồi cho ca này.', hint: 'Vui lòng đọc ảnh như bình thường.' });
    expect(root.querySelectorAll('[data-act="retry"]').length).toBe(0);
  });

  it('thu gon (collapsed=true, status READY): chi hien dong goi mo, khong hien chi tiet', () => {
    const s = makeReadyState();
    s.collapsed = true;
    renderAiPanel(root, s);
    expect(root.dataset.collapsed).toBe('true');
    expect(root.textContent).toContain('Bấm để mở');
    expect(root.querySelector('[data-act="accept"]')).toBeNull();
  });
});

describe('bindAiPanel — cong tac overlay', () => {
  it('bam toggle overlay: doi aria-checked va hien/an slider+chu giai', () => {
    const root = document.createElement('div');
    const s = makeReadyState();
    renderAiPanel(root, s);
    bindAiPanel(root, () => {});

    const toggle = root.querySelector('[data-act="overlay"]');
    expect(toggle.getAttribute('aria-checked')).toBe('false');
    expect(root.querySelector('[data-ovl-extra]').hasAttribute('hidden')).toBe(true);

    toggle.click();

    const toggleAfter = root.querySelector('[data-act="overlay"]');
    expect(toggleAfter.getAttribute('aria-checked')).toBe('true');
    expect(root.querySelector('[data-ovl-extra]').hasAttribute('hidden')).toBe(false);
  });

  it('mac dinh overlay la TAT khi mo ca', () => {
    const root = document.createElement('div');
    renderAiPanel(root, makeReadyState());
    expect(root.querySelector('[data-act="overlay"]').getAttribute('aria-checked')).toBe('false');
  });
});

describe('bindAiPanel — thu gon/mo rong', () => {
  it('bam header: doi collapsed va aria-expanded', () => {
    const root = document.createElement('div');
    const s = makeReadyState();
    s.collapsed = true;
    renderAiPanel(root, s);
    bindAiPanel(root, () => {});

    expect(root.querySelector('.ai-panel__header').getAttribute('aria-expanded')).toBe('false');
    root.querySelector('[data-act="collapse"]').click();
    expect(root.dataset.collapsed).toBe('false');
    expect(root.querySelector('.ai-panel__header').getAttribute('aria-expanded')).toBe('true');
  });
});

describe('bindAiPanel — phat su kien', () => {
  let root;
  let onEmit;
  beforeEach(() => {
    root = document.createElement('div');
    renderAiPanel(root, makeReadyState());
    onEmit = vi.fn();
    bindAiPanel(root, onEmit);
  });

  it('mo panel lan dau (READY) phat toggle-collapse roi suggestion-viewed', () => {
    const collapsedRoot = document.createElement('div');
    const s = makeReadyState();
    s.collapsed = true;
    renderAiPanel(collapsedRoot, s);
    const emit = vi.fn();
    bindAiPanel(collapsedRoot, emit);

    collapsedRoot.querySelector('[data-act="collapse"]').click();

    expect(emit).toHaveBeenCalledWith('toggle-collapse', { collapsed: false });
    expect(emit).toHaveBeenCalledWith('suggestion-viewed', {});
  });

  it('phat overlay-toggle kem gia tri on', () => {
    root.querySelector('[data-act="overlay"]').click();
    expect(onEmit).toHaveBeenCalledWith('overlay-toggle', { on: true });
  });

  it('phat accept/edit kem draft (de map sang AI_DRAFT_APPLY.fields); dismiss khong kem draft', () => {
    const expectedDraft = makeReadyState().draft;
    root.querySelector('[data-act="accept"]').click();
    expect(onEmit).toHaveBeenCalledWith('accept', { draft: expectedDraft });

    root.querySelector('[data-act="edit"]').click();
    expect(onEmit).toHaveBeenCalledWith('edit', { draft: expectedDraft });

    root.querySelector('[data-act="dismiss"]').click();
    expect(onEmit).toHaveBeenCalledWith('dismiss', {});
  });

  it('phat refresh khi dang PENDING va bam Lam moi', () => {
    const pendingRoot = document.createElement('div');
    renderAiPanel(pendingRoot, { status: 'PENDING', collapsed: false, pendingText: 'x' });
    const emit = vi.fn();
    bindAiPanel(pendingRoot, emit);
    pendingRoot.querySelector('[data-act="refresh"]').click();
    expect(emit).toHaveBeenCalledWith('refresh', {});
  });

  it('cung mot CustomEvent "ai-panel:*" cung duoc bubble ra ngoai DOM (de host khac co the lang nghe)', () => {
    const domListener = vi.fn();
    root.addEventListener('ai-panel:accept', domListener);
    root.querySelector('[data-act="accept"]').click();
    expect(domListener).toHaveBeenCalledTimes(1);
  });
});
