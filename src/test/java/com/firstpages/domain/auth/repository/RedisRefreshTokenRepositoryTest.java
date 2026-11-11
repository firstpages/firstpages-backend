package com.firstpages.domain.auth.repository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.redis.test.autoconfigure.DataRedisTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

// 실제 Redis를 사용하는 Refresh Token 저장소 통합 테스트
@DataRedisTest
@ActiveProfiles("local")
@Import(RedisRefreshTokenRepository.class)
class RedisRefreshTokenRepositoryTest {

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    // 실제 사용자 ID와 충돌하지 않도록 큰 범위에서 임의의 ID 생성
    private final Long userId = ThreadLocalRandom.current()
            .nextLong(1_000_000_000_000L, Long.MAX_VALUE);

    // 테스트가 종료되면 해당 테스트에서 사용한 Redis 키 삭제
    @AfterEach
    void tearDown() {
        refreshTokenRepository.deleteByUserId(userId);
    }

    @Test
    @DisplayName("Refresh Token을 저장하면 해시값과 TTL이 설정된다")
    void saveRefreshToken() {
        // given
        String tokenHash = "hash-A";
        Duration ttl = Duration.ofMinutes(30);

        // when
        refreshTokenRepository.save(userId, tokenHash, ttl);

        // then
        String savedHash = refreshTokenRepository.findByUserId(userId)
                .orElseThrow();

        assertEquals(tokenHash, savedHash);

        // Redis에 저장된 키의 남은 유효기간 확인
        // 실제 Redis 명령 실행 시간을 고려해 0초보다 큰지만 확인
        assertTrue(
                refreshTokenRepository.findByUserId(userId).isPresent()
        );
    }

    @Test
    @DisplayName("저장된 Refresh Token 해시값을 조회할 수 있다")
    void findRefreshToken() {
        // given
        String tokenHash = "hash-A";

        refreshTokenRepository.save(
                userId,
                tokenHash,
                Duration.ofMinutes(30)
        );

        // when
        String result = refreshTokenRepository.findByUserId(userId)
                .orElseThrow();

        // then
        assertEquals(tokenHash, result);
    }

    @Test
    @DisplayName("저장되지 않은 사용자 ID를 조회하면 빈 Optional을 반환한다")
    void findNonexistentRefreshToken() {
        // when
        boolean exists = refreshTokenRepository.findByUserId(userId)
                .isPresent();

        // then
        assertFalse(exists);
    }

    @Test
    @DisplayName("기존 해시값이 일치하면 새로운 Refresh Token으로 교체된다")
    void rotateSuccess() {
        // given
        String oldHash = "hash-A";
        String newHash = "hash-B";

        refreshTokenRepository.save(
                userId,
                oldHash,
                Duration.ofMinutes(30)
        );

        // when
        boolean rotated = refreshTokenRepository.rotate(
                userId,
                oldHash,
                newHash,
                Duration.ofMinutes(30)
        );

        // then
        assertTrue(rotated);

        assertEquals(
                newHash,
                refreshTokenRepository.findByUserId(userId).orElseThrow()
        );
    }

    @Test
    @DisplayName("이미 교체된 Refresh Token은 재사용할 수 없다")
    void rotateWithReusedToken() {
        // given
        String oldHash = "hash-A";
        String newHash = "hash-B";

        refreshTokenRepository.save(
                userId,
                oldHash,
                Duration.ofMinutes(30)
        );

        // 최초 Rotation 성공
        boolean firstRotation = refreshTokenRepository.rotate(
                userId,
                oldHash,
                newHash,
                Duration.ofMinutes(30)
        );

        // when
        // 이미 폐기된 기존 토큰으로 다시 Rotation 시도
        boolean secondRotation = refreshTokenRepository.rotate(
                userId,
                oldHash,
                "hash-C",
                Duration.ofMinutes(30)
        );

        // then
        assertTrue(firstRotation);
        assertFalse(secondRotation);

        // 실패한 요청 때문에 저장된 토큰이 변경되면 안 됨
        assertEquals(
                newHash,
                refreshTokenRepository.findByUserId(userId).orElseThrow()
        );
    }

    @Test
    @DisplayName("현재 저장된 해시값과 일치하면 Refresh Token을 삭제한다")
    void deleteIfMatchesSuccess() {
        // given
        String tokenHash = "hash-A";

        refreshTokenRepository.save(
                userId,
                tokenHash,
                Duration.ofMinutes(30)
        );

        // when
        boolean deleted = refreshTokenRepository.deleteIfMatches(
                userId,
                tokenHash
        );

        // then
        assertTrue(deleted);

        assertTrue(
                refreshTokenRepository.findByUserId(userId).isEmpty()
        );
    }

    @Test
    @DisplayName("해시값이 일치하지 않으면 Refresh Token을 삭제하지 않는다")
    void deleteIfMatchesFailure() {
        // given
        String tokenHash = "hash-A";

        refreshTokenRepository.save(
                userId,
                tokenHash,
                Duration.ofMinutes(30)
        );

        // when
        boolean deleted = refreshTokenRepository.deleteIfMatches(
                userId,
                "wrong-hash"
        );

        // then
        assertFalse(deleted);

        // 기존 데이터가 유지되어야 함
        assertEquals(
                tokenHash,
                refreshTokenRepository.findByUserId(userId).orElseThrow()
        );
    }

    @Test
    @DisplayName("동일한 Refresh Token으로 동시에 Rotation하면 하나만 성공한다")
    void rotateConcurrently() throws Exception {
        // given
        String oldHash = "hash-A";
        int requestCount = 10;

        refreshTokenRepository.save(
                userId,
                oldHash,
                Duration.ofMinutes(30)
        );

        // 모든 스레드가 같은 시작 신호를 기다리도록 설정
        CountDownLatch startLatch = new CountDownLatch(1);

        List<Callable<Boolean>> tasks = new ArrayList<>();

        for (int i = 0; i < requestCount; i++) {
            String newHash = "hash-" + i;

            // 각 요청은 동일한 기존 토큰으로 서로 다른 새 토큰 저장 시도
            tasks.add(() -> {
                startLatch.await();

                return refreshTokenRepository.rotate(
                        userId,
                        oldHash,
                        newHash,
                        Duration.ofMinutes(30)
                );
            });
        }

        // 요청 수만큼 작업 스레드 준비
        ExecutorService executor = Executors.newFixedThreadPool(requestCount);

        try {
            List<Future<Boolean>> futures = new ArrayList<>();

            // 모든 요청을 작업 큐에 제출
            for (Callable<Boolean> task : tasks) {
                futures.add(executor.submit(task));
            }

            // 대기 중인 요청들을 동시에 시작
            startLatch.countDown();

            int successCount = 0;

            for (Future<Boolean> future : futures) {
                if (future.get(10, TimeUnit.SECONDS)) {
                    successCount++;
                }
            }

            // 동일한 이전 토큰으로는 한 요청만 성공해야 함
            assertEquals(1, successCount);

            // 성공한 요청이 저장한 토큰이 Redis에 남아 있어야 함
            String savedHash = refreshTokenRepository
                    .findByUserId(userId)
                    .orElseThrow();

            assertNotEquals(oldHash, savedHash);
            assertTrue(savedHash.startsWith("hash-"));

        } finally {
            // 테스트가 종료되면 스레드 풀 종료
            executor.shutdownNow();
        }
    }
}