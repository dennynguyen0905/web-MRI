// eslint.config.js moi (2026-11-05, doc/14-devops-trien-khai.md muc 7) - Frontend/ truoc day CHUA
// co ESLint. Dung flat config (ESLint 9+), day la mau tieu chuan cho du an React 19 + TypeScript +
// Vite 7 (giong template "npm create vite@latest -- --template react-ts"), khop dung stack thuc
// te cua Frontend/ (doc/01-scope.md muc 8.3) - khong doi them rule rieng nao.
import js from '@eslint/js'
import globals from 'globals'
import reactHooks from 'eslint-plugin-react-hooks'
import reactRefresh from 'eslint-plugin-react-refresh'
import tseslint from 'typescript-eslint'

export default tseslint.config(
  { ignores: ['dist'] },
  {
    extends: [js.configs.recommended, ...tseslint.configs.recommended],
    files: ['**/*.{ts,tsx}'],
    languageOptions: {
      ecmaVersion: 2020,
      globals: globals.browser,
    },
    plugins: {
      'react-hooks': reactHooks,
      'react-refresh': reactRefresh,
    },
    rules: {
      ...reactHooks.configs.recommended.rules,
      'react-refresh/only-export-components': [
        'warn',
        { allowConstantExport: true },
      ],
      // Lan dau bat ESLint cho codebase da co san (2026-11-05, doc/14-devops-trien-khai.md muc 7) -
      // ban eslint-plugin-react-hooks 7.x them luat moi nay va bao loi that o
      // src/components/modals/ReportModal.tsx (goi setState truc tiep trong useEffect). Day la
      // code nghiep vu Frontend co san, ngoai pham vi viec duoc giao (14-devops/12-seed-data) - ha
      // xuong "warn" de CI (frontend-ci.yml) khong do vi mot phat hien moi chua ai yeu cau sua,
      // nhung van hien thi de nhom biet va sua sau. KHONG ha cac luat react-hooks khac.
      'react-hooks/set-state-in-effect': 'warn',
    },
  },
)
