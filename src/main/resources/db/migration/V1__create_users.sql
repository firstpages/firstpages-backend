CREATE TABLE users
(
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '서비스 내부 사용자 식별자',
    provider    VARCHAR(20)  NOT NULL COMMENT '소셜 로그인 제공자',
    provider_id VARCHAR(100) NOT NULL COMMENT '소셜 로그인 사용자 식별자',
    nickname    VARCHAR(50) NULL COMMENT '사용자 닉네임',
    created_at  DATETIME(6)  NOT NULL COMMENT '생성 시각',
    updated_at  DATETIME(6)  NOT NULL COMMENT '수정 시각',

    CONSTRAINT pk_users
        PRIMARY KEY (id),

    CONSTRAINT uk_users_provider_provider_id
        UNIQUE (provider, provider_id)

) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '사용자 정보';
