# Безплатен Deploy

Този проект има backend, frontend и PostgreSQL база. Затова чистият безплатен вариант е да разделим нещата:

- Hugging Face Docker Space за приложението
- Neon Free PostgreSQL за базата

## 1. Създай Безплатна PostgreSQL База

Използвай Neon Free. Free планът е $0 и е описан като без кредитна карта.

Създай project и вземи pooled PostgreSQL connection string.

Connection string-ът обикновено изглежда така:

```bash
postgresql://USER:PASSWORD@HOST/DATABASE?sslmode=require
```

Не го записвай в Git.

## 2. Създай Hugging Face Space

Отвори:

[https://huggingface.co/new-space](https://huggingface.co/new-space)

Настройки:

- Space SDK: Docker
- Hardware: free CPU
- App port: `7860`

В Settings -> Secrets добави:

```bash
DB_URL=postgresql://USER:PASSWORD@HOST/DATABASE?sslmode=require
APP_JWT_SECRET=replace-with-a-long-random-secret
APP_DEMO_SEED_ENABLED=false
APP_TOKEN_TTL_HOURS=336
```

## 3. Качи Кода В Space-а

Hugging Face Space е Git repo. След като създадеш Space-а, можеш да го clone-неш и да push-неш този проект към него. Най-удобно е през token от Hugging Face Settings -> Access Tokens.

## 4. След Deploy

Отвори публичния Hugging Face Space URL. При чиста база няма готов потребител, така че първо създай акаунт от регистрацията.

Free плановете са подходящи за проба и споделяне с приятели. Ако приложението започне да се използва постоянно, очаквай ограничения като sleep/pause или лимити на база/трафик.
