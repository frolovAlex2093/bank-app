import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "PATCH /api/accounts/{login}/balance — пополнение: возвращает 200"

    request {
        method PATCH()
        urlPath("/api/accounts/ivan_ivanov/balance") {
            queryParameters {
                parameter("amount", "500")
            }
        }
        headers {
            header("Authorization", matching("Bearer .*"))
        }
    }

    response {
        status OK()
    }
}