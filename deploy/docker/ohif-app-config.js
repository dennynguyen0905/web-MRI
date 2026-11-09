window.config = {
  routerBasename: '/',
  extensions: [],
  showStudyList: true,
  servers: {
    dicomWeb: [
      {
        name: 'VRRIS_PACS',
        // Di qua "orthanc-cors-proxy" (cong 8044), KHONG goi thang Orthanc (8042): Orthanc khong
        // tu them header CORS, trinh duyet se chan QIDO-RS/WADO-RS neu goi thang (xem comment o
        // deploy/compose/docker-compose.full.yml, service "orthanc-cors-proxy").
        qidoRoot: 'http://localhost:8044/dicom-web',
        wadoRoot: 'http://localhost:8044/dicom-web',
        wadoUriRoot: 'http://localhost:8044/dicom-web',
        qidoSupportsIncludeField: true,
        imageRendering: 'wadors',
        thumbnailRendering: 'wadors',
        supportsFuzzyMatching: true,
      },
    ],
  },
};

// Nap "vr-ris-demo-panel" - extension OHIF demo chung minh "cach nhe" tuy bien OHIF (khong fork
// source, khong tu build lai image ohif/viewer) ma van sua duoc UI (them panel/toolbar...). Co
// che nay dung dung mau co san trong chinh index.html cua anh ohif/viewer:v4.12.51.21579 (xem khoi
// comment "<!-- EXTENSIONS -->" trong index.html goc, da kiem bang tay 2026-11-09): nap 1 the
// <script> ngoai dinh nghia 1 bien global, roi them bien do vao window.config.extensions. Vi sao
// khong dat truc tiep trong index.html: anh nay khong cho sua index.html rieng (chi app-config.js
// la file duoc mount/volume vao), nen dung document.write de tu "chen" script ma khong phai thay
// ca index.html (an toan hon khi image cap nhat ban moi). Source + cach build lai: xem
// deploy/ohif-extensions/vr-ris-demo-panel/ va doc/07-pacs-dicom.md.
document.write('<script src="/extensions/vr-ris-demo-panel.bundle.js"><' + '/script>');
document.write(
  '<script>window.config.extensions = (window.config.extensions || []).concat([window.VrRisDemoPanelExtension]);<' +
    '/script>'
);
