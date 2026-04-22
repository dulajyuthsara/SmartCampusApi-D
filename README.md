# Smart Campus Sensor & Room Management API

## 1. Overview

This is a RESTful API I built for the "Smart Campus" coursework using JAX-RS (Jersey 2.41). The idea is that a university campus has a bunch of rooms (like lecture halls, libraries, labs etc) and each room can have different sensors in it (temperature sensors, CO2 monitors, occupancy trackers and so on).

The API lets you:
- **Create, view and delete rooms** on the campus
- **Register sensors** and link them to specific rooms
- **Record sensor readings** (like temperature values) and keep a history of them
- **Filter sensors** by type using query parameters
- **Handle errors properly** with custom exceptions and meaningful error messages

### How it's structured

The API follows REST principles, so everything is organized around resources:

- `/api/v1` — Discovery endpoint that shows what the API can do and links to other endpoints
- `/api/v1/rooms` — For managing rooms (create, list, get, delete)
- `/api/v1/sensors` — For managing sensors (create, list, get, delete, filter by type)
- `/api/v1/sensors/{sensorId}/readings` — Sub-resource for managing historical readings of a sensor

I used `ConcurrentHashMap` as the data store instead of a real database (as required by the spec). It's basically an in-memory storage that's thread-safe so it doesn't break if multiple requests come in at the same time.

For error handling, I created custom exceptions with Exception Mappers so the API never shows raw Java stack traces. Instead it returns clean JSON error messages with proper HTTP status codes like 409, 422, 403, 500 etc.

There's also a logging filter that automatically logs every incoming request and outgoing response so I can see what's happening without putting log statements everywhere.

---

## 2. How to Build and Run the Project


- **Java 11** or higher installed
- **Maven** installed (or you can use the Maven wrapper if provided)

### Steps to run

1. Open a terminal/command prompt
2. Navigate to the project folder (the one with `pom.xml` in it):
   ```bash
   cd SmartCampusAPI
   ```

3. Clean and build the project:
   ```bash
   mvn clean install
   ```

4. Start the embedded Tomcat server:
   ```bash
   mvn tomcat7:run
   ```

5. Once you see the server is running in the console, the API is ready at:
   ```
   http://localhost:8080/api/v1
   ```

6. To stop the server, just press `Ctrl+C` in the terminal.

---

## 3. Sample cURL Commands

Here are 5 sample curl commands you can use to test the API:

### 1. Get API Discovery Info
```bash
curl -X GET http://localhost:8080/api/v1
```
This returns info about the API and links to the main endpoints.

### 2. Create a New Room
```bash
curl -X POST http://localhost:8080/api/v1/rooms -H "Content-Type: application/json" -d "{\"id\": \"LIB-301\", \"name\": \"Library Quiet Study\", \"capacity\": 50}"
```
Creates a room with id "LIB-301". Returns 201 Created if successful.

### 3. Register a Sensor in that Room
```bash
curl -X POST http://localhost:8080/api/v1/sensors -H "Content-Type: application/json" -d "{\"id\": \"TEMP-001\", \"type\": \"Temperature\", \"status\": \"ACTIVE\", \"currentValue\": 22.5, \"roomId\": \"LIB-301\"}"
```
Creates a temperature sensor and links it to room LIB-301. If the room doesn't exist it throws a 422 error.

### 4. Add a Reading to the Sensor (Sub-Resource)
```bash
curl -X POST http://localhost:8080/api/v1/sensors/TEMP-001/readings -H "Content-Type: application/json" -d "{\"value\": 23.1}"
```
Adds a new reading to sensor TEMP-001. The API auto-generates an ID and timestamp for the reading, and also updates the sensor's currentValue.

### 5. Try to Delete a Room That Has Sensors (Should Fail with 409)
```bash
curl -X DELETE http://localhost:8080/api/v1/rooms/LIB-301
```
This will return a 409 Conflict error because the room still has sensors in it. You have to remove the sensors first before deleting the room.

---

## Answers

### Part 1: Service Architecture & Setup

**Question 1.1: Explain the default lifecycle of a JAX-RS Resource class. Is a new instance instantiated for every incoming request, or does the runtime treat it as a singleton? Elaborate on how this architectural decision impacts the way you manage and synchronize your in-memory data structures (maps/lists) to prevent data loss or race conditions.**

By default, JAX-RS creates a brand new instance of the resource class for every HTTP request that arrives. This means if a hundred users hit the API simultaneously, the runtime spins up a hundred separate objects. As a direct consequence, any data stored as an instance variable inside the resource class is completely lost the moment that request finishes — making it an entirely unsuitable place to hold persistent state.

To work around this, I created a separate `DataStore` class using the enum singleton pattern. Because Java guarantees that an enum value is instantiated exactly once by the JVM, the `DataStore` instance is effectively a true singleton that survives across all requests. Every resource class accesses the same shared maps for rooms, sensors, and readings through this single object.

The remaining challenge is thread safety. When multiple requests run at the same time, they can all try to read and write to the same maps simultaneously, which is a classic race condition that can cause corrupted data or silent data loss. To prevent this, I used `ConcurrentHashMap` instead of a plain `HashMap`. `ConcurrentHashMap` is designed specifically for concurrent access — it uses internal locking strategies that allow multiple threads to operate safely without blocking each other unnecessarily. Had I used a regular `HashMap` here, two simultaneous writes could easily corrupt the map's internal state in ways that are notoriously difficult to reproduce and debug.

**Question 1.2: Why is the provision of "Hypermedia" (links and navigation within responses) considered a hallmark of advanced RESTful design (HATEOAS)? How does this approach benefit client developers compared to static documentation?**

HATEOAS — which stands for "Hypermedia As The Engine Of Application State" — is the principle that an API response should not just carry data, but also tell the client what it can do next by embedding navigational links directly in the response body. In this project, the discovery endpoint at `/api/v1` returns links pointing to `/api/v1/rooms` and `/api/v1/sensors`, so a client can explore the entire API surface without needing to consult any external documentation first.

The key advantage over static documentation is resilience to change. If a URL is ever restructured, a client that follows links from the API response simply picks up the new path automatically. A client relying on hardcoded URLs from a PDF or wiki page, on the other hand, would immediately break. The analogy to a website is useful here — nobody memorises every page URL; you just follow the links. HATEOAS brings that same navigability to APIs, making integrations more loosely coupled and significantly easier to maintain over time.

---

### Part 2: Room Management

**Question 2.1: When returning a list of rooms, what are the implications of returning only IDs versus returning the full room objects? Consider network bandwidth and client side processing.**

Returning only IDs produces a very compact response and saves bandwidth, but it shifts a significant burden onto the client. To display anything useful, the client would need to fire off a separate GET request for every ID it received — so a list of 100 rooms would result in 100 additional round trips to the server. This is the classic "N+1 problem," and in practice it dramatically increases latency and puts unnecessary load on the server.

Returning the full room objects produces a larger initial payload, but the client gets everything it needs in a single request. For a campus management context — where a dashboard typically needs to display room names, capacities, and other details all at once — this trade-off is clearly worth making. The marginal increase in response size is far less costly than the cumulative delay of making dozens of sequential API calls.

**Question 2.2: Is the DELETE operation idempotent in your implementation? Provide a detailed justification by describing what happens if a client mistakenly sends the exact same DELETE request for a room multiple times.**

Yes, DELETE is idempotent in this implementation. Here is what happens across repeated calls:

- **First call** to `DELETE /api/v1/rooms/LIB-301`: The room exists, it is removed from the data store, and the server responds with `200 OK`.
- **Second call** to `DELETE /api/v1/rooms/LIB-301`: The room is no longer present, so the server responds with `404 Not Found`.
- **Any further calls**: The result is identical — `404 Not Found` — every single time.

The critical point is that the server's state stops changing after the very first call. The room is gone, and no subsequent DELETE request modifies anything further. This satisfies the HTTP definition of idempotency: applying the operation multiple times produces the same end state as applying it once. The response code may differ between the first and subsequent calls, but the state of the server does not — and state, not response code, is what idempotency is actually about.

---

### Part 3: Sensor Operations & Linking

**Question 3.1: We explicitly use the @Consumes(MediaType.APPLICATION_JSON) annotation on the POST method. Explain the technical consequences if a client attempts to send data in a different format, such as text/plain or application/xml. How does JAX-RS handle this mismatch?**

Annotating a method with `@Consumes(MediaType.APPLICATION_JSON)` is a declaration to the JAX-RS runtime that this method will only accept requests whose body is JSON. When a client sends a request with a `Content-Type` of `text/plain` or `application/xml`, JAX-RS compares that header against the declared constraint during request matching — and rejects the request outright before our method code is ever invoked. The framework automatically returns an HTTP `415 Unsupported Media Type` response.

This is a useful form of automatic validation. It means the application does not need to write any defensive parsing code to guard against unexpected formats; JAX-RS enforces the contract at the framework level. The client receives a clear, unambiguous status code that tells it exactly what is wrong with the request.

**Question 3.2: You implemented this filtering using @QueryParam. Contrast this with an alternative design where the type is part of the URL path (e.g., /api/v1/sensors/type/CO2). Why is the query parameter approach generally considered superior for filtering and searching collections?**

Using a query parameter such as `/api/v1/sensors?type=CO2` is the more appropriate design for several interconnected reasons:

1. **Filters are inherently optional.** If the `?type` parameter is omitted, the endpoint simply returns the full collection. With a path-based approach, you would need to define two distinct endpoints — one for filtered results and one for the unfiltered list — which creates unnecessary duplication.

2. **Multiple filters compose cleanly.** Adding a second criterion is as natural as appending `&status=ACTIVE` to the query string. Encoding the same logic into the path quickly produces unwieldy URLs like `/sensors/type/CO2/status/ACTIVE` that are hard to read and hard to extend.

3. **It respects the semantics of REST.** The URL path is intended to identify a resource — `/api/v1/sensors` names the sensors collection. Query parameters are the conventional mechanism for qualifying *how* that collection should be retrieved (filtering, sorting, pagination). Embedding filter values in the path implies they identify a sub-resource, which is semantically incorrect and misleading.

4. **Caching behaviour is cleaner.** The canonical resource URL remains stable, and different filter combinations simply produce different query strings on the same base URL, which HTTP caching infrastructure handles naturally.

---

### Part 4: Deep Nesting with Sub-Resources

**Question 4.1: Discuss the architectural benefits of the Sub-Resource Locator pattern. How does delegating logic to separate classes help manage complexity in large APIs compared to defining every nested path in one massive controller class?**

The Sub-Resource Locator pattern works by having a resource class delegate a portion of the URL space to another dedicated class, rather than handling everything itself. In this project, when a request arrives at `/sensors/{sensorId}/readings`, the `SensorResource` class does not handle it directly — instead, it instantiates and returns a `SensorReadingResource` object, and JAX-RS forwards the remaining path segments to that class. All logic related to reading history lives exclusively in `SensorReadingResource`.

The architectural advantages of this approach become clear as an API grows:

1. **Single Responsibility Principle.** Each class has exactly one concern. `SensorResource` manages sensor lifecycle; `SensorReadingResource` manages reading data. Neither class needs to know about the internals of the other.

2. **Maintainability.** When a change is needed in how readings are stored or retrieved, there is exactly one file to open. There is no need to navigate hundreds of lines of sensor logic to find the relevant code.

3. **Testability.** Because the concerns are separated, the reading logic can be unit tested in complete isolation from the sensor logic, leading to more focused and reliable tests.

4. **Scalability.** Adding new nested resources — sensor alerts, calibration records, configuration history — simply means creating a new sub-resource class and adding a locator method. The alternative, putting every endpoint into one class, produces a "God Class" anti-pattern: a bloated, fragile file that becomes increasingly difficult to understand and safely modify over time.

---

### Part 5: Advanced Error Handling, Exception Mapping & Logging

**Question 5.2: Why is HTTP 422 often considered more semantically accurate than a standard 404 when the issue is a missing reference inside a valid JSON payload?**

HTTP `404 Not Found` carries a very specific meaning: the resource identified by the request URL does not exist. In this scenario, however, the URL `/api/v1/sensors` is perfectly valid and fully operational. The problem is not with the endpoint itself — it is with the data inside the request body. The client has supplied a `roomId` value that does not correspond to any room currently in the system.

HTTP `422 Unprocessable Entity` is semantically correct for this situation. It tells the client: "I received your request, I successfully parsed the JSON, but I cannot fulfil the operation because the content contains a logical error." The server understood the request completely; it simply cannot act on it given the current state of the data.

Returning `404` in this case would be actively misleading. A developer receiving that response would naturally assume the endpoint itself is missing or the URL is wrong — potentially spending considerable time debugging a URL that is, in fact, working perfectly. A `422` response pinpoints the issue precisely: the request is structurally valid, but the referenced resource does not exist, so the client should check the `roomId` it is sending.

**Question 5.4: From a cybersecurity standpoint, explain the risks associated with exposing internal Java stack traces to external API consumers. What specific information could an attacker gather from such a trace?**

Leaking stack traces in API responses is a significant security vulnerability because it hands an attacker a detailed map of the application's internals at no cost. The specific risks include:

- **Internal package and class structure** — A trace exposes fully qualified class names such as `com.smartcampus.resource.SensorResource`, revealing how the codebase is organised and what components exist.
- **Dependency fingerprinting** — Library names and versions (e.g., Jersey 2.41, Jackson 2.x) appear in stack frames. If any of those dependencies carry known CVEs, the attacker immediately knows which exploits to attempt.
- **Server file system paths** — Paths such as `/usr/local/tomcat/webapps/...` can reveal the operating system, the application server being used, and the deployment directory structure.
- **Database internals** — In the case of a persistence error, a trace may inadvertently expose SQL table names, column names, or even partial connection string information.
- **Business logic hints** — Method names and line numbers provide enough detail for an attacker to reason about control flow, identify error-prone paths, and tailor exploits accordingly.

This is why the project includes a global `ExceptionMapper` that intercepts all unhandled exceptions and returns only a generic, non-descriptive error message to the client. The full diagnostic detail is written to the server-side log, where it is accessible to developers but invisible to anyone outside the system.

**Question 5.5: Why is it advantageous to use JAX-RS filters for cross-cutting concerns like logging, rather than manually inserting Logger.info() statements inside every single resource method?**

Centralising logging in a JAX-RS filter is far superior to scattering log statements throughout individual resource methods for several practical reasons:

1. **Universal coverage without ongoing effort.** A filter intercepts every request and response automatically, regardless of which endpoint is called. Any new endpoint added to the API in the future is logged from day one, without any additional work.

2. **Adherence to the DRY principle.** Manually duplicating a log statement in every method introduces a large volume of repetitive code. If the logging format ever needs to change, a filter means updating a single class rather than tracking down and editing dozens of methods spread across multiple files.

3. **Clean separation of concerns.** Logging is infrastructure — it has nothing to do with the business logic of managing rooms or sensors. Embedding log calls inside resource methods conflates two distinct responsibilities and makes the business logic harder to read and reason about. A filter keeps these concerns properly isolated.

4. **Consistency and completeness.** Manual logging is error-prone; it is easy to forget to add statements to a new method, or to add them inconsistently. A filter operates at the framework level and provides a guarantee that no request ever goes unrecorded.
