import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "GET /api/accounts/me — возвращает данные текущего пользователя"

    request {
        method GET()
        urlPath("/api/accounts/me")
        headers {
            header("Authorization", matching("Bearer .*"))
        }
    }

    response {
        status OK()
        headers {
            contentType(applicationJson())
        }
        body(
                login: "ivan_ivanov",
                firstName: $(anyNonEmptyString()),
                lastName: $(anyNonEmptyString()),
                balance: $(anyNumber())
        )
    }
}