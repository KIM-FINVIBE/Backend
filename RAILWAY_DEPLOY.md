# Railway Deploy Guide

## 1. Services

Railway 프로젝트에서 아래 이름으로 서비스를 만듭니다.

- `mariadb`
- `mongodb`
- `redis`
- `kafka`
- `backend`

`backend`는 GitHub repo `mjcapstone1/-KIM-back-`를 연결합니다. 이 repo에는 `railway.json`이 있어서 Railway가 `Dockerfile`로 빌드합니다.

## 2. Database Services

각 DB 서비스의 Variables와 Volume은 [deploy/railway.services.env.md](deploy/railway.services.env.md)를 그대로 참고하세요.

비밀번호는 특수문자 없이 만드는 것이 안전합니다.

```bash
openssl rand -hex 32
```

## 3. Backend Variables

`backend` 서비스의 Variables에는 [deploy/railway.backend.env.example](deploy/railway.backend.env.example)을 복사해서 넣습니다.

필수로 바꿀 값:

- `JWT_SECRET_KEY`
- `DB_PASSWORD`
- `MONGODB_URI`
- `SPRING_MONGODB_URI`
- `SPRING_DATA_MONGODB_URI`
- `CORS_ALLOWED_ORIGINS`
- `OPENROUTER_SITE_URL`

`JWT_SECRET_KEY`는 이렇게 만듭니다.

```bash
openssl rand -hex 64
```

처음 배포에서는 아래 외부 API를 꺼둔 상태로 시작하세요.

```env
KIS_ENABLED=false
FINVIBE_MARKET_LIVE_KIS_ON_REQUEST=false
NAVER_NEWS_ENABLED=false
OPENROUTER_ENABLED=false
FINVIBE_MARKET_BOOTSTRAP_PRICES_RUN_ON_STARTUP=false
FINVIBE_MARKET_CANDLE_BACKFILL_RUN_ON_STARTUP=false
```

백엔드가 정상 기동한 뒤 하나씩 `true`로 켜는 편이 장애 원인을 찾기 쉽습니다.

`FINVIBE_MARKET_LIVE_KIS_ON_REQUEST=false`는 프론트 요청마다 KIS를 직접 호출하지 않고 DB에 저장된 가격/차트만 응답하게 합니다. KIS 수집은 배치가 천천히 채우도록 두는 것이 Railway에서 더 안전합니다.

## 4. Public Domain

`backend` 서비스에서 Public Domain을 생성합니다.

Port는 `8080`입니다.

생성된 주소 예:

```text
https://backend-production-xxxx.up.railway.app
```

확인 URL:

```text
https://backend-production-xxxx.up.railway.app/actuator/health/liveness
https://backend-production-xxxx.up.railway.app/actuator/health
https://backend-production-xxxx.up.railway.app/docs
```

Railway 배포 healthcheck는 `/actuator/health/liveness`를 사용합니다. `/actuator/health`는 DB, Redis, MongoDB 같은 외부 의존성이 아직 준비되지 않았을 때 503이 날 수 있습니다.

## 5. Frontend Connection

Vercel 프론트에는 아래 환경변수를 넣습니다.

```env
VITE_USE_MOCKS=false
VITE_API_BASE_URL=https://backend-production-xxxx.up.railway.app
```

프론트 주소가 생기면 Railway `backend`의 CORS도 다시 맞춥니다.

```env
CORS_ALLOWED_ORIGINS=https://your-front.vercel.app
OPENROUTER_SITE_URL=https://your-front.vercel.app
```

## 6. Final Custom Domain

나중에 `finvibe.kr`을 붙이면 추천 구조는 아래입니다.

```text
Frontend: https://finvibe.kr, https://www.finvibe.kr
Backend:  https://api.finvibe.kr
```

이때 백엔드 Variables:

```env
CORS_ALLOWED_ORIGINS=https://finvibe.kr,https://www.finvibe.kr
OPENROUTER_SITE_URL=https://finvibe.kr
```

프론트 Variables:

```env
VITE_USE_MOCKS=false
VITE_API_BASE_URL=https://api.finvibe.kr
```
