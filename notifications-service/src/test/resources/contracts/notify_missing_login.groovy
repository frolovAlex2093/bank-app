package contracts

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "POST /api/notifications — без login возвращает 400"

    request {
        method POST()
        urlPath("/api/notifications")
        headers {
            contentType(applicationJson())
            header("Authorization", matching("Bearer .*"))
        }
        body(
                message: "какое-то сообщение"
        )
    }

    response {
        status BAD_REQUEST()
    }
}