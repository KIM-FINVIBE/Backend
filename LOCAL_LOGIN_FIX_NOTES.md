# 로컬 프론트 로그인 연동 수정 내역

## 적용한 수정

1. `src/main/resources/application.yml`
   - Docker 컨테이너에서 MongoDB를 `localhost`로 보던 문제를 수정했습니다.
   - 이제 `MONGODB_URI` 환경변수를 사용합니다.

2. `src/main/resources/db/migration/V17__seed_frontend_test_user.sql`
   - 프론트 로그인 버튼에서 사용하는 테스트 계정을 Flyway migration으로 추가했습니다.
   - 기존 Docker DB volume이 있어도 새 migration으로 계정이 들어갑니다.

## 로컬 테스트 계정

- email: `student@universion.local`
- password: `finvest1234!`

## 실행

```bash
docker compose up -d --build app
```

이전 DB를 완전히 초기화하고 싶으면 아래 명령을 사용하세요.

```bash
docker compose down -v
docker compose up -d --build app
```

## 로그인 확인

```bash
curl -i -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"student@universion.local","password":"finvest1234!"}'
```
