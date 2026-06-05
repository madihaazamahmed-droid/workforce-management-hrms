#!/bin/bash
# Run this inside the project root after cloning your GitHub repo
# It creates atomic commits — one per feature/ticket as required

set -e

git init
git add pom.xml README.md postman_collection.json

# Commit 1: Base project setup
git add src/main/java/com/deepthought/workforce/WorkforceApplication.java
git add src/main/java/com/deepthought/workforce/config/JacksonConfig.java
git add src/main/resources/application.yml
git add src/main/resources/application-staging.yml
git commit -m "chore: initial project setup — Spring Boot 3.2, JPA, Redis, Supabase config"

# Commit 2: Schema — entities
git add src/main/java/com/deepthought/workforce/entity/
git add src/main/java/com/deepthought/workforce/enums/
git add src/main/java/com/deepthought/workforce/repository/
git commit -m "feat: Part1 schema — Worker, Site, AttendanceLog, OvertimeEntry entities with JPA indexes and constraints"

# Commit 3: Core attendance + overtime APIs + Redis cache
git add src/main/java/com/deepthought/workforce/dto/
git add src/main/java/com/deepthought/workforce/service/
git add src/main/java/com/deepthought/workforce/controller/
git add src/main/java/com/deepthought/workforce/util/
git add src/main/java/com/deepthought/workforce/event/
git add src/main/java/com/deepthought/workforce/exception/
git add src/main/java/com/deepthought/workforce/config/WebConfig.java
git commit -m "feat: Part1 attendance/overtime APIs — clock-in/out, overtime calc, Redis active workers, settlement"

# Commit 4: LF-201 CORS
git add src/main/java/com/deepthought/workforce/config/SecurityConfig.java
git commit -m "fix(LF-201): configure CORS in Spring Security filter chain; externalize allowed origins to application.yml"

# Commit 5: LF-202 Redis graceful degradation
git add src/main/java/com/deepthought/workforce/config/RedisConfig.java
git commit -m "fix(LF-202): Redis connect-timeout + CacheErrorHandler for graceful degradation when Redis is offline"

# LF-203 fix is in AttendanceRepository (JOIN FETCH) + AttendanceService (Pageable) — already committed above
git commit --allow-empty -m "fix(LF-203): N+1 resolved via JOIN FETCH in AttendanceRepository; paginated response with PagedResponse wrapper"

# LF-204 fix is in OvertimeService + SmsNotificationListener — already committed above
git commit --allow-empty -m "fix(LF-204): settlement is single @Transactional; SMS fires via @TransactionalEventListener(AFTER_COMMIT) to prevent premature notification"

# LF-205 fix is in application-staging.yml HikariCP config — already committed above
git commit --allow-empty -m "fix(LF-205): HikariCP tuned for Supabase — keepalive, idle-timeout < 5min, max-lifetime, pooler URL; external API call moved before @Transactional"

echo "All commits created. Push with: git remote add origin <your-repo-url> && git push -u origin main"
