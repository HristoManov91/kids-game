---
title: Kids Game
emoji: 🧮
colorFrom: green
colorTo: blue
sdk: docker
app_port: 7860
pinned: false
license: mit
---

# Kids Game

Kids Game е детско образователно приложение за упражнения по математика, български език и логика. Направих го с идеята ученето да не изглежда като сух тест, а като малка игра: детето решава задачи, събира кристали и после ги използва, за да отключва картинки и да подрежда собствени албуми.

Приложението може да се пробва тук:

[https://manoraev-kids-game.hf.space/](https://manoraev-kids-game.hf.space/)

## Идеята

Исках да събера няколко неща в едно приложение:

- кратки тестове, които не изморяват детето
- ясна обратна връзка след всеки тест
- награди, които мотивират без да пречат на ученето
- албуми с картинки, които детето само подрежда
- справки, с които родител или учител може да види напредъка
- админ панел за преглед, корекции и поддръжка на съдържанието

Проектът не е само frontend демо. Има реален backend, база данни, регистрация, вход, роли, защитени endpoints, миграции и тестове.

## Екрани

### Вход

![Екран за вход](docs/screenshots/login.png)

### Избор на тест

![Избор на тест](docs/screenshots/dashboard.png)

### Албум с награди

![Албум с награди](docs/screenshots/rewards.png)

## Какво Може Приложението

- регистрация и вход с потребителски акаунт
- тестове по математика, български език и логика
- различни подкатегории и 10 нива на трудност
- автоматично генериране на задачи
- резултат, оценка, време и спечелени кристали след тест
- албум с награди с различни теми: гора, космос, море, ферма и джунгла
- покупка и подреждане на картинки в албум
- лична справка за решени тестове
- админ справки по детски профили
- докладване на проблем в задача
- админ екрани за каталог, награди и преглед на опити

## Технологии

Backend:

- Java 25
- Spring Boot 4
- Spring Security
- JWT authentication
- Spring Data JPA
- Flyway migrations
- PostgreSQL
- Maven

Frontend:

- Vue 3
- TypeScript
- Vite
- Pinia
- Vue Router
- Lucide icons
- CSS без готов UI framework

Инфраструктура:

- Docker
- Hugging Face Spaces за публичния app
- PostgreSQL hosted база

## Архитектура

Приложението е разделено на две основни части:

- `backend` е Spring Boot API, което пази потребители, тестове, резултати, кристали, албуми и админ операции.
- `frontend` е Vue приложение, което се build-ва със Vite и говори с backend-а през `/api`.

В production frontend-ът и backend-ът се сервират като един Docker app. Това улеснява deploy-а, защото няма нужда от два отделни публични URL-а и отделна CORS конфигурация между тях.

## Локално Пускане

Необходими са Java 25, Maven, Node.js/npm и Docker.

Стартиране на PostgreSQL:

```bash
docker compose up -d
```

Backend:

```bash
cd backend
mvn spring-boot:run
```

Frontend:

```bash
cd frontend
npm install
npm run dev
```

Локалният адрес е:

[http://localhost:5173](http://localhost:5173)

При чиста база няма готов потребител. Първият акаунт се създава от екрана за регистрация.

## Environment Променливи

Backend:

```bash
DB_URL=jdbc:postgresql://localhost:5432/kids_game
DB_USERNAME=kids
DB_PASSWORD=kids
APP_JWT_SECRET=change-this-to-a-long-random-secret
APP_TOKEN_TTL_HOURS=12
APP_ADMIN_USERNAME=
APP_ADMIN_PASSWORD=
APP_ADMIN_DISPLAY_NAME=Администратор
APP_CORS_ALLOWED_ORIGINS=http://localhost:5173
APP_DEMO_SEED_ENABLED=false
```

Frontend:

```bash
VITE_API_BASE_URL=http://localhost:8080/api
VITE_SHOW_DEMO_LOGINS=false
```

## Тестове И Проверки

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

В backend тестовете има проверки за JWT, CORS, protected/admin endpoints, генериране на задачи и бизнес логиката около наградите.

## Deploy

Публичната версия е качена като Docker Space в Hugging Face:

[https://manoraev-kids-game.hf.space/](https://manoraev-kids-game.hf.space/)

Repo-то съдържа нужната Hugging Face Space metadata в началото на този README:

```yaml
sdk: docker
app_port: 7860
```

Production deploy-ът очаква външна PostgreSQL база и следните secrets/environment променливи:

```bash
DB_URL=postgresql://USER:PASSWORD@HOST/DATABASE?sslmode=require
APP_JWT_SECRET=long-random-production-secret
APP_TOKEN_TTL_HOURS=12
APP_DEMO_SEED_ENABLED=false
APP_ADMIN_USERNAME=optional-admin-username
APP_ADMIN_PASSWORD=optional-strong-admin-password
APP_ADMIN_DISPLAY_NAME=Администратор
```

Backend-ът приема както `jdbc:postgresql://...`, така и hosted Postgres URL от типа `postgresql://...`.

Demo seed-ът е изключен в production. Ако трябва да се създаде admin акаунт автоматично, това става само чрез secrets `APP_ADMIN_USERNAME` и `APP_ADMIN_PASSWORD`, а не чрез стойности в кода.

## Структура

```text
backend/      Spring Boot API, security, database models, services and tests
frontend/     Vue 3 application, views, stores, components and assets
docs/         screenshots and deployment notes
Dockerfile    production image for the Hugging Face Space
```
