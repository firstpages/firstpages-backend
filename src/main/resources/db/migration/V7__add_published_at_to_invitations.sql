ALTER TABLE invitations
    ADD COLUMN published_at DATETIME(6) NULL
        COMMENT '마지막으로 공개본을 반영한 시각';