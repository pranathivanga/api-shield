# 🛡 API Shield

API Shield is a Redis-backed API protection layer built using Spring Boot.  
It enforces rate limits, tier-based access control, idempotency, and automatic abuse detection.

---

## 🚀 Features

- **Rate Limiting (Redis + TTL)**
  - Per-user and per-endpoint limits
  - Atomic counters using Redis
  - Automatic reset via TTL

- **Tier-Based Access Control**
  - FREE and PREMIUM users
  - Limits resolved from database
  - Different quotas per tier

- **Idempotency Support**
  - Prevents duplicate POST execution
  - Uses `Idempotency-Key` header
  - Cached responses stored in Redis

- **Abuse Detection**
  - Tracks repeated violations
  - Automatic temporary bans
  - Ban expiry handled via Redis TTL

- **Persistent User Management**
  - User tiers stored in database
  - JPA + H2 (development setup)

- **Minimal Demo UI**
  - Simple HTML interface
  - Demonstrates limits and bans visually

---

## 🏗 Architecture

```
Client
↓
Idempotency Filter
↓
Rate Limiting Interceptor
↓
Controller
↓
Redis (rate limits, violations, bans, idempotency)
↓
H2 Database (user tiers)
```

---

## 🛠 Tech Stack

- Java 17
- Spring Boot
- Spring Data JPA
- Redis
- H2 Database
- HTML + JavaScript

---

## 🧪 Running Locally

### Requirements
- Java 17+
- Redis running locally

### Start Application
git clone <repo-url>
cd api-shield
./mvnw spring-boot:run

## 🔍 Testing

** Use request header:

X-User-Id: 101   (FREE)
X-User-Id: 202   (PREMIUM)

** Behavior:

FREE users → lower request limit

PREMIUM users → higher request limit

Repeated violations → temporary ban (HTTP 403)

Rate limit exceeded → HTTP 429

Idempotent POST requests return cached response

## 📌 Status Codes

200 – Request successful

429 – Rate limit exceeded

403 – User temporarily blocked
