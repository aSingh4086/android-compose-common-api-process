# Common Api Request Class
## NetworkServiceClient.android.kt
This file contains the Android-specific implementation of the NetworkServiceClient class, which is part of a multiplatform Kotlin project. It provides a network service client using the Ktor HTTP client with the OkHttp engine for making HTTP requests.  

## Features
HTTP Methods: Supports GET, POST, PUT, and DELETE requests.
Authentication: Includes bearer token authentication using a token provided by the SharedAppContext.
Timeout Configuration: Configurable request, connection, and socket timeouts.
Error Handling: Handles HTTP errors and maps them to SharedUserMessage objects.
Singleton Pattern: Ensures a single instance of NetworkServiceClient using a thread-safe singleton implementation.
JSON Serialization: Uses Kotlinx Serialization for JSON parsing with lenient and unknown key handling.

## Dependencies
Ktor for HTTP client functionality.
Kotlinx Serialization for JSON serialization.
OkHttp as the HTTP client engine.
Key Components
NetworkServiceClient
Constructor: Accepts an optional SharedAppContext for handling authentication and network failure callbacks.

## Methods:
makeRequest: A generic method to handle all HTTP methods.
get, post, put, delete: Convenience methods for specific HTTP methods.
close: Closes the HTTP client.


## processApiResponse
A helper function to process the HTTP response:
Maps successful responses (status codes 200-299) to NetworkResult.Success.
Handles specific error codes (e.g., 401, 502) and invokes the onNetworkFailure callback if provided.
Parses error responses into SharedUserMessage objects.

