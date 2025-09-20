# Telegram Bot with Google Sheets Integration

Этот проект демонстрирует мои навыки разработки на Kotlin: создание Telegram-бота, приём заявок от пользователей и автоматическая запись данных в Google Sheets.

## 🚀 Возможности
- Регистрация мастеров (имя, район, услуга)
- Приём заявок от клиентов через Telegram
- Автоматическая запись заявок в Google Sheets с помощью Google API
- Уведомления мастерам о новых заявках
- Минимальная админ-панель в чате для управления

## 🛠 Стек технологий
- Kotlin
- [Telegram Bot API](https://core.telegram.org/bots/api) (через библиотеку `java-telegram-bot-api`)
- Google Sheets API (v4)
- Jackson (JSON)
- Dotenv для конфигурации

## ⚙️ Настройка и запуск
1. Установите JDK 17 и Gradle.
2. Создайте бота в [BotFather](https://t.me/botfather) и получите TELEGRAM_TOKEN.
3. Настройте проект в Google Cloud Console:
   - Создайте проект и включите Google Sheets API.
   - Скачайте credentials.json (OAuth 2.0 Client).
   - Создайте Google Sheet и скопируйте его ID.
4. В корне проекта создайте файл .env:
   ```env
   TELEGRAM_TOKEN=ваш_токен
   GOOGLE_SHEET_ID=ваш_id_таблицы
