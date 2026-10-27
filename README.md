# Jev terminal client — Spring Boot

Java 17 + Spring Boot. Every decision calls TypeSafe AI with your API key. No frontend, demo mode, canned predictions, fallback responses or built-in action policies. Standard output contains one plain JSON result; logs go to standard error. The process exits after each terminal request.

## Configure

Edit `.env` in the project directory:

```properties
TYPESAFE_API_KEY=your-api-key
JEV_MODEL=jev-1.14.0
JEV_IMAGES_ENABLED=false
JEV_LONG_CONTEXT_ENABLED=false
PORT=8080
```

Use Java properties syntax: no quotes or `export`. Add, change or remove values in this file. An absent/blank API key causes an error; it never falls back to a simulated result. `.env` is ignored by Git. `.env.example` contains blank/default configuration for sharing.

Spring loads `.env` from your **current working directory**, including when running the JAR. Environment variables override the file. Create your key at https://console.typesafe.ai. Each decision sends the supplied content to TypeSafe and uses your account. Do not include your populated `.env` in shared archives.

## Build and run

From the project directory, using Java 17+ and Maven 3.6.3+:

```powershell
mvn clean verify
java -jar target/jev-learning-lab-1.0.0.jar --request=requests/support-routing.json
```

Or use the standalone `jev-learning-lab.jar` delivered beside the source archive:

```powershell
java -jar jev-learning-lab.jar --request=path/to/request.json
```

Keep `.env` in the directory from which you run the command. Input requests contain `state` and named `questions`; the configured `JEV_MODEL` is applied by the client.

Print help as JSON:

```powershell
java -jar target/jev-learning-lab-1.0.0.jar --help
```

Read a request from stdin or save just the JSON result:

```powershell
Get-Content requests/support-routing.json -Raw | java -jar target/jev-learning-lab-1.0.0.jar --request=-
java -jar target/jev-learning-lab-1.0.0.jar --request=requests/support-routing.json > result.json
```

Maven execution is also available, but Maven may print its own build messages. Use the JAR for clean JSON stdout:

```powershell
mvn spring-boot:run '-Dspring-boot.run.arguments=--request=requests/support-routing.json'
```

Exit codes: `0` success/help; `1` configuration/provider failure; `2` invalid or unreadable request. Errors are JSON objects with an `error` field. HTTP/provider-related errors include a `status` field. No automatic retries are made.

## Request files

`requests/` contains editable **input definitions only**, with no predicted answers:

- `support-routing.json`: Choice + urgency Score
- `moderation.json`: independent Noul risk checks
- `sentiment.json`: closed-set Choice
- `quality-scoring.json`: independent rubric scores
- `rag-filter.json`: relevance, evidence and injection judgments
- `citation-check.json`: compare a claim and source
- `entity-matching.json`: compare structured records
- `memory-selection.json`: evaluate durability and sensitivity
- `tool-selection.json`: select a named tool without executing it
- `reranking.json`: score two supplied passages
- `image-localization.json`: experimental Box with a base64 image asset

Edit the state, questions, criteria and assets as needed. Each file runs against the API. No document retrieval, memory storage, database merging, tool execution or answer generation is performed. For image requests, set `JEV_IMAGES_ENABLED=true`. Long context is separately opt-in with `JEV_LONG_CONTEXT_ENABLED=true`.

## Optional JSON HTTP server

If you prefer curl or a REST client, explicitly start the API server:

```powershell
java -jar target/jev-learning-lab-1.0.0.jar --serve
curl.exe -sS http://127.0.0.1:8080/api/decisions -H 'Content-Type: application/json' --data-binary '@requests/support-routing.json'
```

`POST /api/decisions` returns the provider JSON directly. `/api/playground` is an alias. No browser interface is served. The server binds to 127.0.0.1; change `PORT` in `.env` if needed. Restart it after editing `.env`.

## Implementation and checks

- `TerminalCommand.java`: reads a file/stdin, validates, calls Jev, prints JSON.
- `JevClient.java`: authenticated API calls, timeouts, experimental headers and answer checks.
- `LabController.java`: optional JSON API and request validation.
- `application.yml`: imports `.env` without an extra dotenv dependency.
- `logback-spring.xml`: keeps logs off stdout.

Run `mvn test`. Test doubles exist only in test sources to verify transport and errors without consuming API credits; the application contains no fake provider. Terminal input is limited to two million bytes. Connect timeout is 10 seconds and request timeout is 45 seconds. The optional HTTP mode has a post-parse character limit; it is intended for local use.

Official references: https://docs.typesafe.ai/api/system-one, https://docs.typesafe.ai/images, https://docs.typesafe.ai/models.
