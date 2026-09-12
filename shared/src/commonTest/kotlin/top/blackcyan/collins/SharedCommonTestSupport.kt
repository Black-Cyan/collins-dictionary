package top.blackcyan.collins

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf

object SharedCommonTestSupport {
    fun createRequestRecorder(responses: List<String>): RequestRecorder {
        val requests = mutableListOf<HttpRequestData>()
        val bodies = ArrayDeque(responses)
        val client = HttpClient(
            MockEngine { request ->
                requests += request
                respond(
                    content = bodies.removeFirst(),
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                )
            },
        )

        return RequestRecorder(client = client, requests = requests)
    }

    data class RequestRecorder(
        val client: HttpClient,
        val requests: List<HttpRequestData>,
    ) {
        val lastRequest: HttpRequestData?
            get() = requests.lastOrNull()
    }
}
