-- 템플릿 미리보기의 바이너리 인사말을 UTF-8 문자열로 복원
UPDATE templates
SET preview_body = JSON_SET(
        preview_body,
        '$.greeting.body',
        CONVERT(
                FROM_BASE64(
                        SUBSTRING(
                                JSON_UNQUOTE(JSON_EXTRACT(
                                        CAST(preview_body AS CHAR CHARACTER SET utf8mb4),
                                        '$.greeting.body'
                                             )),
                                CHAR_LENGTH('base64:type15:') + 1
                        )
                ) USING utf8mb4
        )
                   )
WHERE JSON_UNQUOTE(JSON_EXTRACT(
        CAST(preview_body AS CHAR CHARACTER SET utf8mb4),
        '$.greeting.body'
                   )) LIKE 'base64:type15:%';