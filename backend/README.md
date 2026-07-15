# TelegramFS Backend

Серверная часть распределённой файловой системы TelegramFS.

## Назначение

Backend предоставляет REST API для операций с файлами и реализует логику хранения данных в Telegram через TDLib.

## Основные компоненты

### Контроллеры
- **FileUploadController** — REST endpoints для загрузки, скачивания, создания, удаления и переименования файлов

### Модели данных
- **FileInfo** — информация о файле
- **NodeInfo** — информация об узле файловой системы
- **NodeAttributes** — атрибуты узла (размер, время модификации, права доступа)
- **Privileges** — права доступа (owner, group, others)
- **FileUpdate** — запрос на обновление файла
- **RenameRequest** — запрос на переименование
- **TruncateRequest** — запрос на обрезку файла
- **ResponseMessage** — стандартный ответ API

### Сервисы хранения
- **StorageService** — интерфейс сервиса хранения
- **LocalStorageService** — реализация локального хранилища
- **TelegramStorageService** — реализация хранилища в Telegram

### Интеграция с Telegram
- **TdlightInitializer** — инициализация и настройка TDLib клиента
- **PinMessageUtils** — утилиты для работы с закреплёнными сообщениями
- **TgfsApplication** — основной класс приложения TDLib

### Исключения
- **StorageException** — базовое исключение хранилища
- **StorageFileNotFoundException** — файл не найден
- **StorageNameAlreadyExistsException** — имя уже существует

### Конфигурация
- **TelegramConfig** — конфигурация Telegram подключения

## Технологии

- Java 21
- Spring Boot 3.2.5
- Spring Web
- TDLib (Tdlight 3.4.0)
- Lombok
- Maven

## Сборка

```bash
mvn clean install
```

## Запуск

```bash
mvn spring-boot:run
```
