
package com.firstpages.domain.invitation.entity;

import com.fasterxml.jackson.databind.JsonNode;
import com.firstpages.domain.template.entity.Template;
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

// 청첩장의 작성본과 공개본을 관리하는 엔티티
@Table(
        name = "invitation_documents",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_documents_invitation_kind",
                        columnNames = {"invitation_id", "kind"}
                )
        }
)
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class InvitationDocument {

    // 청첩장 문서 고유 식별자 (PK)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    // 문서가 속한 청첩장 (FK: invitations.id)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invitation_id", nullable = false)
    private Invitation invitation;

    // 청첩장 문서 종류 (DRAFT, PUBLISHED)
    @Enumerated(EnumType.STRING)
    @Column(name = "kind", length = 16, nullable = false)
    private InvitationDocumentKind kind;

    // 문서에 적용된 디자인 템플릿
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    private Template template;

    // 청첩장 작성본 또는 공개본의 JSON 데이터
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "body", columnDefinition = "json", nullable = false)
    private JsonNode body;

    // 청첩장 문서 생성 일시
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // 청첩장 문서 최종 수정 일시
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // 청첩장 문서 생성자
    private InvitationDocument(
            Invitation invitation,
            InvitationDocumentKind kind,
            Template template,
            JsonNode body
    ) {
        this.invitation = invitation;
        this.kind = kind;
        this.template = template;
        this.body = body.deepCopy();
    }

    // 새로운 청첩장 문서 생성 정적 팩토리 메서드
    public static InvitationDocument create(
            Invitation invitation,
            InvitationDocumentKind kind,
            Template template,
            JsonNode body
    ) {
        return new InvitationDocument(
                invitation,
                kind,
                template,
                body
        );
    }
}
