// Lop du lieu (adapter) cho AI Panel - interface on dinh, cho AI Backend that cam vao sau nay (H-04).
// KHONG goi API that o day. Du lieu mau chep nguyen van chuoi tieng Viet tu ai-panel.html (goc Figma).
//
// interface AiPanelDataSource {
//   getState(studyInstanceUID: string): Promise<AiPanelState>
//   refresh(studyInstanceUID: string): Promise<AiPanelState>
// }

const AI_PANEL_SAMPLE = {
  NOT_TRIGGERED: {
    status: 'NOT_TRIGGERED', collapsed: false,
    message: 'Chưa có kết quả AI cho ca này.',
    hint: 'Loại ảnh này chưa được AI hỗ trợ cho ca này.',
  },
  PENDING: {
    status: 'PENDING', collapsed: false,
    pendingText: 'Đang phân tích ảnh (ước tính còn vài phút)',
  },
  READY: {
    status: 'READY', collapsed: false,
    metaText: 'MedAI-ChestCT v1.2 · hoàn tất 09/08/2026 23:02',
    overlay: {
      on: false, opacity: 65,
      legend: [
        { label: 'Nốt mờ thuỳ trên phổi (P)', color: 'var(--ai-overlay-coral)' },
        { label: 'Tràn dịch màng phổi (T)', color: 'var(--ai-overlay-amber)' },
      ],
    },
    findings: [
      { label: 'Nốt mờ thuỳ trên phổi (P), ~8mm', level: 'high' },
      { label: 'Dày thành phế quản hai bên', level: 'medium' },
      { label: 'Tràn dịch màng phổi (T), lượng ít', level: 'low' },
    ],
    // "key" la truong them cho nhiem vu noi AI Panel <-> ReportModal (xem risBridge.js): dung de
    // anh xa nhap nhap sang dung truong form bao cao (technique/findings/diagnosis/conclusion/
    // recommendation - dung ten o ReportDraft phia RIS), KHONG anh xa theo "title" hien thi (title
    // la tieng Viet, co the doi UI copy ma khong lam vo mapping).
    draft: [
      { key: 'technique', title: 'Kỹ thuật', text: 'Chụp X-quang ngực thẳng, tư thế đứng, hít sâu.' },
      { key: 'findings', title: 'Mô tả', text: 'Nốt mờ ranh giới rõ, đường kính khoảng 8mm ở thuỳ trên phổi phải. Thành phế quản hai bên dày nhẹ. Góc sườn hoành (T) hơi tù, nghi tràn dịch màng phổi lượng ít.' },
      { key: 'conclusion', title: 'Kết luận', text: 'Nốt mờ thuỳ trên phổi (P) cần theo dõi thêm. Nghi tràn dịch màng phổi (T) mức độ ít.' },
    ],
    warning: 'Đây là gợi ý AI, không phải chẩn đoán chính thức. AI chỉ đánh dấu một số vùng nghi ngờ, không thay thế việc đọc toàn bộ ảnh. Bác sĩ chịu trách nhiệm xác nhận.',
    dismissed: false,
  },
  FAILED: {
    status: 'FAILED', collapsed: false,
    metaText: 'MedAI-ChestCT v1.2 · 09/08/2026 23:05',
    message: 'AI không trả được kết quả cho ca này.',
    hint: 'Vui lòng đọc ảnh như bình thường.',
  },
  TIMEOUT: {
    status: 'TIMEOUT', collapsed: false,
    metaText: 'MedAI-ChestCT v1.2 · hết hạn sau 120s',
    message: 'AI không phản hồi cho ca này.',
    hint: 'Vui lòng đọc ảnh như bình thường.',
  },
};

const VALID_STATUSES = Object.keys(AI_PANEL_SAMPLE);

function clone(o) {
  return JSON.parse(JSON.stringify(o));
}

/**
 * Doc trang thai mong muon tu query string cua chinh trang OHIF, vd:
 *   /viewer/1.2.3...?aiStatus=FAILED
 * Khong co hoac gia tri khong hop le -> mac dinh READY (de demo khong can them tham so).
 */
function readRequestedStatus() {
  try {
    const params = new URLSearchParams(window.location.search);
    const v = (params.get('aiStatus') || '').toUpperCase();
    return VALID_STATUSES.includes(v) ? v : 'READY';
  } catch {
    return 'READY';
  }
}

export class MockAiPanelDataSource {
  // eslint-disable-next-line no-unused-vars
  async getState(studyInstanceUID) {
    const status = readRequestedStatus();
    return clone(AI_PANEL_SAMPLE[status]);
  }

  // eslint-disable-next-line no-unused-vars
  async refresh(studyInstanceUID) {
    // Mock: "lam moi" chi tra lai dung trang thai hien tai (khong doi du lieu) - that hon la random.
    return this.getState(studyInstanceUID);
  }
}
