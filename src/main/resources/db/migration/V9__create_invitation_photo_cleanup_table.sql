-- 청첩장 삭제와 같은 트랜잭션에 S3 사진 정리 요청을 기록한다.
CREATE TABLE invitation_photo_cleanup
(
    id              BIGINT                                                 NOT NULL AUTO_INCREMENT,
    storage_key     VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
    attempt_count   INT                                                    NOT NULL DEFAULT 0,
    next_attempt_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    created_at      DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_invitation_photo_cleanup_storage_key (storage_key),
    KEY             ix_invitation_photo_cleanup_due (next_attempt_at, id)
);