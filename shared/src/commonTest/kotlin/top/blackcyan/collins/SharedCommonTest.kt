package top.blackcyan.collins

import io.ktor.http.HttpHeaders
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest
import top.blackcyan.collins.data.CollinsAcceptHeaderValue
import top.blackcyan.collins.data.CollinsAccessKeyHeader
import top.blackcyan.collins.data.CollinsApiConfig
import top.blackcyan.collins.data.HttpCollinsApiClient

class SharedCommonTest {
    @Test
    fun listDictionariesSendsTheCollinsHeadersAndParsesThePayload() {
        runTest {
            val recorder = SharedCommonTestSupport.createRequestRecorder(defaultResponses())
            val client = HttpCollinsApiClient(
                httpClient = recorder.client,
                config = CollinsApiConfig(accessKey = "secret-key"),
            )

            val response = client.listDictionaries()

            assertEquals(listOf("british", "american"), response.map { it.dictionaryCode })
            assertEquals("British English", response.first().dictionaryName)
            assertEquals("/api/v1/dictionaries", recorder.lastRequest?.url?.encodedPath)
            assertEquals(
                CollinsAcceptHeaderValue,
                recorder.lastRequest?.headers?.getAll(HttpHeaders.Accept)?.single(),
            )
            assertEquals(
                "secret-key",
                recorder.lastRequest?.headers?.getAll(CollinsAccessKeyHeader)?.single(),
            )
        }
    }

    @Test
    fun searchAddsQueryParametersAndParsesTheSearchPage() {
        runTest {
            val recorder = SharedCommonTestSupport.createRequestRecorder(
                responses = listOf(
                    """
                        {
                          "dictionaryCode": "british",
                          "resultNumber": 12,
                          "pageNumber": 2,
                          "currentPageIndex": 1,
                          "results": [
                            {
                              "entryId": "car",
                              "entryLabel": "car",
                              "entryUrl": "https://example.test/car"
                            }
                          ]
                        }
                    """.trimIndent(),
                ),
            )
            val client = HttpCollinsApiClient(
                httpClient = recorder.client,
                config = CollinsApiConfig(accessKey = "secret-key"),
            )

            val response = client.search(dictionaryCode = "british", query = "car", pageSize = 25, pageIndex = 2)

            assertEquals("british", response.dictionaryCode)
            assertEquals(12, response.resultNumber)
            assertEquals(2, response.pageNumber)
            assertEquals(1, response.currentPageIndex)
            assertEquals("car", response.results.single().entryLabel)
            assertEquals("/api/v1/dictionaries/british/search/", recorder.lastRequest?.url?.encodedPath)
            assertEquals("car", recorder.lastRequest?.url?.parameters?.getAll("q")?.single())
            assertEquals("25", recorder.lastRequest?.url?.parameters?.getAll("pagesize")?.single())
            assertEquals("2", recorder.lastRequest?.url?.parameters?.getAll("pageindex")?.single())
        }
    }

    @Test
    fun entryAndPronunciationEndpointsReturnTheExpectedContent() {
        runTest {
            val recorder = SharedCommonTestSupport.createRequestRecorder(
                responses = listOf(
                    """
                        {
                          "dictionaryCode": "british",
                          "format": "html",
                          "entryContent": "<p>car</p>",
                          "entryId": "car",
                          "entryLabel": "car",
                          "entryUrl": "https://example.test/car",
                          "topics": [
                            {
                              "topicId": "topic-1",
                              "topicLabel": "Transport",
                              "topicUrl": "https://example.test/topic",
                              "topicParentId": null
                            }
                          ]
                        }
                    """.trimIndent(),
                    """
                        [
                          {
                            "dictionaryCode": "british",
                            "entryId": "car",
                            "lang": "en",
                            "pronunciationUrl": "https://example.test/car.mp3"
                          }
                        ]
                    """.trimIndent(),
                ),
            )
            val client = HttpCollinsApiClient(
                httpClient = recorder.client,
                config = CollinsApiConfig(accessKey = "secret-key"),
            )

            val entry = client.entry(dictionaryCode = "british", entryId = "car")
            val pronunciations = client.pronunciations(dictionaryCode = "british", entryId = "car", lang = "en")

            assertEquals("car", entry?.entryId)
            assertEquals("html", entry?.format)
            assertEquals(1, entry?.topics?.size)
            assertEquals("Transport", entry?.topics?.single()?.topicLabel)
            assertEquals("en", pronunciations.single().lang)
            assertEquals("/api/v1/dictionaries/british/entries/car", recorder.requests[0].url.encodedPath)
            assertEquals("/api/v1/dictionaries/british/entries/car/pronunciations", recorder.requests[1].url.encodedPath)
            assertEquals("html", recorder.requests[0].url.parameters.getAll("format")?.single())
            assertEquals("en", recorder.requests[1].url.parameters.getAll("lang")?.single())
        }
    }

    @Test
    fun missingEntryPayloadDecodesToNull() {
        runTest {
            val recorder = SharedCommonTestSupport.createRequestRecorder(
                responses = listOf("{}"),
            )
            val client = HttpCollinsApiClient(
                httpClient = recorder.client,
                config = CollinsApiConfig(accessKey = "secret-key"),
            )

            assertEquals(null, client.entry(dictionaryCode = "british", entryId = "ghost"))
        }
    }

    private fun defaultResponses(): List<String> =
        listOf(
            """
                [
                  {
                    "dictionaryCode": "british",
                    "dictionaryName": "British English",
                    "dictionaryUrl": "https://example.test/british"
                  },
                  {
                    "dictionaryCode": "american",
                    "dictionaryName": "American English",
                    "dictionaryUrl": "https://example.test/american"
                  }
                ]
            """.trimIndent(),
        )
}