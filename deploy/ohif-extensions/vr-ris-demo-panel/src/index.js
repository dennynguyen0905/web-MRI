import React from 'react';
import { renderAiPanel, bindAiPanel } from './aiPanelCore';
import { injectAiPanelStylesOnce } from './aiPanelStyles';
import { MockAiPanelDataSource } from './aiPanelDataSource';
import { draftToFields, sendDraftApply, sendDraftDismiss, listenForAck } from './risBridge';

// AI Panel that cho OHIF (tab thu 3 "AI" o cot phai, canh Segmentation/Measurements) - port tu
// ai-panel.html (goc repo), xem khoi comment dau file do de biet nguon Figma + spec day du
// (doc/11-ai-design.md muc 4-7). Markup/CSS/event la DOM thuan (aiPanelCore.js, aiPanelStyles.js),
// KHONG dung hook React - tranh loi "Invalid hook call" giua ban React cua OHIF va ban React ma
// chinh extension nay tu dong goi rieng (da gap va sua o ban demo truoc, xem doc/07-pacs-dicom.md
// muc 6.4). Component React duoi day CHI la lop vo mong de OHIF mount/unmount dung cho; toan bo UI
// that render qua renderAiPanel()/bindAiPanel() vao 1 div thuan.

function getStudyInstanceUidFromUrl() {
  // URL dang "/viewer/<uid>" (xac nhan 2026-11-09, xem doc/07-pacs-dicom.md muc 6.1) - uid la segment
  // ngay sau "/viewer/".
  const m = window.location.pathname.match(/\/viewer\/([^/]+)/);
  return m ? decodeURIComponent(m[1]) : null;
}

// STUB: RIS chua co endpoint ghi audit cho cac hanh dong AI (doc/11-ai-design.md muc 6 - 7 gia tri
// audit_log.action moi cho AI la DE XUAT, chua co trong DB/migration that). Khong tu them endpoint/
// migration o day - chi log ro de nhom thay duoc moi lan su kien nay xay ra.
function onAuditEvent(name, payload) {
  // eslint-disable-next-line no-console
  console.info('[ai-panel][audit-stub] ' + name, payload);
}

class AiPanelHost extends React.Component {
  constructor(props) {
    super(props);
    this.containerRef = null;
    this.dataSource = new MockAiPanelDataSource();
    this.studyInstanceUID = getStudyInstanceUidFromUrl();
    this.setRef = (el) => {
      this.containerRef = el;
    };
  }

  componentDidMount() {
    injectAiPanelStylesOnce(document);
    this.load();
    this.unlistenAck = listenForAck((ack) => {
      if (ack.ok) {
        // eslint-disable-next-line no-console
        console.info('[ai-panel] RIS da dien gop y vao form bao cao', { studyInstanceUID: ack.studyInstanceUID });
      } else {
        // eslint-disable-next-line no-console
        console.warn('[ai-panel] RIS tu choi gop y AI', { studyInstanceUID: ack.studyInstanceUID, reason: ack.reason });
      }
    });
  }

  componentWillUnmount() {
    if (this.unlistenAck) this.unlistenAck();
  }

  load() {
    this.dataSource.getState(this.studyInstanceUID).then((state) => {
      if (!this.containerRef) return;
      // doc/11-ai-design.md muc 5.2.6: mo ca voi READY -> panel o dang thu gon, chua tu bung.
      if (state.status === 'READY') state.collapsed = true;
      renderAiPanel(this.containerRef, state);
      bindAiPanel(this.containerRef, (name, detail) => this.handleEvent(name, detail));
    });
  }

  handleEvent(name, detail) {
    switch (name) {
      case 'accept':
      case 'edit': {
        // "Dung goi y nay" / "Sua truoc khi dung" -> chep draft vao form bao cao chinh (ReportModal)
        // qua postMessage AI_DRAFT_APPLY, CHUA luu (luu luon o form chinh de server tinh
        // report.origin - quy tac cung, khong tu them nut Luu o day). Can RIS mo OHIF bang
        // window.open KHONG "noopener" moi co window.opener de gui nguoc - xem risBridge.js va
        // bao cao cuoi cua task noi kenh nay.
        const fields = draftToFields(detail && detail.draft);
        const mode = name === 'accept' ? 'use' : 'edit';
        const sent = sendDraftApply(mode, this.studyInstanceUID, fields);
        if (!sent) {
          // eslint-disable-next-line no-console
          console.warn('[ai-panel] Khong gui duoc AI_DRAFT_APPLY (thieu RIS_ORIGIN, window.opener, hoac studyInstanceUID).');
        }
        // Khong ghi noi dung nhap day du vao audit (doc/11-ai-design.md muc 6.2) - chi khoa + so luong.
        onAuditEvent(name, { studyInstanceUID: this.studyInstanceUID, fieldKeys: Object.keys(fields), sent });
        break;
      }
      case 'dismiss':
        sendDraftDismiss(this.studyInstanceUID);
        onAuditEvent(name, detail);
        break;
      case 'refresh':
        this.load();
        break;
      default:
        // toggle-collapse, suggestion-viewed, overlay-toggle, opacity-change
        onAuditEvent(name, detail);
        break;
    }
  }

  render() {
    return React.createElement('div', { className: 'ai-panel-root', ref: this.setRef });
  }
}

function getPanelModule() {
  return {
    // "defaultContext" la BAT BUOC trong ban OHIF nay - thieu field nay lam crash CA toolbar chinh,
    // khong chi panel moi (phat hien va ghi lai o doc/07-pacs-dicom.md muc 6.4).
    defaultContext: ['VIEWER'],
    menuOptions: [
      {
        icon: 'list',
        label: 'AI',
        target: 'vr-ris-ai-panel',
      },
    ],
    components: [
      {
        id: 'vr-ris-ai-panel',
        component: AiPanelHost,
      },
    ],
  };
}

function preRegistration() {
  // eslint-disable-next-line no-console
  console.log('[vr-ris-ai-panel] extension da nap thanh cong');
}

window.VrRisAiPanelExtension = {
  id: 'vr-ris-ai-panel',
  preRegistration,
  getPanelModule,
};
