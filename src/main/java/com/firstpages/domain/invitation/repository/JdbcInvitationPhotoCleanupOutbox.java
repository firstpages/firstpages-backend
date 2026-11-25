package com.firstpages.domain.invitation.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

// 사진 정리 아웃박스를 MySQL에 저장하고 조회
@Repository
@RequiredArgsConstructor
public class JdbcInvitationPhotoCleanupOutbox implements InvitationPhotoCleanupOutbox {

    private final JdbcTemplate jdbcTemplate;

    // 호출자의 트랜잭션에 참여해 사진 정리 요청을 일괄 등록
    // Propagation.MANDATORY는 참여할 트랜잭션이 없을 경우 예외 발생
    @Transactional(propagation = Propagation.MANDATORY)
    @Override
    public void enqueue(Collection<String> storageKeys) {
        // 삭제할 사진이 없을 경우 즉시 종료
        if (storageKeys == null || storageKeys.isEmpty()) {
            return;
        }

        // batchUpdate는 바인딩 파라미터로 List<Object[]> 형태를 요구
        List<Object[]> arguments = storageKeys.stream()
                .map(storageKey -> new Object[]{storageKey})
                .toList();

        String sql = """
                INSERT INTO invitation_photo_cleanup (storage_key) 
                VALUES (?)
                """;

        jdbcTemplate.batchUpdate(
                sql,
                arguments
        );
    }

    // 실행 시각이 도래한 요청을 오래 대기한 순서로 최대 100건 조회
    @Override
    public List<String> findDueKeys() {
        String sql = """
                SELECT storage_key 
                FROM invitation_photo_cleanup 
                WHERE next_attempt_at <= CURRENT_TIMESTAMP(6)
                ORDER BY next_attempt_at, id
                LIMIT 100
                """;

        return jdbcTemplate.queryForList(
                sql,
                String.class
        );
    }

    // S3에서 성공적으로 삭제한 사진의 정리 요청을 제거
    @Override
    public void complete(String storageKey) {
        String sql = """
                DELETE FROM invitation_photo_cleanup
                WHERE storage_key = ?
                """;

        jdbcTemplate.update(
                sql,
                storageKey
        );
    }

    // 실패 횟수를 늘리고 다음 시도 예약
    @Override
    public void reschedule(String storageKey) {
        String sql = """
                UPDATE invitation_photo_cleanup
                SET next_attempt_at = TIMESTAMPADD(
                        MINUTE, 
                        LEAST(POW(2, LEAST(attempt_count, 6)), 60),
                        CURRENT_TIMESTAMP(6)
                ),
                attempt_count = attempt_count + 1
                WHERE storage_key = ?
                """;

        jdbcTemplate.update(
                sql,
                storageKey
        );
    }
}
