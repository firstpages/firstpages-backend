CREATE TABLE invitation_guestbook_entries
(
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    invitation_id BIGINT       NOT NULL,
    author_name   VARCHAR(20)  NOT NULL,
    message       VARCHAR(500) NOT NULL,
    created_at    DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),
    KEY ix_guestbook_entries_invitation_created (invitation_id, created_at, id),

    CONSTRAINT fk_guestbook_entries_invitation
        FOREIGN KEY (invitation_id) REFERENCES invitations (id)
            ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;