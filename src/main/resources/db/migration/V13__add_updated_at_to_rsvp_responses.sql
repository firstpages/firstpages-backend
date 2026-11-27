-- RSVP 응답의 마지막 수정 시각을 추가한다.
ALTER TABLE rsvp_responses
    ADD COLUMN updated_at DATETIME(6) NULL AFTER created_at;

-- 기존 응답은 제출 시각을 초기 수정 시각으로 사용한다.
UPDATE rsvp_responses
SET updated_at = created_at
WHERE updated_at IS NULL;

-- 신규 응답의 생성 시각을 기록하고 응답이 변경되면 수정 시각을 갱신한다.
ALTER TABLE rsvp_responses
    MODIFY COLUMN updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6);
