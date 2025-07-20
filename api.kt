

data class SharedAppContext(
    val onNetworkFailure: (() -> Unit)? = null,
    val getAuthenticationToken: suspend () -> String,
)

@Serializable
data class SharedUserMessage(
    var errors: List<ErrorMessage>? = listOf(),
    @SerialName("user_messages")
    val userMessages: List<UserMessage>? = listOf(),
)
@Serializable
data class ErrorMessage(
    val code: Long,
    val message: String,
)
@Serializable
data class UserMessage(
    val severity: String = "",
    val text: String = "",
    val details: Details = Details(),
)

@Serializable
data class Details(
    val code: Long? = null,
)

sealed class NetworkResult<out T> {
    data class Success<out T>(val data: T) : NetworkResult<T>()
    data class Error(val error: SharedUserMessage) : NetworkResult<Nothing>()
}

actual fun httpClientEngine(): HttpClientEngine = OkHttp.create()

actual class NetworkServiceClient actual constructor(val sharedAppContext: SharedAppContext?) {
    companion object {
        @Volatile
        private var instance: NetworkServiceClient? = null

        fun getInstance(sharedAppContext: SharedAppContext? = null): NetworkServiceClient {
            return instance ?: synchronized(this) {
                instance ?: NetworkServiceClient(sharedAppContext).also { instance = it }
            }
        }
    }

    val httpClient = HttpClient(httpClientEngine()) {
      
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
                isLenient = true
            })
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 30000
            connectTimeoutMillis = 10000
            socketTimeoutMillis = 15000
        }
        defaultRequest {
        

        }

    }

    actual suspend inline fun <reified T, reified R> makeRequest(
        url: String,
        body: T?,
        httpMethodType: HttpMethodType,
        queryParameters: Map<String, String>?
    ): NetworkResult<R> {
        try {
            val token = runBlocking { sharedAppContext?.getAuthenticationToken?.let { it() } ?: "" }
            val apiUrl = ""

            val response: HttpResponse = when (httpMethodType) {
                HttpMethodType.GET -> httpClient.get(apiUrl) {
                    bearerAuth(token)
                    contentType(ContentType.Application.Json)
                    url {
                        queryParameters?.forEach { (key, value) ->
                            parameters.append(key, value)
                        }
                    }
                }

                HttpMethodType.POST -> httpClient.post(apiUrl) {
                    if (token.isNotEmpty()) {
                        bearerAuth(token)
                    }
                    contentType(ContentType.Application.Json)
                    setBody(body)
                }

                HttpMethodType.PUT -> httpClient.put(apiUrl) {
                    token?.let {
                        bearerAuth(it)
                    }
                    contentType(ContentType.Application.Json)
                    setBody(body)
                }

                HttpMethodType.DELETE -> httpClient.delete(apiUrl) {
                    bearerAuth(token)
                    contentType(ContentType.Application.Json)
                    setBody(body)
                }
            }
            return processApiResponse<R>(response, sharedAppContext)
        } catch (e: Exception) {
            e.printStackTrace()
            return NetworkResult.Error(
                SharedUserMessage(
                    errors = listOf(
                        ErrorMessage(
                            code = 500,
                            message = e.message ?: "Unknown error occurred"
                        )
                    )
                )
            )
        }
    }

    actual suspend inline fun <reified T, reified R> post(
        url: String,
        body: T,
        queryParameters: Map<String, String>?
    ): NetworkResult<R> {
        return makeRequest<T, R>(url, body = body, HttpMethodType.POST, queryParameters)
    }

    actual suspend inline fun <reified T, reified R> put(
        url: String,
        body: T,
        queryParameters: Map<String, String>?
    ): NetworkResult<R> {
        return makeRequest<T, R>(url, body = body, HttpMethodType.PUT, queryParameters)
    }

    actual suspend inline fun delete(
        url: String,
        queryParameters: Map<String, String>?
    ): NetworkResult<Unit> {
        return makeRequest<Unit, Unit>(
            url, null, HttpMethodType.DELETE, queryParameters
        )
    }


    actual suspend inline fun <reified R> get(
        url: String,
        queryParameters: Map<String, String>?
    ): NetworkResult<R> {
        return makeRequest<R, R>(url, null, HttpMethodType.GET, queryParameters)
    }

    actual fun close() {
        httpClient.close()
    }


}


suspend inline fun <reified T> processApiResponse(
    response: HttpResponse, sharedAppContext: SharedAppContext?
): NetworkResult<T> {
    return when (response.status.value) {
        in 200..299 -> {
            val responseBody: T = response.body()
            NetworkResult.Success(responseBody)
        }

        401 -> {
            sharedAppContext?.onNetworkFailure?.let { it() }
            NetworkResult.Error(response.body())

        }

        502 -> {
            sharedAppContext?.onNetworkFailure?.let { it() }
            NetworkResult.Error(response.body())

        }

        else -> {

            val json = Json
            val errorBody = response.bodyAsText()
            val errorModel = json.decodeFromString<SharedUserMessage>(errorBody)
            NetworkResult.Error(errorModel)
        }
    }
}
