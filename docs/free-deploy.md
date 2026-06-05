# Безплатен Deploy

Този проект има backend, frontend и PostgreSQL база. Затова чистият безплатен вариант е да разделим нещата:

- Koyeb free instance за приложението
- Supabase или Neon free PostgreSQL за базата

## 1. Създай Безплатна PostgreSQL База

Избери едно:

- Supabase Free: създай project и вземи PostgreSQL connection string
- Neon Free: създай project и вземи pooled PostgreSQL connection string

Connection string-ът обикновено изглежда така:

```bash
postgresql://USER:PASSWORD@HOST/DATABASE?sslmode=require
```

Не го записвай в Git.

## 2. Deploy В Koyeb

Отвори бутона:

[![Deploy to Koyeb](https://www.koyeb.com/static/images/deploy/button.svg)](https://app.koyeb.com/deploy?type=git&builder=dockerfile&repository=github.com/HristoManov91/kids-game&branch=main&name=kids-game&ports=8080;http;/)

В Koyeb избери free instance и добави тези environment променливи:

```bash
DB_URL=postgresql://USER:PASSWORD@HOST/DATABASE?sslmode=require
APP_JWT_SECRET=replace-with-a-long-random-secret
APP_DEMO_SEED_ENABLED=false
APP_TOKEN_TTL_HOURS=12
```

Остави порт `8080`.

## 3. След Deploy

Отвори публичния Koyeb URL. При чиста база няма готов потребител, така че първо създай акаунт от регистрацията.

Free плановете са подходящи за проба и споделяне с приятели. Ако приложението започне да се използва постоянно, очаквай ограничения като sleep/pause или лимити на база/трафик.
