package com.firstpages.domain.rsvp.service;

import com.firstpages.domain.invitation.scheduler.InvitationPhotoCleanupJob;
import com.firstpages.domain.rsvp.dto.request.RsvpCreateRequestDto;
import com.firstpages.domain.rsvp.dto.response.RsvpItemResponseDto;
import com.firstpages.domain.rsvp.entity.RsvpGuestSide;
import com.firstpages.domain.rsvp.exception.RsvpErrorCode;
import com.firstpages.global.exception.BusinessException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// 실제 MySQL에서 RSVP의 저장, 중복 제한, 목록과 통계를 검증한다.
@SpringBootTest
@EnabledIfEnvironmentVariable(named = "RUN_RSVP_MYSQL_TEST", matches = "true")
@DisplayName("RSVP MySQL 통합 테스트")
class RsvpMySqlIntegrationTest {

    // 실제 RSVP 제출과 소유자 조회 서비스
    @Autowired
    private RsvpService rsvpService;

    // 공개 청첩장 준비와 데이터베이스 제약 검증에 사용하는 JDBC 접근 객체
    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 테스트 실행 중 사진 정리 스케줄러의 외부 호출을 막는다.
    @MockitoBean
    private InvitationPhotoCleanupJob cleanupJob;

    // 테스트용 소유자와 청첩장에만 사용하는 고유 식별자
    private String publicId;

    // 테스트용 청첩장 소유자의 내부 식별자
    private Long ownerId;

    // 테스트용 공개 청첩장의 내부 식별자
    private Long invitationId;

    // 고유 소유자와 RSVP가 활성화된 공개 청첩장을 준비한다.
    @BeforeEach
    void setUp() {
        publicId = UUID.randomUUID().toString().replace("-", "");
        jdbcTemplate.update("""
                INSERT INTO users (provider, provider_id, nickname, created_at, updated_at)
                VALUES ('KAKAO', ?, 'RSVP 테스트', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6))
                """, publicId);
        ownerId = jdbcTemplate.queryForObject(
                "SELECT id FROM users WHERE provider = 'KAKAO' AND provider_id = ?",
                Long.class, publicId
        );
        jdbcTemplate.update("""
                INSERT INTO invitations (owner_id, public_id, status)
                VALUES (?, ?, 'PUBLISHED')
                """, ownerId, publicId);
        invitationId = jdbcTemplate.queryForObject(
                "SELECT id FROM invitations WHERE public_id = ?", Long.class, publicId
        );
        int documents = jdbcTemplate.update("""
                INSERT INTO invitation_documents (invitation_id, kind, template_id, body)
                SELECT ?, 'PUBLISHED', id, JSON_OBJECT('rsvp', JSON_OBJECT('enabled', CAST('true' AS JSON)))
                FROM templates WHERE is_selectable = TRUE ORDER BY id LIMIT 1
                """, invitationId);
        assertThat(documents).isEqualTo(1);
    }

    // 이번 테스트에서 만든 소유자를 삭제해 연결된 테스트 데이터도 정리한다.
    @AfterEach
    void tearDown() {
        if (publicId != null) {
            jdbcTemplate.update("DELETE FROM users WHERE provider = 'KAKAO' AND provider_id = ?", publicId);
        }
    }

    // 중복 응답이 저장되지 않고 페이지와 집계가 올바르게 계산되는지 확인한다.
    @Test
    @DisplayName("중복 제출을 거절하고 모든 페이지에서 신랑·신부 측 전체 집계를 반환한다")
    void savesResponsesAndReturnsInvitationWideSummary() {
        rsvpService.create(publicId, request("김참석", RsvpGuestSide.GROOM, "0123", true, 2, 2));
        rsvpService.create(publicId, request("박불참", RsvpGuestSide.GROOM, "4567", false, null, null));
        rsvpService.create(publicId, request("이참석", RsvpGuestSide.BRIDE, "8901", true, 0, 0));

        assertThatThrownBy(() -> rsvpService.create(
                publicId, request(" 김참석 ", RsvpGuestSide.GROOM, "0123", true, 0, 0)
        )).isInstanceOfSatisfying(BusinessException.class, exception ->
                assertThat(exception.getErrorCode()).isEqualTo(RsvpErrorCode.RSVP_ALREADY_SUBMITTED));

        assertThatThrownBy(() -> jdbcTemplate.update("""
                INSERT INTO rsvp_responses (
                    invitation_id, guest_name, guest_side, phone_last_four, attending,
                    companion_count, meal_count
                ) VALUES (?, '김참석', 'GROOM', '0123', TRUE, 0, 0)
                """, invitationId)).isInstanceOf(DuplicateKeyException.class);

        var first = rsvpService.getRsvps(ownerId, invitationId, 0, 1);
        var next = rsvpService.getRsvps(ownerId, invitationId, 1, 1);

        assertThat(first.getResponses().getTotalElements()).isEqualTo(3L);
        assertThat(first.getResponses().getTotalPages()).isEqualTo(3);
        assertThat(first.getResponses().getItems())
                .extracting(RsvpItemResponseDto::getGuestName).containsExactly("이참석");
        assertThat(next.getResponses().getItems())
                .extracting(RsvpItemResponseDto::getGuestName).containsExactly("박불참");
        assertThat(first.getSummary().getGroom().getAttendingGuests()).isEqualTo(3);
        assertThat(first.getSummary().getGroom().getDeclinedResponses()).isEqualTo(1);
        assertThat(first.getSummary().getGroom().getMealGuests()).isEqualTo(2);
        assertThat(first.getSummary().getBride().getAttendingGuests()).isEqualTo(1);
        assertThat(first.getSummary().getBride().getDeclinedResponses()).isZero();
        assertThat(first.getSummary().getBride().getMealGuests()).isZero();
        assertThat(next.getSummary()).usingRecursiveComparison().isEqualTo(first.getSummary());
    }

    // 하객 정보와 참석 인원을 제출 요청에 담는다.
    private RsvpCreateRequestDto request(
            String name, RsvpGuestSide side, String phone, boolean attending,
            Integer companionCount, Integer mealCount
    ) {
        var request = BeanUtils.instantiateClass(RsvpCreateRequestDto.class);
        ReflectionTestUtils.setField(request, "guestName", name);
        ReflectionTestUtils.setField(request, "guestSide", side);
        ReflectionTestUtils.setField(request, "phoneLastFour", phone);
        ReflectionTestUtils.setField(request, "attending", attending);
        ReflectionTestUtils.setField(request, "companionCount", companionCount);
        ReflectionTestUtils.setField(request, "mealCount", mealCount);
        return request;
    }
}
