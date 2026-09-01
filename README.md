# AgenticAI-springAI

A Spring Boot application leveraging **Spring AI** and **Ollama** (`llama3.2`) to implement synchronous chat interfaces, structured JSON outputs, prompt engineering strategies, and multiple asynchronous real-time streaming implementations.

---

## 🚀 How to Run the Project

### 1. Prerequisites
* **Java 17** or higher
* **Maven** (via the included `./mvnw` wrapper)
* **Ollama** installed and running locally with the `llama3.2` model

```bash
# Verify Ollama is running and download the model
ollama run llama3.2
```

### 2. Run the Application
Navigate to the root directory of your project and run the following command:

```bash
# On Linux/macOS
./mvnw spring-boot:run

# On Windows
mvnw.cmd spring-boot:run
```
The application will start by default on port `8080`.

---

## 📡 API Reference & cURL Examples

### 1. Standard AI Endpoints (`AIController`)

#### 🔹 Simple Chat
Send a basic string prompt and receive a direct string text response.
* **URL:** `/chat`
* **Query Params:** `message` (Default: "tell me something")

```bash
curl -X GET "http://localhost:8080/chat?message=Why%20is%20the%20sky%20blue?"
```

#### 🔹 Prompt Engineering (System & User Messages)
Uses explicit `SystemMessage` rules to force the model to act and explain things comprehensively like a detailed teacher.
* **URL:** `/prompt-engineering`
* **Query Params:** `topic` (Default: "java")

```bash
curl -X GET "http://localhost:8080/prompt-engineering?topic=Polymorphism"
```

#### 🔹 Prompt Template rendering
Renders pre-configured template strings securely with custom runtime language context parameters.
* **URL:** `/tempate`
* **Query Params:** `language` (Default: "java")

```bash
curl -X GET "http://localhost:8080/tempate?language=Python"
```

#### 🔹 Structured Output (Author Books JSON)
Enforces a low temperature, strict model formatting rules (`format=json`), and mapping conversion definitions to map raw LLM text cleanly back into an internal Java POJO (`AuthorBooks`).
* **URL:** `/author-books`
* **Query Params:** `author` (Default: "paulo coelho")

```bash
curl -X GET "http://localhost:8080/author-books?author=Stephen%20King"
```

---

### 2. Real-Time Streaming Endpoints (`StreamingController`)
All streaming endpoints output a continuous real-time transmission stream using the `text/event-stream` media type context.

#### 🔹 Server-Sent Events (SSE) Chat Stream
Streams real-time text tokens cleanly using Spring’s standard managed `SseEmitter` context wrapper.
* **URL:** `/stream/chat`
* **Query Params:** `message` (Default: "tell me about RAG in s sentence")

```bash
curl -N -X GET "http://localhost:8080/stream/chat?message=Explain%20Microservices"
```

#### 🔹 SSE Prompt Template Stream
Streams tokens using a parameterized template containing specific expertise level context rules.
* **URL:** `/stream/prompt`
* **Query Params:** `topic` (Default: "Spring AI"), `level` (Default: "beginner")

```bash
curl -N -X GET "http://localhost:8080/stream/prompt?topic=Docker&level=expert"
```

#### 🔹 Multi-Turn Conversational Stream
Streams a chained context block, automatically binding preceding historical conversation logs directly to the active prompt turn.
* **URL:** `/stream/multi-turn`
* **Query Params:** `message` (Required)

```bash
curl -N -X GET "http://localhost:8080/stream/multi-turn?message=Tell%20me%20more"
```

#### 🔹 Raw Object Response Stream
Emits sequential raw asynchronous chunks processing via an unformatted `ResponseBodyEmitter`.
* **URL:** `/stream/raw-stream`
* **Query Params:** `message` (Required)

```bash
curl -N -X GET "http://localhost:8080/stream/raw-stream?message=Write%20a%20poem%20about%20coding"
```

#### 🔹 Direct Asynchronous Output Stream
Uses a custom lightweight `StreamingResponseBody` pipeline to pipe tokens directly down into the HTTP response `OutputStream` without locking server engine threads.
* **URL:** `/stream/output-stream`
* **Query Params:** `message` (Required)

```bash
curl -N -X GET "http://localhost:8080/stream/output-stream?message=Explain%20quantum%20computing"
```

> **Note:** The `-N` flag in the streaming cURL commands disables response buffering, ensuring you can see the tokens stream into your terminal in real-time.
