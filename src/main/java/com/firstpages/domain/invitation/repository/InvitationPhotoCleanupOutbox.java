package com.firstpages.domain.invitation.repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

// 청첩장 사진 정리 요청을 저장하고 처리 결과를 기록하는 Outbox
public interface InvitationPhotoCleanupOutbox {

    // 청첩장 삭제 트랜잭션에 사진 정리 요청을 기록
    void enqueue(Collection<String> storageKeys);

    // 실행 시각이 도래한 정리 요청의 객체 키를 조회
    List<String> findDueKeys();

    // 처리가 완료된 정리 요청을 제거
    void complete(String storageKey);

    // 실패 횟수를 기록하고 다음 시도 시각을 갱신
    void reschedule(String storageKey);
}
