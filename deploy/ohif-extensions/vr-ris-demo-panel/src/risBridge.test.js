import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import {
  draftToFields, sendDraftApply, sendDraftDismiss, listenForAck, getRisOrigin,
} from './risBridge';

function setRisOrigin(origin) {
  window.VR_RIS_CONFIG = origin ? { risOrigin: origin } : undefined;
}

describe('getRisOrigin / cau hinh', () => {
  afterEach(() => setRisOrigin(null));

  it('tra null khi chua cau hinh window.VR_RIS_CONFIG', () => {
    setRisOrigin(null);
    expect(getRisOrigin()).toBeNull();
  });

  it('tra dung gia tri da cau hinh, khong hard-code', () => {
    setRisOrigin('http://localhost:8080');
    expect(getRisOrigin()).toBe('http://localhost:8080');
  });
});

describe('draftToFields - anh xa theo key, khong theo title', () => {
  it('chi lay field co key hop le trong danh sach cho phep', () => {
    const fields = draftToFields([
      { key: 'technique', title: 'Kỹ thuật', text: 'A' },
      { key: 'findings', title: 'Mô tả khác tên nhưng vẫn map theo key', text: 'B' },
      { key: 'unknown_key', title: 'X', text: 'C' },
    ]);
    expect(fields).toEqual({ technique: 'A', findings: 'B' });
  });

  it('bo qua item thieu key/text hoac draft rong/null', () => {
    expect(draftToFields(null)).toEqual({});
    expect(draftToFields([])).toEqual({});
    expect(draftToFields([{ title: 'Kỹ thuật', text: 'A' }])).toEqual({});
  });

  it('cat bo chuoi qua 5000 ky tu (tu choi, khong cat ngan)', () => {
    const long = 'x'.repeat(5001);
    const fields = draftToFields([{ key: 'technique', text: long }]);
    expect(fields.technique).toBeUndefined();
  });

  it('chap nhan dung 5000 ky tu', () => {
    const ok = 'x'.repeat(5000);
    const fields = draftToFields([{ key: 'technique', text: ok }]);
    expect(fields.technique).toBe(ok);
  });
});

describe('sendDraftApply / sendDraftDismiss', () => {
  beforeEach(() => {
    setRisOrigin('http://localhost:8080');
    window.opener = { postMessage: vi.fn() };
  });
  afterEach(() => {
    setRisOrigin(null);
    window.opener = null;
  });

  it('khong gui duoc khi chua cau hinh RIS_ORIGIN', () => {
    setRisOrigin(null);
    const sent = sendDraftApply('use', '1.2.3', { technique: 'A' });
    expect(sent).toBe(false);
    expect(window.opener.postMessage).not.toHaveBeenCalled();
  });

  it('khong gui duoc khi window.opener rong (bi noopener chan)', () => {
    window.opener = null;
    const sent = sendDraftApply('use', '1.2.3', { technique: 'A' });
    expect(sent).toBe(false);
  });

  it('khong gui duoc khi khong con field hop le nao', () => {
    const sent = sendDraftApply('use', '1.2.3', { unknown: 'A' });
    expect(sent).toBe(false);
    expect(window.opener.postMessage).not.toHaveBeenCalled();
  });

  it('gui dung schema, dung targetOrigin tu cau hinh, KHONG bao gio la "*"', () => {
    const sent = sendDraftApply('edit', '1.2.3.4', { technique: 'A', findings: 'B' });
    expect(sent).toBe(true);
    expect(window.opener.postMessage).toHaveBeenCalledWith(
      { v: 1, type: 'AI_DRAFT_APPLY', mode: 'edit', studyInstanceUID: '1.2.3.4', fields: { technique: 'A', findings: 'B' } },
      'http://localhost:8080',
    );
  });

  it('sendDraftDismiss gui dung schema', () => {
    sendDraftDismiss('1.2.3.4');
    expect(window.opener.postMessage).toHaveBeenCalledWith(
      { v: 1, type: 'AI_DRAFT_DISMISS', studyInstanceUID: '1.2.3.4' },
      'http://localhost:8080',
    );
  });
});

describe('listenForAck', () => {
  let unlisten;
  beforeEach(() => {
    setRisOrigin('http://localhost:8080');
    window.opener = {};
  });
  afterEach(() => {
    if (unlisten) unlisten();
    setRisOrigin(null);
    window.opener = null;
  });

  function dispatch(data, origin, source) {
    const ev = new MessageEvent('message', { data, origin, source });
    window.dispatchEvent(ev);
  }

  it('bo qua message tu origin la (khong goi onAck)', () => {
    const onAck = vi.fn();
    unlisten = listenForAck(onAck);
    dispatch({ v: 1, type: 'AI_DRAFT_ACK', studyInstanceUID: 'x', ok: true }, 'http://evil.example', window.opener);
    expect(onAck).not.toHaveBeenCalled();
  });

  it('bo qua message dung origin nhung source la (khong phai window.opener)', () => {
    const onAck = vi.fn();
    unlisten = listenForAck(onAck);
    dispatch({ v: 1, type: 'AI_DRAFT_ACK', studyInstanceUID: 'x', ok: true }, 'http://localhost:8080', {});
    expect(onAck).not.toHaveBeenCalled();
  });

  it('bo qua message sai version hoac sai type', () => {
    const onAck = vi.fn();
    unlisten = listenForAck(onAck);
    dispatch({ v: 2, type: 'AI_DRAFT_ACK', studyInstanceUID: 'x', ok: true }, 'http://localhost:8080', window.opener);
    dispatch({ v: 1, type: 'SOMETHING_ELSE', studyInstanceUID: 'x', ok: true }, 'http://localhost:8080', window.opener);
    expect(onAck).not.toHaveBeenCalled();
  });

  it('nhan dung ACK tu dung origin + dung source', () => {
    const onAck = vi.fn();
    unlisten = listenForAck(onAck);
    const ack = { v: 1, type: 'AI_DRAFT_ACK', studyInstanceUID: 'x', ok: false, reason: 'STUDY_MISMATCH' };
    dispatch(ack, 'http://localhost:8080', window.opener);
    expect(onAck).toHaveBeenCalledWith(ack);
  });
});
