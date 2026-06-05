# Kids Game

Детско приложение за упражнения по математика, български и логика. Идеята е проста: детето решава кратки тестове, печели кристали и с тях си прави собствени картинки в албум с награди.

Проектът е писан като истинско full-stack приложение, не като еднократна демо страница. Има регистрация, вход, JWT auth, PostgreSQL база, Flyway миграции, справки, класации, admin екрани и тестове за важните части.

## Как Изглежда

### Вход

![Екран за вход](docs/screenshots/login.png)

### Избор на тест

![Избор на тест](docs/screenshots/dashboard.png)

### Албум с награди

![Албум с награди](docs/screenshots/rewards.png)

## Какво Може

- упражнения по математика, български и логика
- различни типове задачи и 10 нива на трудност
- резултат, оценка, време и кристали след тест
- албум с награди: горска полянка, космос, морско дъно, ферма и джунгла
- справки за последни опити и напредък
- докладване на проблем в задача
- admin екрани за преглед и корекции

## Технологии

Backend:

- Java 25
- Spring Boot 4
- Spring Security
- Spring Data JPA
- Flyway
- PostgreSQL

Frontend:

- Vue 3
- Vite
- Pinia
- Vue Router
- TypeScript

## Локално Пускане

Първо стартирай базата:

```bash
docker compose up -d
```

После backend-а:

```bash
cd backend
mvn spring-boot:run
```

И frontend-а:

```bash
cd frontend
npm install
npm run dev
```

Отвори [http://localhost:5173](http://localhost:5173).

При чиста база няма готов потребител. Създай акаунт от екрана за регистрация и започни оттам.

## Настройки

Backend променливи:

```bash
DB_URL=jdbc:postgresql://localhost:5432/kids_game
DB_USERNAME=kids
DB_PASSWORD=kids
APP_JWT_SECRET=change-this-to-a-long-random-secret
APP_TOKEN_TTL_HOURS=12
APP_ADMIN_USERNAMES=христо
APP_CORS_ALLOWED_ORIGINS=http://localhost:5173
APP_DEMO_SEED_ENABLED=false
```

Frontend променливи:

```bash
VITE_API_BASE_URL=http://localhost:8080/api
VITE_SHOW_DEMO_LOGINS=false
```

За публична среда най-важните неща са:

- смени `APP_JWT_SECRET` със силна стойност
- не включвай `APP_DEMO_SEED_ENABLED`
- сложи реалния frontend домейн в `APP_CORS_ALLOWED_ORIGINS`
- използвай силна парола за PostgreSQL
- пази `.env` файловете извън Git

## Проверки

Backend тестове:

```bash
cd backend
mvn test
```

Frontend build:

```bash
cd frontend
npm run build
```

В backend тестовете има проверки за JWT, CORS, protected/admin endpoints и бизнес правилата около албума с награди. Frontend build-ът минава през TypeScript проверка и production Vite build.

## Deploy

Няма Render или Koyeb deploy конфигурация в repo-то, за да не те води към платен план или карта по невнимание.

[Hugging Face Spaces](https://huggingface.co/new-space) е по-подходящият безплатен вариант за приложението, защото Docker Spaces могат да стартират custom web app без кредитна карта. За базата използвай [Neon Free](https://neon.com/pricing), защото Free планът им е $0 и е описан като без кредитна карта.

Безплатният вариант, който има най-много смисъл за този проект:

- Hugging Face Docker Space за приложението
- Neon Free PostgreSQL за базата

В Hugging Face Space настрой `sdk: docker`, порт `7860`, и добави тези Secrets:

| Key | Value |
| --- | --- |
| `DB_URL` | connection string от Neon |
| `APP_JWT_SECRET` | дълга случайна стойност |
| `APP_DEMO_SEED_ENABLED` | `false` |
| `APP_TOKEN_TTL_HOURS` | `12` |

Backend-ът приема и `jdbc:postgresql://...`, и нормален hosted Postgres URL от типа `postgresql://...`.

Приложението пак логически има две части:

- backend service: Spring Boot + PostgreSQL
- frontend static site: Vite build, който говори с backend-а през `/api`

За production настройките горе са задължителни, особено `APP_JWT_SECRET` и изключения demo seed. При single-service deploy frontend-ът използва относителен `/api`, така че няма нужда да гониш два различни публични URL-а.
