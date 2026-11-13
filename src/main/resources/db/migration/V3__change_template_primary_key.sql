-- 기존 테이블 삭제
DROP TABLE invitation_documents;
DROP TABLE invitations;
DROP TABLE templates;

-- 1. 디자인 템플릿
CREATE TABLE templates
(
    id               BIGINT                                            NOT NULL AUTO_INCREMENT,
    code             VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    name             VARCHAR(60)                                       NOT NULL,
    subtitle         VARCHAR(120)                                      NOT NULL,
    description      VARCHAR(500)                                      NOT NULL,
    category         VARCHAR(32)                                       NOT NULL,
    tags             JSON                                              NOT NULL,
    cover_url        VARCHAR(512)                                      NOT NULL,
    default_font_key VARCHAR(64)
                         CHARACTER SET ascii COLLATE ascii_bin         NOT NULL,
    is_selectable    BOOLEAN                                           NOT NULL DEFAULT TRUE,
    created_at       DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at       DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),
    UNIQUE KEY uq_templates_code (code),

    CONSTRAINT ck_templates_tags
        CHECK (JSON_TYPE(tags) = 'ARRAY'),
    CONSTRAINT ck_templates_selectable
        CHECK (is_selectable IN (0, 1))
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;


-- 2. 사용자가 만든 청첩장
CREATE TABLE invitations
(
    id               BIGINT                                         NOT NULL AUTO_INCREMENT,
    owner_id         BIGINT                                         NOT NULL,
    management_title VARCHAR(50)                                    NOT NULL DEFAULT '',
    public_id        CHAR(32) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    status           VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin
                                                                    NOT NULL DEFAULT 'DRAFT',
    version          INT                                            NOT NULL DEFAULT 0,
    created_at       DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at       DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),
    UNIQUE KEY uq_invitations_public (public_id),
    KEY              ix_invitations_owner_recent (owner_id, updated_at DESC, id DESC),

    CONSTRAINT fk_invitations_owner
        FOREIGN KEY (owner_id) REFERENCES users (id)
            ON DELETE CASCADE,
    CONSTRAINT ck_invitations_status
        CHECK (status IN ('DRAFT', 'PUBLISHED')),
    CONSTRAINT ck_invitations_version
        CHECK (version >= 0)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;


-- 3. 청첩장 작성본·공개본
CREATE TABLE invitation_documents
(
    invitation_id BIGINT                                            NOT NULL,
    kind          VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    template_id   BIGINT                                            NOT NULL,
    body          JSON                                              NOT NULL,
    created_at    DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at    DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),

    PRIMARY KEY (invitation_id, kind),

    CONSTRAINT fk_documents_invitation
        FOREIGN KEY (invitation_id) REFERENCES invitations (id)
            ON DELETE CASCADE,
    CONSTRAINT fk_documents_template
        FOREIGN KEY (template_id) REFERENCES templates (id)
            ON DELETE RESTRICT,
    CONSTRAINT ck_documents_kind
        CHECK (kind IN ('DRAFT', 'PUBLISHED')),
    CONSTRAINT ck_documents_body
        CHECK (JSON_TYPE(body) = 'OBJECT')
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;
