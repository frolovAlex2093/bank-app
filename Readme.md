# Bank App

Микросервисное приложение «Банк» на Spring Boot + Spring Cloud.
Позволяет клиенту банка редактировать данные аккаунта, класть и снимать деньги, переводить средства другим пользователям.

### Схема портов

| Сервис | Порт |
|---|---|
| Keycloak | 9090 |
| Gateway | 8080 |
| Accounts Service | 8081 |
| Cash Service | 8082 |
| Transfer Service | 8083 |
| Notifications Service | 8084 |
| Front UI | 8085 |

## Структура проекта

```
bank-root/
├── helm/           
├── gateway-service/         # Spring Cloud Gateway MVC
├── accounts-service/        # Аккаунты и балансы
├── cash-service/            # Пополнение и снятие наличных
├── transfer-service/        # Переводы между счетами
├── notifications-service/   # Уведомления 
├── bank-front-ui/           # Веб-интерфейс
├── docker-compose.yml
└── pom.xml                  # Parent POM
```
## Структура Helm

```text
helm/bank
├── Chart.yaml
├── values.yaml
├── templates/tests/service-discovery-test.yaml
└── charts
    ├── postgres
    ├── keycloak
    ├── accounts-service
    ├── cash-service
    ├── transfer-service
    ├── notifications-service
    ├── gateway-service
    └── bank-front-ui
```

## Требования

- Java 21
- Maven 3.9+
- Docker
- Kubernetes (Minikube / Kind / Rancher Desktop / Colima)
- Helm 

## Сборка

```bash
# Собрать все модули одной командой
mvn clean package -DskipTests

# Со сборкой тестов
mvn clean verify
```

## Сборка Docker-образов

Пример для локального Docker daemon:

```bash
docker build -t accounts-service:latest ./accounts-service
docker build -t cash-service:latest ./cash-service
docker build -t transfer-service:latest ./transfer-service
docker build -t notifications-service:latest ./notifications-service
docker build -t gateway-service:latest ./gateway-service
docker build -t bank-front-ui:latest ./bank-front-ui
```

## Деплой в Kubernetes

```bash
helm dependency update ./helm/bank
helm upgrade --install bank ./helm/bank --namespace bank-dev --create-namespace
```

Проверка ресурсов:

```bash
kubectl get pods,svc,statefulset -n bank-dev
```

## Доступ снаружи кластера

По умолчанию:
- `gateway-service` публикуется как `NodePort` (`30080`)
- `bank-front-ui` публикуется как `NodePort` (`30085`)

При необходимости можно включить Ingress в `helm/bank/charts/gateway-service/values.yaml`.

## Настройка секретов OAuth2

Секреты задаются в `helm/bank/values.yaml`:

- `global.oauth2Secrets.frontClientSecret`
- `global.oauth2Secrets.accountsClientSecret`
- `global.oauth2Secrets.cashClientSecret`
- `global.oauth2Secrets.transferClientSecret`

Issuer и JWK:

- `global.oauth2.issuerUri`
- `global.oauth2.jwkSetUri`

## Тестирование Helm-чартов

```bash
helm lint ./helm/bank
helm test bank -n bank-dev
```

`helm test` запускает pod-хук, который проверяет DNS-резолв микросервисов в кластере.