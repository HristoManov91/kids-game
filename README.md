# Kids Game

Full-stack детско образователно приложение с тестове, класации, справки и албум с награди. Приложението е локално подготвено, но вече има основните настройки и проверки за публичен deploy.

## Стек

- Backend: Java 25, Spring Boot 4.0.6, Spring Security, Spring Data JPA, Flyway
- Database: PostgreSQL 18
- Frontend: Vue 3, Vite 8, Pinia, Vue Router, TypeScript
- Tests: JUnit 5, Mockito, Spring Security Test, Vue typecheck/build

## Локално Стартиране

1. Стартирай PostgreSQL:

```bash
docker compose up -d
```

2. Стартирай backend-а:

```bash
cd backend
mvn spring-boot:run
```

3. Стартирай frontend-а:

```bash
cd frontend
npm install
npm run dev
```

Отвори [http://localhost:5173](http://localhost:5173).

## Локални Demo Данни

По подразбиране backend-ът seed-ва demo акаунт и примерни резултати за локална разработка.

- Demo login: `mila` / `mila123`
- За да покажеш бързия demo бутон във frontend-а, стартирай с `VITE_SHOW_DEMO_LOGINS=true`.
- За production задай `APP_DEMO_SEED_ENABLED=false`.

## Environment

Backend:

```bash
DB_URL=jdbc:postgresql://localhost:5432/kids_game
DB_USERNAME=kids
DB_PASSWORD=kids
APP_JWT_SECRET=change-this-to-a-long-random-secret
APP_TOKEN_TTL_HOURS=12
APP_ADMIN_USERNAMES=христо
APP_CORS_ALLOWED_ORIGINS=http://localhost:5173
APP_DEMO_SEED_ENABLED=true
```

Frontend:

```bash
VITE_API_BASE_URL=http://localhost:8080/api
VITE_SHOW_DEMO_LOGINS=false
```

Production минимум:

- смени `APP_JWT_SECRET`
- задай `APP_DEMO_SEED_ENABLED=false`
- задай `APP_CORS_ALLOWED_ORIGINS` към публичния frontend URL
- задай силна `DB_PASSWORD`
- не използвай локалните demo credentials

## Тестове И Проверки

Backend:

```bash
cd backend
mvn test
```

Покрива:

- JWT валидиране, tampering и expiry
- authenticated/anonymous достъп
- admin endpoint защита
- CORS allow/deny поведение
- reward album business правила: чужда тема, премахнат каталогов артикул, неплатени предмети, refund и geometry clamp
- генератор и reward scoring регресии

Frontend:

```bash
cd frontend
npm run build
```

Командата пуска Vue TypeScript проверка и production Vite build.

## Основни Функции

- Регистрация и вход с JWT
- Тестове по математика, български и логика
- Класации по категория, тип и ниво
- История и справки за резултати
- Докладване на проблем в конкретен въпрос
- Албум с награди и теми: горска полянка, космос, морско дъно, ферма, джунгла
- Admin изгледи за мониторинг, каталог и reward catalog

## Deploy Checklist

1. Създай PostgreSQL база при избрания hosting provider.
2. Deploy-ни backend-а като Java/Spring service.
3. Deploy-ни frontend-а като static site.
4. Настрой backend env променливите от секция `Environment`.
5. Настрой frontend `VITE_API_BASE_URL` към публичния backend `/api`.
6. Добави публичния frontend домейн в `APP_CORS_ALLOWED_ORIGINS`.
7. Пусни `mvn test` и `npm run build` преди release.

## Полезни Команди

```bash
docker compose up -d
cd backend && mvn test
cd frontend && npm run build
docker compose down
```
