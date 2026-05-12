# msa-auth-service

Troica Market Service의 **인증 / JWT 발급·검증** 마이크로서비스 (Phase 4 D1 결정으로 신규 추가).

> Single source of truth: [TROICA_SPEC.md](https://github.com/KTCloud-CloudNative-Troica-Team/msa-argocd-manifest/blob/main/TROICA_SPEC.md)

## 모듈 구조

```
msa-auth-service/
├── auth/            # 도메인 라이브러리 (JWT 발급/검증, Redis refresh-token 캐시)
└── auth-service/    # Spring Boot 앱 (gRPC AuthService.{SignUp, SignIn, CheckValidity})
```

## 포트 / 의존성

| 항목 | 값 |
|------|----|
| HTTP / Actuator | 8005 (SPEC §1.4) |
| gRPC | 9005 |
| PostgreSQL | `auth_db` (port 7007) |
| Redis | `auth-service-redis` (port 7006, refresh-token 캐시 — `RedisTemplate<String, String>`) |

## 외부 의존성 (GitHub Packages)

```kotlin
implementation("com.troica.msa:user:0.1.0")     // user 도메인 in-process 호출 (CreateUserCommand 등)
implementation("com.troica.msa:common:0.3.0")   // JPA/QueryDSL config, base 엔티티
```

JitPack client-redis는 미사용 (auth는 `RedisTemplate` 직접 사용).

## gRPC 엔드포인트

```protobuf
service AuthService {
  rpc SignUp (SignUpRequest) returns (Empty);
  rpc SignIn (SignInRequest) returns (SignInResponse);
  rpc CheckValidity (CheckValidityRequest) returns (UserResponseDto);
}
```

api-gateway의 `JwtHeaderCheckFilter`가 `CheckValidity`를 호출해서 JWT 검증.

## JWT secret

`application.yaml`의 `jwt.secret`은 환경변수 `JWT_SECRET`으로 override. PoC default값은 모노레포 코드 그대로 (시연용). **Phase 0 후 ExternalSecrets로 정식 운영** (BACKLOG R-25).

## 빌드 + 실행

```bash
./gradlew build -x test

docker build \
  --build-arg GPR_USER=$GITHUB_ACTOR \
  --build-arg GPR_TOKEN=$GITHUB_TOKEN \
  -t msa/auth-service:local .

docker run --rm -p 8005:8005 -p 9005:9005 \
  -e SPRING_PROFILES_ACTIVE=dev \
  -e JWT_SECRET=$(openssl rand -base64 48) \
  msa/auth-service:local
```

## CI

- PR + push: `build-test`
- Push to main + `vars.AWS_DEPLOYMENTS_ENABLED == 'true'`: ECR push + manifest update (Phase 0 후)
