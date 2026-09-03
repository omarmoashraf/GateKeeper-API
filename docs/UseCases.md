# Use Cases & Scenarios
## GateKeeper: Traffic Analyzer & Rate Limiter System

---

### Use Case 1: Normal Authorized Traffic (Within Quota)
* **Actor:** Legitimate Web Client / Mobile Application
* **Preconditions:** The client IP has made fewer requests than `rate-limit.max-requests` within the current time window.
* **Trigger:** Client dispatches a `GET` request to `/api/test`.
* **Execution Flow:**
  1. The HTTP request reaches `DispatcherServlet` and is passed to `RateLimitInterceptor.preHandle()`.
  2. The interceptor resolves the client IP address (e.g., `192.168.1.50`).
  3. `RateLimitingService.isRequestAllowed("192.168.1.50")` evaluates the current window.
  4. The request counter increments from `3` to `4` (where `maxRequests = 10`).
  5. Condition `count <= maxRequests` evaluates to `true`.
  6. The interceptor returns `true` and the request proceeds to `TestController.healthCheck()`.
* **Postconditions:** Client receives HTTP status `200 OK` with response body `"GateKeeper Server is up and running!"`.

---

### Use Case 2: Spam / DoS Abuse Scenario (Quota Exceeded)
* **Actor:** Malicious Script / Aggressive Crawler / Misconfigured Microservice
* **Preconditions:** The client IP has already exhausted its allowed quota (`10` requests within the active 60-second window).
* **Trigger:** Client dispatches an 11th request to `/api/test`.
* **Execution Flow:**
  1. The HTTP request is intercepted by `RateLimitInterceptor.preHandle()`.
  2. The interceptor resolves the client IP address (e.g., `203.0.113.195`).
  3. `RateLimitingService.isRequestAllowed("203.0.113.195")` increments the counter to `11`.
  4. Condition `11 <= 10` evaluates to `false`.
  5. The interceptor logs the block event: `BLOCKED request from IP: 203.0.113.195`.
  6. The interceptor sets HTTP Status `429 Too Many Requests`.
  7. The interceptor sets `Content-Type: application/json` and writes the error payload to the response stream.
  8. The interceptor returns `false`, halting the execution pipeline immediately.
* **Postconditions:** 
  * Target controller is never invoked (saving CPU and DB resources).
  * Client receives HTTP status `429 Too Many Requests` with:
    ```json
    {
      "error": "Too Many Requests",
      "message": "Rate limit exceeded. Please try again later."
    }
    ```

---

### Use Case 3: Reverse Proxy / Cloud Load Balancer Request
* **Actor:** Client accessing through Cloudflare / AWS ALB / Nginx
* **Preconditions:** The application is hosted behind an edge proxy or load balancer.
* **Trigger:** Incoming HTTP request contains:
  * Socket IP (`remoteAddr`): `10.0.0.1` (Internal Load Balancer IP)
  * Header: `X-Forwarded-For: 198.51.100.42, 10.0.0.1`
* **Execution Flow:**
  1. `RateLimitInterceptor.getClientIP()` inspects the `X-Forwarded-For` header.
  2. The header is detected and parsed to extract the originating public IP (`198.51.100.42`).
  3. Rate limiting is scoped specifically to `198.51.100.42` rather than the proxy IP `10.0.0.1`.
* **Postconditions:** 
  * Rate-limiting quotas are correctly applied per real user/client.
  * Different users routed through the same load balancer do not share or exhaust each other's quota.
