# API Contract & Interception Documentation
## GateKeeper: Traffic Analyzer & Rate Limiter System

---

## 1. Interception Boundary & Behavior
GateKeeper intercepts all incoming HTTP requests before they reach the controller layer. The interception behavior is transparent to clients when within quota, and emits an explicit error contract upon quota exhaustion.

| Criteria | Value / Behavior |
| :--- | :--- |
| **Interception Layer** | Spring Web MVC `HandlerInterceptor.preHandle` |
| **Path Mapping** | Global (`/**`) |
| **Allowed Methods** | `GET`, `POST`, `PUT`, `DELETE`, `PATCH`, `HEAD`, `OPTIONS` |
| **Normal Behavior** | Hands off execution to the target controller; HTTP status determined by endpoint |
| **Exceeded Behavior** | Short-circuits execution; returns HTTP `429 Too Many Requests` |

---

## 2. Rejection Response Specification (HTTP 429)

### HTTP Response Headers
```http
HTTP/1.1 429 Too Many Requests
Content-Type: application/json;charset=UTF-8
Connection: close
```

### JSON Response Schema
```json
{
  "$schema": "https://json-schema.org/draft/2020-12/schema",
  "title": "RateLimitErrorResponse",
  "type": "object",
  "properties": {
    "error": {
      "type": "string",
      "description": "Standard HTTP status error phrase",
      "example": "Too Many Requests"
    },
    "message": {
      "type": "string",
      "description": "Human-readable explanation of rate limit enforcement",
      "example": "Rate limit exceeded. Please try again later."
    }
  },
  "required": ["error", "message"],
  "additionalProperties": false
}
```

### Example Payload
```json
{
  "error": "Too Many Requests",
  "message": "Rate limit exceeded. Please try again later."
}
```

---

## 3. Sample Endpoint Behavior: Health Check (`/api/test`)

### Request
```http
GET /api/test HTTP/1.1
Host: localhost:8080
X-Forwarded-For: 203.0.113.10
```

### Response (Under Quota - Request #1 through #10)
```http
HTTP/1.1 200 OK
Content-Type: text/plain;charset=UTF-8
Content-Length: 37

GateKeeper Server is up and running!
```

### Response (Exceeded Quota - Request #11+)
```http
HTTP/1.1 429 Too Many Requests
Content-Type: application/json

{"error": "Too Many Requests", "message": "Rate limit exceeded. Please try again later."}
```
