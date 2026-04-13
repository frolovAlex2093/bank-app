package contracts

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "POST /api/notifications — валидный запрос возвращает 200"

    request {
        method POST()
        urlPath("/api/notifications")
        headers {
            contentType(applicationJson())
            header("Authorization", matching("Bearer .*"))
        }
        body(
                login: $(anyNonEmptyString()),
                message: $(anyNonEmptyString())
        )
    }

    response {
        status OK()
    }
}