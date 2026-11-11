// Kenh postMessage OHIF (AI Panel) <-> RIS (ReportModal) - doc/11-ai-design.md muc 4, 5.2.5.d;
// doc/07-pacs-dicom.md muc 6 (deep-link, window.open/window.opener). Thiet ke day du o bao cao
// cuoi task "Noi kenh OHIF AI Panel <-> ReportModal bang postMessage" (xem lich su commit).
//
// Nguyen tac cung:
//   - KHONG BAO GIO dung targetOrigin "*".
//   - RIS_ORIGIN lay tu cau hinh (window.VR_RIS_CONFIG.risOrigin, dat trong app-config.js moi
//     moi truong - xem deploy/docker/ohif-app-config.js), KHONG hard-code trong file nay.
//   - Kiem event.source === window.opener truoc khi tin bat ky message nao (chong iframe/tab khac
//     gia origin dung - du origin dung, source sai van la nguon la).
//   - Gioi han do dai moi truong (5000 ky tu), chi nhan chuoi thuan, chi nhan dung 5 key da biet.

export const AI_DRAFT_SCHEMA_VERSION = 1;
export const ALLOWED_DRAFT_KEYS = ['technique', 'findings', 'diagnosis', 'conclusion', 'recommendation'];
const MAX_FIELD_LENGTH = 5000;

export function getRisOrigin() {
  const cfg = window.VR_RIS_CONFIG;
  const origin = cfg && cfg.risOrigin;
  return typeof origin === 'string' && origin ? origin : null;
}

function sanitizeFields(raw) {
  const out = {};
  if (!raw || typeof raw !== 'object') return out;
  for (const key of ALLOWED_DRAFT_KEYS) {
    const v = raw[key];
    if (typeof v === 'string' && v.length > 0 && v.length <= MAX_FIELD_LENGTH) out[key] = v;
  }
  return out;
}

/**
 * Chuyen "draft" cua AI Panel (mang {key,title,text}, xem aiPanelDataSource.js) sang "fields"
 * cho AI_DRAFT_APPLY. Anh xa theo "key", KHONG theo "title" hien thi (yeu cau cung của task).
 */
export function draftToFields(draft) {
  const out = {};
  (draft || []).forEach((item) => {
    if (item && typeof item.key === 'string' && typeof item.text === 'string') {
      out[item.key] = item.text;
    }
  });
  return sanitizeFields(out);
}

function canSend() {
  const origin = getRisOrigin();
  if (!origin) {
    // eslint-disable-next-line no-console
    console.warn('[ai-panel][ris-bridge] Chua cau hinh RIS_ORIGIN (window.VR_RIS_CONFIG.risOrigin) - khong gui duoc.');
    return null;
  }
  if (!window.opener) {
    // eslint-disable-next-line no-console
    console.warn('[ai-panel][ris-bridge] window.opener rong - OHIF khong duoc RIS mo bang window.open (hoac bi noopener chan).');
    return null;
  }
  return origin;
}

/** OHIF -> RIS: AI_DRAFT_APPLY. Tra true neu da goi postMessage (khong dam bao RIS nhan/dong y). */
export function sendDraftApply(mode, studyInstanceUID, rawFields) {
  const origin = canSend();
  if (!origin || !studyInstanceUID) return false;
  const fields = sanitizeFields(rawFields);
  if (Object.keys(fields).length === 0) return false;
  window.opener.postMessage(
    { v: AI_DRAFT_SCHEMA_VERSION, type: 'AI_DRAFT_APPLY', mode, studyInstanceUID, fields },
    origin,
  );
  return true;
}

/** OHIF -> RIS: AI_DRAFT_DISMISS (RIS chi ghi nhan cuc bo, khong goi API - doc/11 muc 4.2). */
export function sendDraftDismiss(studyInstanceUID) {
  const origin = canSend();
  if (!origin || !studyInstanceUID) return false;
  window.opener.postMessage(
    { v: AI_DRAFT_SCHEMA_VERSION, type: 'AI_DRAFT_DISMISS', studyInstanceUID },
    origin,
  );
  return true;
}

/**
 * Dang ky lang nghe AI_DRAFT_ACK tu RIS. Chi tin message co event.origin == RIS_ORIGIN cau hinh
 * VA event.source === window.opener; sai thi im lang bo qua (console.debug), KHONG bao loi to.
 * Tra ve ham huy dang ky.
 */
export function listenForAck(onAck) {
  const handler = (event) => {
    const origin = getRisOrigin();
    if (!origin || event.origin !== origin) {
      // eslint-disable-next-line no-console
      console.debug('[ai-panel][ris-bridge] bo message tu origin la', event.origin);
      return;
    }
    if (!window.opener || event.source !== window.opener) {
      // eslint-disable-next-line no-console
      console.debug('[ai-panel][ris-bridge] bo message tu window la (khong phai window.opener)');
      return;
    }
    const data = event.data;
    if (!data || typeof data !== 'object') return;
    if (data.v !== AI_DRAFT_SCHEMA_VERSION || data.type !== 'AI_DRAFT_ACK') return;
    if (typeof data.studyInstanceUID !== 'string' || typeof data.ok !== 'boolean') return;
    onAck(data);
  };
  window.addEventListener('message', handler);
  return () => window.removeEventListener('message', handler);
}
