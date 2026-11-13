package com.firstpages.domain.template.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "templates")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Template {

    // 고유 식별자
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    // 코드
    @Column(name = "code", length = 64, nullable = false, unique = true)
    private String code;

    // 이름
    @Column(name = "name", length = 60, nullable = false)
    private String name;

    // 부제목
    @Column(name = "subtitle", length = 120, nullable = false)
    private String subtitle;

    // 상세설명
    @Column(name = "description", length = 500, nullable = false)
    private String description;

    // 카테고리
    @Column(name = "category", length = 32, nullable = false)
    private String category;

    // 태그 목록
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "tags", columnDefinition = "json", nullable = false)
    private List<String> tags;

    // 대표 이미지 경로
    @Column(name = "cover_url", length = 512, nullable = false)
    private String coverUrl;

    // 기본 글꼴 식별자
    @Column(name = "default_font_key", length = 64, nullable = false)
    private String defaultFontKey;

    // 선택 가능 여부
    @Column(name = "is_selectable", nullable = false)
    private boolean selectable = true;

    // 생성 일시
    @CreatedDate
    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;

    // 수정 일시
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    // 디자인 템플릿 생성자
    private Template(
            String code,
            String name,
            String subtitle,
            String description,
            String category,
            List<String> tags,
            String coverUrl,
            String defaultFontKey,
            boolean selectable
    ) {
        this.code = code;
        this.name = name;
        this.subtitle = subtitle;
        this.description = description;
        this.category = category;
        this.tags = List.copyOf(tags);
        this.coverUrl = coverUrl;
        this.defaultFontKey = defaultFontKey;
        this.selectable = selectable;
    }

    // 새로운 디자인 템플릿 생성 정적 팩토리 메서드
    public static Template create(
            String code,
            String name,
            String subtitle,
            String description,
            String category,
            List<String> tags,
            String coverUrl,
            String defaultFontKey,
            boolean selectable
    ) {
        return new Template(
                code,
                name,
                subtitle,
                description,
                category,
                tags,
                coverUrl,
                defaultFontKey,
                selectable
        );
    }
}
