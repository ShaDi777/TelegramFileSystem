# TelegramFileSystem

Распределённая файловая система, использующая Telegram как бэкенд для хранения данных.

## Архитектура проекта

Проект состоит из двух основных компонентов:

### 1. Backend ([README](backend/README.md))
Серверная часть на Spring Boot, предоставляющая REST API для операций с файлами и реализующая логику хранения данных в Telegram.

**Основные возможности:**
- REST API для CRUD операций с файлами и директориями
- Интеграция с Telegram через TDLib (Tdlight)
- Поддержка локального хранилища (LocalStorageService)
- Поддержка Telegram-хранилища (TelegramStorageService)
- Управление метаданными файлов (FileInfo, NodeInfo, NodeAttributes)
- Обработка исключений и валидация запросов

**Технологии:**
- Java 21
- Spring Boot 3.2.5
- TDLib (Tdlight 3.4.0)
- Lombok
- Maven

### 2. Client Side ([README](clientside/README.md))
Клиентская часть, реализующая FUSE-файловую систему для монтирования удалённого хранилища как локальной файловой системы.

**Основные возможности:**
- FUSE-интеграция через jnr-fuse
- Полная поддержка файловых операций (чтение, запись, создание, удаление, переименование)
- Синхронизация с backend через REST API
- Кэширование файловых операций
- Поддержка bash-скриптов для запуска

**Технологии:**
- Java 21
- Spring Boot 3.2.5
- jnr-fuse 0.5.7
- Gradle (Kotlin DSL)

## Структура проекта

```
TelegramFileSystem/
├── backend/                    # Серверная часть
│   ├── src/main/java/ru/tgfs/backend/
│   │   ├── configurations/     # Конфигурация приложения
│   │   ├── controllers/        # REST контроллеры
│   │   ├── exceptions/         # Обработка исключений
│   │   ├── models/             # Модели данных
│   │   ├── services/           # Бизнес-логика
│   │   │   ├── storage/        # Сервисы хранения
│   │   │   └── tdlight/        # Интеграция с Telegram
│   │   └── TelegramFsBackendApplication.java
│   └── pom.xml
│
├── clientside/                 # Клиентская часть (FUSE)
│   ├── bash/                   # Bash-скрипты запуска
│   ├── src/main/java/ru/tgfs/
│   │   ├── filesystem/         # FUSE реализация
│   │   ├── model/              # Модели данных
│   │   ├── service/            # Сервисы клиента
│   │   └── TgfsApplication.java
│   └── build.gradle.kts
│
└── README.md
```

## Ключевые компоненты

### Backend
- **FileUploadController** — REST API для загрузки и управления файлами
- **StorageService** — интерфейс сервисов хранения
- **LocalStorageService** — локальное хранилище файлов
- **TelegramStorageService** — хранилище в Telegram
- **TdlightInitializer** — инициализация TDLib клиента
- **PinMessageUtils** — утилиты для закреплённых сообщений

### Client Side
- **TelegramFS** — основная FUSE реализация
- **FileSystemService** — интерфейс файловой системы
- **TelegramFileSystemService** — клиентский сервис для работы с backend

## Взаимодействие компонентов

```
┌─────────────┐      REST API      ┌─────────────┐
│   FUSE FS   │ ◄────────────────► │   Backend   │
│ (clientside)│                    │  (Spring)   │
└─────────────┘                    └──────┬──────┘
                                          │
                                          ▼
                                   ┌─────────────┐
                                   │   Telegram  │
                                   │   (TDLib)   │
                                   └─────────────┘
```
