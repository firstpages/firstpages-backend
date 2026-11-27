-- 하객이 청첩장에 제출한 참석 응답을 저장한다.
CREATE TABLE rsvp_responses
(
    id              BIGINT      NOT NULL AUTO_INCREMENT,
    invitation_id   BIGINT      NOT NULL,
    guest_name      VARCHAR(20) NOT NULL,
    guest_side      VARCHAR(16) NOT NULL,
    phone_last_four VARCHAR(4)  NOT NULL,
    attending       BOOLEAN     NOT NULL,
    companion_count INT         NOT NULL,
    meal_count      INT         NOT NULL,
    created_at      DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),
    UNIQUE KEY uq_rsvp_responses_guest (invitation_id, guest_name, guest_side, phone_last_four),
    KEY ix_rsvp_responses_invitation_created (invitation_id, created_at, id),

    CONSTRAINT fk_rsvp_responses_invitation
        FOREIGN KEY (invitation_id) REFERENCES invitations (id)
            ON DELETE CASCADE,

    CONSTRAINT ck_rsvp_responses_guest_side
        CHECK (guest_side IN ('GROOM', 'BRIDE')),

    CONSTRAINT ck_rsvp_responses_counts
        CHECK (
            companion_count >= 0
            AND meal_count >= 0
            AND (
                (attending = TRUE AND meal_count <= companion_count + 1)
                OR (attending = FALSE AND companion_count = 0 AND meal_count = 0)
            )
        )
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;
