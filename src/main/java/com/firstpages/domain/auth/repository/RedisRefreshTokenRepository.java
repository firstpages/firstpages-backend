package com.firstpages.domain.auth.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

// Redis를 이용한 Refresh Token 저장소 구현체
@Repository
@RequiredArgsConstructor
public class RedisRefreshTokenRepository implements RefreshTokenRepository {

    private static final String REFRESH_TOKEN_KEY_PREFIX = "auth:refresh:";

    // Redis에서 기존 토큰 비교와 새 토큰 저장을 원자적으로 처리
    private static final String ROTATE_SCRIPT = """
            local current = redis.call('GET', KEYS[1])
            
            if current ~= ARGV[1] then
                return 0
            end
            
            redis.call('SET', KEYS[1], ARGV[2], 'PX', ARGV[3])
            return 1
            """;

    // 저장된 Refresh Token 해시가 일치할 때만 키를 삭제
    private static final String DELETE_IF_MATCHES_SCRIPT = """
            local current = redis.call('GET', KEYS[1])
            
            if current ~= ARGV[1] then
                return 0
            end
            
            redis.call('DEL', KEYS[1])
            return 1
            """;

    private final StringRedisTemplate redisTemplate;

    // Refresh Token 해시값을 TTL과 함께 Redis에 저장
    @Override
    public void save(
            Long userId,
            String tokenHash,
            Duration ttl
    ) {
        redisTemplate.opsForValue()
                .set(
                        generateKey(userId),
                        tokenHash,
                        ttl
                );
    }

    // Redis에서 사용자 ID에 해당하는 Refresh Token 해시값 조회
    @Override
    public Optional<String> findByUserId(Long userId) {
        String tokenHash = redisTemplate.opsForValue()
                .get(generateKey(userId));

        return Optional.ofNullable(tokenHash);
    }

    // Redis에 저장된 사용자별 Refresh Token 정보 삭제
    @Override
    public void deleteByUserId(Long userId) {
        redisTemplate.delete(generateKey(userId));
    }

    // 기존 해시값이 일치하는 경우에만 새로운 해시값으로 교체
    @Override
    public boolean rotate(
            Long userId,
            String expectedHash,
            String newHash,
            Duration ttl
    ) {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>(ROTATE_SCRIPT, Long.class);

        // Redis에서 비교와 교체를 단일 Lua 스크립트로 실행
        Long result = redisTemplate.execute(
                script,
                List.of(generateKey(userId)),
                expectedHash,
                newHash,
                String.valueOf(ttl.toMillis())
        );

        // 1이면 교체 성공, 0이면 기존 토큰이 불일치하거나 없음
        return Long.valueOf(1L).equals(result);
    }

    // 현재 토큰과 일치하는 경우에만 Redis 키를 원자적으로 삭제
    @Override
    public boolean deleteIfMatches(
            Long userId,
            String expectedHash
    ) {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>(DELETE_IF_MATCHES_SCRIPT, Long.class);

        Long result = redisTemplate.execute(
                script,
                List.of(generateKey(userId)),
                expectedHash
        );

        return Long.valueOf(1L).equals(result);
    }

    // 사용자 ID를 이용해 Refresh Token 전용 Redis 키 생성
    private String generateKey(Long userId) {
        return REFRESH_TOKEN_KEY_PREFIX + userId;
    }
}
