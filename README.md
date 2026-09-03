# GateKeeper: Traffic Analyzer & Rate Limiter System

GateKeeper is a lightweight, low-latency, thread-safe rate-limiting middleware system built on Java 17 and Spring Boot 3. It utilizes an in-memory Fixed Window counter pattern with non-blocking atomic operations to protect backend services from abusive request volumes, DoS attempts, and accidental bursts.

---

## 📚 Documentation Index
Comprehensive project architecture and requirement specifications are maintained in the [`docs/`](./docs) directory:

* [Product Requirements Document (PRD)](./docs/PRD.md)
* [Software Requirements Specification (SRS)](./docs/SRS.md)
* [System Architecture & Design](./docs/Architecture.md)
* [Use Cases & Scenarios](./docs/UseCases.md)
* [API Contract & Interception Documentation](./docs/API_Contract.md)

---

## 1. Prerequisites
* **Java Development Kit (JDK):** Version 17 or higher
* **Apache Maven:** Version 3.8+ (or use the included `./mvnw` wrapper)
* **cURL or Postman:** For sending test HTTP traffic and verifying rate-limiting headers

---

## 2. Project Structure

```text
gatekeeper
├── pom.xml
├── docs/
│   ├── PRD.md
│   ├── SRS.md
│   ├── Architecture.md
│   ├── UseCases.md
│   └── API_Contract.md
└── src/main/java/com/omar/gatekeeper/
    ├── GatekeeperApplication.java      # Application entry point
    ├── config/
    │   └── WebConfig.java             # WebMvcConfigurer registering interceptors
    ├── controller/
    │   └── TestController.java        # Health check endpoint (/api/test)
    ├── interceptor/
    │   └── RateLimitInterceptor.java  # Pre-controller request interceptor
    └── service/
        └── RateLimitingService.java   # Lockless Fixed Window rate-limiting service
```

---

## 3. Configuration Guide

Rate-limiting limits and window configurations are externalized in `src/main/resources/application.properties`:

```properties
spring.application.name=gatekeeper

# The maximum number of requests allowed per client IP within a single window
rate-limit.max-requests=10

# The duration of each window in milliseconds (e.g., 60000 = 1 minute)
rate-limit.window-size-ms=60000
```

### Parameter Details
| Property Key | Type | Default | Description |
| :--- | :--- | :--- | :--- |
| `rate-limit.max-requests` | Integer | `10` | The maximum number of requests permitted per unique client IP within the configured window. |
| `rate-limit.window-size-ms` | Long | `60000` | The duration of the fixed time window in milliseconds (60,000 ms = 1 minute). |

---

## 4. How to Build and Run

### Build the Application
```bash
./mvnw clean package -DskipTests
```

### Run the Application
```bash
./mvnw spring-boot:run
```
By default, the server will start on port `8080`.

---

## 5. Testing the Rate Limiter

### 1. Verification (Under Quota)
Send a single request to verify the server is active:
```bash
curl -i http://localhost:8080/api/test
```
**Expected Response:**
```http
HTTP/1.1 200 OK
Content-Type: text/plain;charset=UTF-8

GateKeeper Server is up and running!
```

### 2. Quota Breach (Simulating High Traffic)
Send 15 requests sequentially to trigger the rate limiter (configured threshold is 10):
```bash
for i in {1..15}; do 
  echo -n "Request $i: "; 
  curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8080/api/test; 
done
```
**Expected Output:**
```text
Request 1: 200
...
Request 10: 200
Request 11: 429
...
Request 15: 429
```

### 3. Testing Proxy Forwarding (`X-Forwarded-For`)
Simulate traffic passing through a reverse proxy:
```bash
curl -i -H "X-Forwarded-For: 203.0.113.55" http://localhost:8080/api/test
```

---

## 6. Engineering Details: Lock-Free Concurrency
* State is stored in a `ConcurrentHashMap<String, Window>`.
* `Window` is defined as an immutable Java record:
  ```java
  private record Window(long startTime, int count) {}
  ```
* Increments and resets are managed atomically through `ConcurrentHashMap.compute()`:
  ```java
  Window currentWindow = requestMap.compute(ip, (key, existingWindow) -> {
      if (existingWindow == null || (now - existingWindow.startTime()) > windowSizeMs) {
          return new Window(now, 1);
      }
      return new Window(existingWindow.startTime(), existingWindow.count() + 1);
  });
  ```
  This design ensures lock-free atomicity per IP key without coarse synchronized blocks or thread starvation.
