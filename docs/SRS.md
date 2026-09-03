# Software Requirements Specification (SRS)
## GateKeeper: Traffic Analyzer & Rate Limiter System

---

## 1. Functional Requirements (FR)

### FR-01: Ingress Request Interception
The system MUST intercept all inbound HTTP requests before dispatching them to target Spring `@RestController` endpoints via an implementation of `HandlerInterceptor.preHandle()`.

### FR-02: Client Identification & IP Resolution
The system MUST reliably determine the originating client IP address:
* Inspect the `X-Forwarded-For` request header.
* If multiple proxy hops are present in a comma-delimited chain, the system MUST parse the chain to resolve the client IP.
* Fallback to `HttpServletRequest.getRemoteAddr()` when no proxy headers exist or when `X-Forwarded-For` is empty.

### FR-03: Window Counter Evaluation
For each distinct IP address, the system MUST track the current window epoch (`startTime`) and request count (`count`):
* If `(currentTime - startTime) > windowSizeMs`, the system MUST atomically reset the window with a fresh timestamp and initial count of 1.
* If within the window, the system MUST atomically increment the count by 1.

### FR-04: Admission Decision Enforcement
* **Permitted:** If `count <= maxRequests`, the request is admitted and proceeds down the handler execution chain.
* **Blocked:** If `count > maxRequests`, the request is blocked, execution halts immediately, and the controller is never invoked.

### FR-05: Standardized Rejection Response
Blocked requests MUST receive:
* HTTP Status Code `429 Too Many Requests`.
* Response Header: `Content-Type: application/json`.
* Standardized JSON error payload:
  ```json
  {
    "error": "Too Many Requests",
    "message": "Rate limit exceeded. Please try again later."
  }
  ```

### FR-06: Configurable Thresholds
The limit threshold (`rate-limit.max-requests`) and the window period (`rate-limit.window-size-ms`) MUST be dynamically configurable via externalized application properties without requiring code re-compilation.

---

## 2. Non-Functional Requirements (NFR)

### NFR-01: Low Latency Overhead
Evaluation latency overhead introduced by the rate-limiting interceptor MUST remain under **1 millisecond** (p99 < 1ms) under nominal memory conditions.

### NFR-02: High Concurrency & Lock-Free Thread-Safety
The rate limiter MUST support simultaneous, highly concurrent requests from multiple threads without data races, deadlocks, or critical section thread starvation. It MUST avoid explicit monitor locks (`synchronized`, `ReentrantLock`) on the fast path, utilizing atomic operations such as `ConcurrentHashMap.compute()` with immutable records.

### NFR-03: Scalability & Memory Footprint
State management MUST retain a minimal memory footprint. The memory overhead per unique IP entry should not exceed 128 bytes. The system must be architecturally primed to adopt an eviction or TTL cleanup strategy for expired keys to prevent memory exhaustion over prolonged uptime.

### NFR-04: Reliability & Fail-Safe Operation
Any internal state calculation must handle edge cases gracefully (e.g., missing headers, malformed proxy strings) without throwing uncaught 500 Internal Server Errors to legitimate clients.
