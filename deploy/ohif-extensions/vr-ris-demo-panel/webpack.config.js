const path = require('path');

module.exports = {
  entry: './src/index.js',
  target: 'web',
  output: {
    path: path.resolve(__dirname, 'dist'),
    filename: 'vr-ris-demo-panel.bundle.js',
    // Khong dung output.library: anh ohif/viewer nay KHONG expose window.React/window.ReactDOM
    // (da kiem tra truc tiep trong bundle, xem doc/07-pacs-dicom.md) nen extension phai dong goi
    // theo kieu "chay va tu gan bien global" (side-effect), khong phai UMD module chuan.
  },
  module: {
    rules: [
      {
        test: /\.jsx?$/,
        exclude: /node_modules/,
        use: {
          loader: 'babel-loader',
          options: {
            presets: ['@babel/preset-env', '@babel/preset-react'],
          },
        },
      },
    ],
  },
  resolve: {
    extensions: ['.js', '.jsx'],
  },
};
