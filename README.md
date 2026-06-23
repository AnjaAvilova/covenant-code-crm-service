# CRM Service

## Общая информация

CRM Service — серверное приложение для управления данными CRM-системы, разработанное на Java с использованием Spring Boot. Проект предоставляет REST API для взаимодействия с клиентскими приложениями, поддерживает аутентификацию пользователей, работу с базой данных и автоматическое документирование API.

---

## Структура проекта

```text
src
├── main
│   ├── java
│   │   └── com.covenantcode
│   │       ├── controller
│   │       ├── service
│   │       ├── repository
│   │       ├── model
│   │       ├── dto
│   │       ├── mapper
│   │       ├── security
│   │       └── config
│   └── resources
│       ├── application.yml
│       └── db
│           └── migration
└── test
```

### Назначение основных пакетов

| Пакет      | Описание                        |
| ---------- | ------------------------------- |
| controller | REST-контроллеры                |
| service    | Бизнес-логика приложения        |
| repository | Работа с базой данных через JPA |
| model      | Сущности базы данных            |
| dto        | Объекты передачи данных         |
| mapper     | Преобразование DTO ↔ Entity     |
| security   | Настройки безопасности и JWT    |
| config     | Конфигурация приложения         |

---

## Используемые зависимости

### Spring Boot

* Spring Boot Starter Web
* Spring Boot Starter Data JPA
* Spring Boot Starter Security
* Spring Boot Starter Validation
* Spring Boot Starter Actuator

### База данных

* PostgreSQL
* Flyway Core
* Flyway PostgreSQL

### Безопасность

* JJWT (JSON Web Token)

### Документирование API

* SpringDoc OpenAPI
* Swagger UI

### Генерация кода

* Lombok
* MapStruct

### Тестирование

* Spring Boot Test
* Spring Security Test
* Testcontainers
* PostgreSQL Testcontainers

---

## Технологический стек

* Java 21
* Spring Boot 3.3.5
* Maven
* PostgreSQL
* Flyway
* JWT
* Swagger/OpenAPI
* Lombok
* MapStruct
* JUnit 5
* Testcontainers

---

## Сборка и запуск

Сборка проекта:

```bash
mvn clean package
```

Запуск приложения:

```bash
mvn spring-boot:run
```

или

```bash
java -jar target/crm-service-0.0.1-SNAPSHOT.jar
```
