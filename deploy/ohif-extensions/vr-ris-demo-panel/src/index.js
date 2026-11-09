import React from 'react';

// QUAN TRONG: dung class component, KHONG dung hook (useState/useEffect...).
// Ly do: extension nay tu dong goi rieng mot ban "react"/"react-dom" cua no (vi anh ohif/viewer
// khong expose window.React de dung chung voi app chinh - da kiem tra). Khi OHIF (ban react/
// react-dom cua NO) render component cua extension, hook se doc "dispatcher hien tai" tu ban
// react CUA EXTENSION (rieng, chua bao gio duoc OHIF gan dispatcher vao) -> loi "Invalid hook
// call". Class component khong bi anh huong vi lifecycle/setState di qua "instance.updater" duoc
// OHIF (chu khong phai ban react cua extension) tu gan luc mount, khong phu thuoc ban react nao.
class DemoPanel extends React.Component {
  render() {
    return React.createElement(
      'div',
      { style: { padding: '12px', color: '#e2e8f0', fontSize: '13px', lineHeight: 1.5 } },
      React.createElement('h3', { style: { marginTop: 0 } }, 'AI Panel (demo)'),
      React.createElement(
        'p',
        null,
        'Panel nay duoc nap vao OHIF qua extension ngoai (khong sua source OHIF), chi de chung minh ' +
          'co che "tuy bien nhe" hoat dong duoc. Chua noi AI thuc su - xem doc/07-pacs-dicom.md muc ' +
          've extension nay de biet buoc tiep theo.'
      )
    );
  }
}

// Dung dung hinh dang ma ban OHIF nay thuc su doc (kiem bang cach doc code render cua Header trong
// chinh bundle da minify - "g.c.modules[PANEL].forEach(i => i.module.components.forEach(...))"):
// module phai co "components: [{id, component}]"; "menuOptions" dung de hien icon tren toolbar.
function getPanelModule() {
  return {
    // "defaultContext" la BAT BUOC (khong phai optional nhu doc gioi thieu): Toolbar cua ban OHIF
    // nay goi thang "Array.from(module.defaultContext)" khong kiem tra null truoc - thieu field nay
    // lam crash CA ToolbarRow (loi "undefined is not iterable", phat hien bang cach doc code render
    // da minify cua no, xem doc/07-pacs-dicom.md). Gia tri ["VIEWER"] lay theo cach cac panel co san
    // trong chinh anh nay tu khai bao (vd panel do/nhan do - xem app.bundle, tu khoa "defaultContext").
    defaultContext: ['VIEWER'],
    menuOptions: [
      {
        icon: 'list',
        label: 'AI Panel (demo)',
        target: 'vr-ris-demo-panel',
      },
    ],
    components: [
      {
        id: 'vr-ris-demo-panel',
        component: DemoPanel,
      },
    ],
  };
}

function preRegistration() {
  // eslint-disable-next-line no-console
  console.log('[vr-ris-demo-panel] extension da nap thanh cong');
}

window.VrRisDemoPanelExtension = {
  id: 'vr-ris-demo-panel',
  preRegistration,
  getPanelModule,
};
