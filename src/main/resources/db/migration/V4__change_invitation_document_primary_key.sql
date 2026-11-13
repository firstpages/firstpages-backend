-- 복합 기본 키를 제거하고 독립적인 BIGINT 기본 키 추가
ALTER TABLE invitation_documents
DROP
PRIMARY KEY,
    ADD COLUMN id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY FIRST,
    ADD UNIQUE KEY uq_documents_invitation_kind (invitation_id, kind);
