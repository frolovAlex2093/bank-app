# Bank App

Микросервисное приложение «Банк» на Spring Boot + Spring Cloud.
Позволяет клиенту банка редактировать данные аккаунта, класть и снимать деньги, переводить средства другим пользователям.

### Схема портов

| Сервис | Порт |
|---|---|
| Keycloak | 9090 |
| Eureka (Discovery) | 8761 |
| Config Server | 8888 |
| Gateway | 8080 |
| Accounts Service | 8081 |
| Cash Service | 8082 |
| Transfer Service | 8083 |
| Notifications Service | 8084 |
| Front UI | 8085 |

## Структура проекта

```
bank-root/
├── config-server/           # Spring Cloud Config Server
├── discovery-server/        # Eureka Server
├── gateway-service/         # Spring Cloud Gateway MVC
├── accounts-service/        # Аккаунты и балансы
├── cash-service/            # Пополнение и снятие наличных
├── transfer-service/        # Переводы между счетами
├── notifications-service/   # Уведомления 
├── bank-front-ui/           # Веб-интерфейс
├── docker-compose.yml
└── pom.xml                  # Parent POM
```

## Требования

- Java 21
- Maven 3.9+
- Docker и Docker Compose

## Настройка Keycloak

Перед первым запуском нужно настроить Keycloak вручную.

### 1. Запустить Keycloak и БД

```bash
docker compose up bank-db keycloak -d
```

Открыть http://localhost:9090, войти с логином `admin` / паролем `admin`.

### 2. Создать Realm

- Нажать «Create realm»
- Name: `bank-realm`
- Нажать «Create»

### 3. Создать клиентов

Перейти в **Clients → Create client**.

**bank-front-client** (для фронта, Authorization Code Flow):
- Client ID: `bank-front-client`
- Client authentication: ON
- Authentication flow: Standard flow (Authorization Code)
- Valid redirect URIs: `http://localhost:8085/login/oauth2/code/keycloak`
- Web origins: `http://localhost:8085`

**accounts-service** (Client Credentials):
- Client ID: `accounts-service`
- Client authentication: ON
- Authentication flow: снять все галочки, оставить только Service accounts roles

**cash-service** (Client Credentials):
- Client ID: `cash-service`
- Client authentication: ON
- Authentication flow: только Service accounts roles

**transfer-service** (Client Credentials):
- Client ID: `transfer-service`
- Client authentication: ON
- Authentication flow: только Service accounts roles

### 4. Создать scope internal_api

Перейти в **Client scopes → Create client scope**:
- Name: `internal_api`
- Type: Optional

Добавить этот scope клиентам `accounts-service`, `cash-service`, `transfer-service`.

### 5. Создать тестового пользователя

Перейти в **Users → Create new user**:
- Username: `ivan_ivanov`
- Email Verified: ON

Перейти на вкладку **Credentials → Set password**:
- Password: `password`
- Temporary: OFF

Убедиться, что в таблице `accounts.accounts` есть запись с `login = 'ivan_ivanov'`.
Если нет — вставить вручную:

```sql
INSERT INTO accounts (login, first_name, last_name, birth_date, balance)
VALUES ('ivan_ivanov', 'Иван', 'Иванов', '1990-01-01', 1000.00);
```

### 6. Получить client secrets

Для каждого клиента (`bank-front-client`, `accounts-service`, `cash-service`, `transfer-service`)
перейти в **Clients → [client] → Credentials → Client secret** и скопировать значение.

Вставить секреты в переменные окружения (или прямо в `application.yml` для локального запуска).

## Сборка

```bash
# Собрать все модули одной командой
mvn clean package -DskipTests

# Со сборкой тестов
mvn clean verify
```

## Запуск локально (без Docker)

Порядок запуска важен — каждый следующий сервис зависит от предыдущего.

```bash
# 1. Инфраструктура
docker compose up bank-db keycloak -d

# 2. Config Server (должен стартовать первым)
cd config-server
mvn spring-boot:run

# 3. Discovery Server (Eureka)
cd ../discovery-server
mvn spring-boot:run

# 4. Микросервисы (в любом порядке, в разных терминалах)
cd ../accounts-service && mvn spring-boot:run
cd ../cash-service     && mvn spring-boot:run
cd ../transfer-service && mvn spring-boot:run
cd ../notifications-service && mvn spring-boot:run

# 5. Gateway
cd ../gateway-service
mvn spring-boot:run

# 6. Фронт
cd ../bank-front-ui
mvn spring-boot:run
```

Открыть в браузере: http://localhost:8085

## Запуск в Docker

```bash
# Шаг 1 — собрать JAR-файлы
mvn clean package -DskipTests

# Шаг 2 — собрать образы и запустить все сервисы
docker compose up --build

# В фоновом режиме
docker compose up --build -d

# Посмотреть логи конкретного сервиса
docker compose logs -f accounts-service
```

Открыть в браузере: http://localhost:8085

### Остановка

```bash
# Остановить все контейнеры
docker compose down

# Остановить и удалить данные БД
docker compose down -v
```

## Переменные окружения

При запуске через Docker Compose секреты передаются через environment:

| Переменная | Описание |
|---|---|
| `KEYCLOAK_CLIENT_SECRET` | Секрет клиента `bank-front-client` |
| `ACCOUNTS_CLIENT_SECRET` | Секрет клиента `accounts-service` |
| `CASH_CLIENT_SECRET` | Секрет клиента `cash-service` |
| `TRANSFER_CLIENT_SECRET` | Секрет клиента `transfer-service` |

Пример запуска с явными секретами:

```bash
KEYCLOAK_CLIENT_SECRET=abc123 \
CASH_CLIENT_SECRET=def456 \
TRANSFER_CLIENT_SECRET=ghi789 \
ACCOUNTS_CLIENT_SECRET=jkl000 \
docker compose up --build -d
```