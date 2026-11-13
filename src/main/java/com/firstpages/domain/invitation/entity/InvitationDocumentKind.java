package com.firstpages.domain.invitation.entity;

// 청첩장 문서의 종류를 구분
public enum InvitationDocumentKind {
    // 사용자가 편집하고 임시 저장하는 작성본
    DRAFT,
    // 하객에게 공개되는 청첩장 문서
    PUBLISHED
}
