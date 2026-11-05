# VR-RIS PACS - ris-backend (Spring Boot). Xem doc/14-devops-trien-khai.md muc 3.
#
# BUILD CONTEXT PHAI LA GOC REPO (khong phai Backend/), vi stage build can COPY ca Backend/ va
# database/migrations/ (maven-resources-plugin trong Backend/pom.xml tham chieu ../database/migrations
# tu basedir Backend/ - xem doc/14-devops-trien-khai.md muc 5 va Backend/pom.xml):
#
#   docker build -f deploy/docker/backend.Dockerfile -t vrris/ris-backend:dev .
#
# (chay tu goc repo, cung cap "." la context - KHONG chay "docker build ." trong Backend/)

# ---------- Stage 1: build (Maven + JDK 21) ----------
# maven:3.9.9-eclipse-temurin-21 - ban chinh thuc cua Maven kem san JDK 21 (khop java.version=21 o
# Backend/pom.xml). Da pull va xac nhan ton tai tren Docker Hub trong phien lam viec nay.
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /build

# database/migrations truoc khi copy Backend/ de tang cache: migration doi it hon pom.xml/src.
COPY database/migrations ./database/migrations

# pom.xml truoc src/ de tang layer cache dependency (chi tai lai khi pom.xml doi).
COPY Backend/pom.xml ./Backend/pom.xml
RUN mvn -f Backend/pom.xml -B -q dependency:go-offline || true

COPY Backend/src ./Backend/src

# -DskipTests: test can MySQL thuc qua Testcontainers (R-05 quyet dinh 4, doc/15-kien-truc-backend.md
# muc 1) - khong chay duoc trong lop build cua image (khong co Docker-trong-Docker o day). Test da
# chay o CI (.github/workflows/backend-ci.yml) va "mvn verify" tren may dev truoc khi build image -
# xem quy trinh trien khai o doc/14-devops-trien-khai.md muc 8.
RUN mvn -f Backend/pom.xml -B -q -DskipTests clean package

# ---------- Stage 2: runtime (JRE, khong Maven/JDK) ----------
# eclipse-temurin:21-jre-alpine - chi co JRE (khong co compiler/Maven) -> image nho hon nhieu so
# voi dung lai image build. Alpine (musl) thay vi Debian slim: nho hon, du cho mot jar Spring Boot
# thuan (khong dung thu vien native can glibc). Da pull va xac nhan ton tai tren Docker Hub.
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Chay bang user thuong, khong phai root (khong co ly do can root: chi chay 1 jar, khong bind port <1024).
RUN addgroup -S app && adduser -S app -G app
COPY --from=build /build/Backend/target/ris-backend-*.jar /app/app.jar
RUN chown app:app /app/app.jar
USER app

EXPOSE 8080

# Khong dat HEALTHCHECK o day: k8s dung readiness/liveness probe rieng vao
# /api/v1/actuator/health/{readiness,liveness} (deploy/k8s/22-backend-deployment.yaml) - dat ca hai
# noi se trung lap va de lech nhau khi doi mot ben. Chi dung cho "docker run" don le (deploy/compose/).
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
