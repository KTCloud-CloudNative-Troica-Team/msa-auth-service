# msa-auth-service

Troica Market Service의 **인증 / JWT 발급·검증** 마이크로서비스. gRPC `AuthService.{SignUp, SignIn, CheckValidity}`.

> SPEC + ADR ([ADR-0001](https://github.com/KTCloud-CloudNative-Troica-Team/msa-argocd-manifest/blob/main/docs/adr/0001-polyrepo-with-auth-service.md)): [msa-argocd-manifest/docs](https://github.com/KTCloud-CloudNative-Troica-Team/msa-argocd-manifest/tree/main/docs)
> 트러블슈팅: [TROUBLESHOOTING.md](https://github.com/KTCloud-CloudNative-Troica-Team/msa-argocd-manifest/blob/main/docs/TROUBLESHOOTING.md)

---

## 빠른 시작 (L2 — 로컬 docker로 끝까지 실행)

### 사전 요구사항

| 항목 | 버전 |
|---|---|
| Java | 21 (Temurin) |
| Docker | 24+ |
| GitHub PAT | `read:packages` (common-libs + user 라이브러리 받기) |

### 1. GH Packages 인증 (1회)

`~/.gradle/gradle.properties`:
```
gpr.user=<github-username>
gpr.token=<PAT-with-read:packages>
```

### 2. PostgreSQL + Redis 컨테이너 띄우기

```bash
docker run -d --name pg-auth \
  -p 7007:5432 \
  -e POSTGRES_USER=auth-service \
  -e POSTGRES_PASSWORD=auth-service \
  -e POSTGRES_DB=auth_db \
  postgres:18-alpine

docker run -d --name redis-auth \
  -p 7006:6379 \
  redis:8.6-alpine \
  redis-server --requirepass auth-service
```

### 3. 빌드 + 테스트

```bash
./gradlew build
```

### 4. 로컬 실행

```bash
./gradlew :auth-service:bootRun \
  --args='--spring.profiles.active=dev' \
  -DJWT_SECRET=local-dev-secret-replace-me
```

기대:
```
Started AuthServiceApplicationKt in X seconds
Tomcat started on port 8005 (http)
gRPC server started on port 9005
```

### 5. 검증

```bash
# Health
curl -s http://localhost:8005/healthz | jq

# gRPC service 목록
grpcurl -plaintext localhost:9005 list
# dev.ktcloud.black.auth.service.AuthService

# CheckValidity 호출 (token 발급 후)
grpcurl -plaintext -d '{"username":"test","password":"test"}' \
  localhost:9005 dev.ktcloud.black.auth.service.AuthService/SignIn
```

### 6. Docker로 실행

```bash
./gradlew :auth-service:bootJar
docker build -t msa/auth-service:local .

docker run --rm \
  -p 8005:8005 -p 9005:9005 \
  -e SPRING_PROFILES_ACTIVE=dev \
  -e AUTH_DB_HOST=host.docker.internal \
  -e REDIS_HOST=host.docker.internal \
  -e JWT_SECRET=local-dev-secret-replace-me \
  msa/auth-service:local
```

### 7. 정리

```bash
docker rm -f pg-auth redis-auth
```

---

## 모듈 구조

```
msa-auth-service/
├── auth/                # 도메인 라이브러리 — JWT 발급/검증 로직, user 도메인 호출
│                        # com.troica.msa:user (in-process 의존, ADR-0001)
└── auth-service/        # Spring Boot 앱 — gRPC controller + main
```

---

## 포트

| 프로토콜 | 포트 | 용도 |
|---------|------|------|
| HTTP | 8005 | Actuator (`/healthz`, `/prometheus`) |
| gRPC | 9005 | `AuthService` (api-gateway가 `CheckValidity` 호출) |

---

## 환경 변수

| 변수 | 기본값 | 설명 |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | (none) | `dev` / `prod` |
| `AUTH_DB_HOST` | localhost | PostgreSQL host |
| `AUTH_DB_PORT` | 7007 | PostgreSQL port |
| `AUTH_DB_NAME` | auth_db | DB name |
| `AUTH_DB_USERNAME` | auth-service | DB user |
| `AUTH_DB_PASSWORD` | auth-service | DB password |
| `REDIS_HOST` | localhost | Redis host (token 저장소) |
| `REDIS_PORT` | 7006 | Redis port |
| `REDIS_PASSWORD` | auth-service | Redis password |
| **`JWT_SECRET`** ⚠️ | `DEV_ONLY_INSECURE_REPLACE_ME_WITH_REAL_SECRET` | **반드시 override 필요** (Phase 5에서 ExternalSecrets 정식 운영) |
| `SERVER_PORT` | 8005 | HTTP listen |
| `GRPC_SERVER_PORT` | 9005 | gRPC listen |

---

## 외부 의존성

| 의존 | 용도 | 로컬 실행 시 |
|------|------|-------------|
| PostgreSQL `auth_db` | user 정보 (auth 라이브러리가 user 도메인 호출) | `postgres:18-alpine` 컨테이너 |
| Redis | 토큰 저장 + blacklist | `redis:8.6-alpine` 컨테이너 |
| `com.troica.msa:common:0.3.1` | JPA/QueryDSL config, 공통 예외 | GH Packages 자동 |
| `com.troica.msa:user:0.1.0` | **in-process** user 도메인 (회원가입 시 호출) | GH Packages 자동 (msa-user-service 레포에서 publish) |

**Kafka 미사용.**

---

## JWT secret 운영 (R-25)

⚠️ **현재 fallback은 placeholder**:
```yaml
secret: ${JWT_SECRET:DEV_ONLY_INSECURE_REPLACE_ME_WITH_REAL_SECRET}
```

운영 시:
- **로컬 개발**: `-DJWT_SECRET=...` 또는 환경변수
- **Phase 5**: ExternalSecrets Operator + AWS Secrets Manager — Kubernetes Secret으로 주입
- fallback이 실제 사용되면 명백히 인지 가능 (운영 보안 게이트 가능)

---

## CI/CD

`.github/workflows/ci.yml`:
- PR: `build-test`
- Push to main: build → Docker → Trivy → ECR push → manifest auto-bump PR

빌드 시간: ~2-3분 (R-27 (a) 적용).

---

## 트러블슈팅

- **JWT_SECRET 미주입 시** → fallback placeholder가 사용됨. 의도된 안전 신호 (운영 거부 가능)
- **`@Configuration class may not be final`** → common-libs 0.3.1+ 사용 ([TROUBLESHOOTING §1.7](https://github.com/KTCloud-CloudNative-Troica-Team/msa-argocd-manifest/blob/main/docs/TROUBLESHOOTING.md#17-kotlin-configuration-class가-final--spring-cglib-proxy-실패-r-38))
- **Redis connection refused** → `docker ps | grep redis-auth` 확인 + `requirepass` 일치
- **PostgreSQL connection refused** → `docker ps | grep pg-auth` 확인

---

## 관련 문서

- [msa-argocd-manifest](https://github.com/KTCloud-CloudNative-Troica-Team/msa-argocd-manifest) — `applications/values/auth-service/`
- [ADR-0001](https://github.com/KTCloud-CloudNative-Troica-Team/msa-argocd-manifest/blob/main/docs/adr/0001-polyrepo-with-auth-service.md) — auth-service 분리 결정
- [msa-user-service](https://github.com/KTCloud-CloudNative-Troica-Team/msa-user-service) — `user` 라이브러리 source
- [msa-api-gateway](https://github.com/KTCloud-CloudNative-Troica-Team/msa-api-gateway) — `CheckValidity` 호출자
- [TROUBLESHOOTING.md](https://github.com/KTCloud-CloudNative-Troica-Team/msa-argocd-manifest/blob/main/docs/TROUBLESHOOTING.md)
