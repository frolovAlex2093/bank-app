import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "PATCH /api/accounts/{login}/balance — уход в минус: возвращает 400"

    request {
        method PATCH()
        urlPath("/api/accounts/ivan_ivanov/balance") {
            queryParameters {
                parameter("amount", "-9999")
            }
        }
        headers {
            header("Authorization", matching("Bearer .*"))
        }
    }

    response {
        status BAD_REQUEST()
    }
}