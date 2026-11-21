-- 템플릿별 미리보기 JSON 데이터를 저장하는 컬럼 추가
ALTER TABLE templates
    ADD COLUMN preview_body JSON NULL AFTER default_font_key;

-- 미리보기 데이터는 JSON 객체만 허용
ALTER TABLE templates
    ADD CONSTRAINT ck_templates_preview_body
        CHECK (
            preview_body IS NULL
                OR JSON_TYPE(preview_body) = 'OBJECT'
            );