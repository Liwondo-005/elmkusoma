# ELMKUSOMA Architecture Overview

## Architecture Pattern
Modular Monolith + Macroservices

## Backend Services
| Service | Technology | Purpose |
|---------|-----------|---------|
| elmkusoma-core | Java + Spring Boot | Main education/business platform |
| elmkusoma-realtime | Java + Spring Boot | WebSocket, chat, presence, live state |
| elmkusoma-workers | Java + Spring Boot | Async jobs, notifications, reports, certificates |
| elmkusoma-media | Java + Spring Boot | Media/file management |

## Frontend
- Next.js 16 + React 19 + Tailwind CSS 4

## Database
- PostgreSQL 16 (primary)
- Redis 7 (cache, presence, pub/sub)
- RabbitMQ 3.12 (async messaging)
- MinIO (S3-compatible object storage)

## Communication Rules
- Frontend → Core: REST/HTTPS
- Core → Workers: RabbitMQ
- Core → Realtime: RabbitMQ events
- Core → Media: REST (proxy)
- Realtime → Redis: Presence state
- Media → MinIO: S3 API
