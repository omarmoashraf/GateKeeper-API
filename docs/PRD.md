# Product Requirements Document (PRD)
## GateKeeper: Traffic Analyzer & Rate Limiter System

---

## 1. Executive Summary & Vision
**GateKeeper** is a high-throughput, low-latency traffic analysis and rate-limiting middleware component engineered for modern Java and Spring Boot backend architectures. Its primary mission is to safeguard upstream and downstream application services from Denial-of-Service (DoS) attacks, brute-force exploits, noisy neighbors, and unintentional traffic surges.

GateKeeper provides predictable, non-blocking admission control at the ingress boundary, terminating abusive traffic before request execution reaches computational business logic, database transaction pools, or internal service meshes.

---

## 2. Target Audience
* **Public API Consumers:** Third-party developers, web clients, and mobile clients consuming public-facing RESTful APIs. GateKeeper enforces usage tiers, fair share allocation, and quota management.
* **Internal Microservices:** Service-to-service communication within the internal infrastructure where misconfigured batch jobs, unthrottled worker pools, or cascading retry loops could trigger systemic failure.
* **DevOps & Platform Security Engineers:** Platform operators requiring visibility into traffic patterns, anomaly detection, and granular edge admission control.

---

## 3. Product Scope

### 3.1 MVP Scope (Current State)
* **Runtime Framework:** Java 17+ and Spring Boot 3+ leveraging standard Spring Web interceptors.
* **Core Rate Limiting Algorithm:** Fixed Window Counter tracking requests over a configurable duration (window size in milliseconds).
* **State Management:** In-memory, non-blocking lockless concurrency model utilizing `ConcurrentHashMap` and immutable Java `record` containers.
* **Ingress Interception:** Pre-controller request filtering executed via `RateLimitInterceptor` registered in `WebConfig`.
* **Network Identity Extraction:** Client IP resolution capable of decoding standard socket addresses and extracting forwarded client identities via the `X-Forwarded-For` HTTP header.
* **Rejection Protocol:** Immediate short-circuiting with HTTP Status Code `429 Too Many Requests` and a standardized JSON payload.
* **Externalized Configuration:** Granular tuning of quota thresholds and window durations via `application.properties`.

### 3.2 Future Scope (Roadmap)
* **Persistence & Audit Logging:** Asynchronous logging of blocked requests, telemetry, and traffic violations into a PostgreSQL relational store.
* **Advanced Rate Limiting Algorithms:**
  * *Token Bucket / Leaky Bucket:* Accommodating legitimate bursty traffic while enforcing continuous consumption rates.
  * *Sliding Window Counter / Sliding Window Log:* Mitigating boundary-burst double quota issues inherent to pure fixed windows.
* **Access Control Lists (ACL):** Dynamic IP Whitelisting (CIDR blocks, internal VPCs) and Blacklisting (known malicious actors).
* **Distributed Caching Tier:** Redis-backed cluster state using Redis Lua scripts for multi-instance distributed rate limiting.
* **Metric Instrumentation:** Prometheus metrics and Micrometer integration for live Grafana alerting on rate-limit saturation.
