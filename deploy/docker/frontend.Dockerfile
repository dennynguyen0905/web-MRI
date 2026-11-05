# VR-RIS PACS - ris-frontend (React 19 + Vite 7 + Tailwind 4). Xem doc/14-devops-trien-khai.md muc 3.
#
# BUILD CONTEXT PHAI LA GOC REPO (vi du goc repo dang nam trong mot thu muc tren may cung ten
# "Frontend" - xem README.md muc "Cau truc repo" - nen duong dan ben duoi la "Frontend/..." TINH TU
# GOC REPO, khong phai tu thu muc nay):
#
#   docker build -f deploy/docker/frontend.Dockerfile -t vrris/ris-frontend:dev .

# ---------- Stage 1: build (Node + Vite) ----------
# node:22-alpine - Node 22 la nhanh Active LTS tai thoi diem viet (2026-11), va la phien ban LTS
# THAP NHAT thoa dieu kien engine cua Vite 7.1 (^20.19.0 || >=22.12.0 - Node 20 khong du vi can
# dung ban vit 20.19 rat moi, con 22 thi moi LTS nao cung >=22.12). Khong dung Node 24 du may dev
# dang cai ban do (node -v = v24.16.0): 24 LTS hay khong tai thoi diem build anh huong kha nang tai
# ve lau dai cua image, 22 la lua chon an toan hon cho mot do an. Doi lai neu nhom xac nhan khac ❓.
FROM node:22-alpine AS build
WORKDIR /build

COPY Frontend/package.json Frontend/package-lock.json ./
# npm ci (khong npm install): dung dung ban trong package-lock.json, khong tu nang version - quan
# trong cho build lap lai duoc giua may dev va CI/image.
RUN npm ci

COPY Frontend/ ./
# "npm run build" = "tsc -b && vite build" (Frontend/package.json) -> xuat ra ./dist/.
RUN npm run build

# ---------- Stage 2: runtime (nginx phuc vu file tinh) ----------
FROM nginx:1.27-alpine
COPY --from=build /build/dist /usr/share/nginx/html
# Dung co che "template" co san cua image nginx chinh thuc (tu ban 1.19): moi file *.template trong
# /etc/nginx/templates/ duoc entrypoint goc chay envsubst roi ghi ra /etc/nginx/conf.d/ truoc khi
# nginx khoi dong - khong can viet entrypoint rieng. Nho vay MOT anh dung duoc ca cho k8s (hostname
# Service "ris-backend") va cho deploy/compose/docker-compose.full.yml (hostname service "backend"),
# chi can doi bien moi truong BACKEND_UPSTREAM, khong phai build lai anh.
COPY deploy/docker/frontend.nginx.conf.template /etc/nginx/templates/default.conf.template
ENV BACKEND_UPSTREAM=ris-backend:8080

EXPOSE 80

# Khong dat HEALTHCHECK o day - k8s readiness/liveness probe la mot httpGet "/" vao chinh Service nay
# (deploy/k8s/30-frontend-deployment.yaml); nginx tra 200 ngay khi con song nen khong can endpoint rieng.
