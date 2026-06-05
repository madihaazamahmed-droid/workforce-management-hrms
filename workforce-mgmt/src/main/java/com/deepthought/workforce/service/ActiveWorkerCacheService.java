package com.deepthought.workforce.service;

import com.deepthought.workforce.dto.ActiveWorkerDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * All active-worker Redis operations isolated here.
 * Every method degrades gracefully — if Redis is down, caller gets safe defaults.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ActiveWorkerCacheService {

    private static final String HASH_KEY = "active_workers";
    private static final String WORKER_KEY_PREFIX = "worker_session:";
    // TTL safety net: 16 hours. If someone forgot to clock out, entry expires automatically.
    private static final Duration SESSION_TTL = Duration.ofHours(16);

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    public void addActiveWorker(ActiveWorkerDto dto) {
        try {
            String key = workerKey(dto.getWorkerId());
            String json = objectMapper.writeValueAsString(dto);
            redisTemplate.opsForValue().set(key, json, SESSION_TTL);
            redisTemplate.opsForSet().add(HASH_KEY, key);
        } catch (Exception ex) {
            log.warn("Redis unavailable — could not cache clock-in for workerId={}. Continuing.", dto.getWorkerId());
        }
    }

    public void removeActiveWorker(Long workerId) {
        try {
            String key = workerKey(workerId);
            redisTemplate.delete(key);
            redisTemplate.opsForSet().remove(HASH_KEY, key);
        } catch (Exception ex) {
            log.warn("Redis unavailable — could not remove clock-out for workerId={}. Continuing.", workerId);
        }
    }

    /**
     * Returns all active workers from Redis.
     * Used exclusively by GET /api/attendance/active — never hits the DB.
     */
    public List<ActiveWorkerDto> getAllActiveWorkers() {
        try {
            Set<Object> keys = redisTemplate.opsForSet().members(HASH_KEY);
            if (keys == null || keys.isEmpty()) return Collections.emptyList();

            return keys.stream()
                    .map(k -> {
                        try {
                            Object val = redisTemplate.opsForValue().get(k.toString());
                            if (val == null) return null;
                            return objectMapper.readValue(val.toString(), ActiveWorkerDto.class);
                        } catch (Exception e) {
                            log.warn("Could not deserialize active worker entry for key={}", k);
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .toList();
        } catch (Exception ex) {
            log.warn("Redis unavailable — returning empty active workers list.");
            return Collections.emptyList();
        }
    }

    public boolean isWorkerActive(Long workerId) {
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(workerKey(workerId)));
        } catch (Exception ex) {
            log.warn("Redis unavailable — cannot check active status for workerId={}", workerId);
            return false; // Safe default: allow the DB check to be the source of truth
        }
    }

    /** Called when worker profile changes — invalidate their session entry */
    public void invalidateWorkerSession(Long workerId) {
        try {
            String key = workerKey(workerId);
            Object cached = redisTemplate.opsForValue().get(key);
            if (cached != null) {
                // Re-read from DB will happen on next relevant request; for now just evict
                log.info("Invalidating stale session cache for workerId={}", workerId);
                redisTemplate.delete(key);
                redisTemplate.opsForSet().remove(HASH_KEY, key);
            }
        } catch (Exception ex) {
            log.warn("Redis unavailable — could not invalidate session for workerId={}", workerId);
        }
    }

    private String workerKey(Long workerId) {
        return WORKER_KEY_PREFIX + workerId;
    }
}
