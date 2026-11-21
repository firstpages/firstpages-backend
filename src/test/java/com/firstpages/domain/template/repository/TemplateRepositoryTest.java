package com.firstpages.domain.template.repository;

import com.firstpages.domain.template.entity.Template;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

// 실제 DB에서 템플릿의 저장과 JSON 매핑 및 목록 조회를 검증
@SpringBootTest
@Transactional
class TemplateRepositoryTest {

    @Autowired
    private TemplateRepository templateRepository;

    @Autowired
    private EntityManager entityManager;

    // 미리보기 JSON의 중첩 구조와 값이 DB 저장 후에도 유지되는지 검증
    @Test
    @DisplayName("미리보기 JSON을 저장하고 조회하면 원래 구조와 값이 유지된다")
    void saveAndReadPreviewBody() {
        // given
        ObjectNode previewBody = JsonNodeFactory.instance.objectNode();
        previewBody.put("font", "lee-seoyun");

        previewBody.putObject("basic")
                .put("groomName", "박서준")
                .put("brideName", "김다은")
                .put("weddingDate", "2027-05-22")
                .put("weddingTime", "12:30:00");

        previewBody.putObject("greeting")
                .put("enabled", true)
                .put("title", "저희 결혼합니다")
                .put("body", "소중한 날에 함께해 주세요.\n감사합니다.");

        ObjectNode gallery = previewBody.putObject("gallery");
        gallery.put("enabled", false);
        gallery.putArray("photos");

        previewBody.putNull("coverPhoto");

        Template template = Template.create(
                uniqueCode(),
                "하트 노트",
                "종이 위에 붙인 작은 사랑",
                "하트 장식이 있는 청첩장입니다.",
                "handwriting",
                List.of("손글씨", "로맨틱"),
                "/images/template-samples/color-transparent-v1/standing-embrace.png",
                "lee-seoyun",
                previewBody,
                true
        );

        Long templateId = templateRepository.save(template).getId();

        flushAndClear();

        // when
        Template found = templateRepository.findById(templateId)
                .orElseThrow();

        // then
        assertThat(found.getPreviewBody()).isNotNull();
        assertThat(found.getPreviewBody()).isEqualTo(previewBody);
    }

    // 템플릿 저장 시 기본키가 자동 생성되는지 검증
    @Test
    @DisplayName("템플릿을 저장하면 기본키가 자동 생성된다")
    void saveTemplate() {
        // given
        String code = uniqueCode();
        Template template = createTemplate(code, true);

        // when
        Template saved = templateRepository.save(template);
        entityManager.flush();

        // then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCode()).isEqualTo(code);
    }

    // 템플릿 코드로 저장한 데이터를 조회할 수 있는지 검증
    @Test
    @DisplayName("템플릿 코드로 해당 템플릿을 조회한다")
    void findByCode() {
        // given
        String code = uniqueCode();

        Long templateId = templateRepository.save(
                createTemplate(code, true)
        ).getId();

        flushAndClear();

        // when
        Template found = templateRepository.findByCode(code)
                .orElseThrow();

        // then
        assertThat(found.getId()).isEqualTo(templateId);
        assertThat(found.getCode()).isEqualTo(code);
        assertThat(found.getName()).isEqualTo("페이퍼 커버");
    }

    // JSON 컬럼의 태그가 원래 순서와 값으로 복원되는지 검증
    @Test
    @DisplayName("JSON으로 저장한 태그 목록을 원래 순서대로 조회한다")
    void saveAndReadJsonTags() {
        // given
        Long templateId = templateRepository.save(
                createTemplate(uniqueCode(), true)
        ).getId();

        flushAndClear();

        // when
        Template found = templateRepository.findById(templateId)
                .orElseThrow();

        // then
        assertThat(found.getTags())
                .containsExactly("미니멀", "사진 중심");
    }

    // 선택 가능한 템플릿만 목록에 포함되는지 검증
    @Test
    @DisplayName("목록 조회 시 선택 불가능한 템플릿은 제외한다")
    void findOnlySelectableTemplates() {
        // given
        Long selectableId = templateRepository.save(
                createTemplate(uniqueCode(), true)
        ).getId();

        Long disabledId = templateRepository.save(
                createTemplate(uniqueCode(), false)
        ).getId();

        flushAndClear();

        // when
        List<Template> result =
                templateRepository.findAllBySelectableTrueOrderByIdAsc();

        // then
        assertThat(result)
                .extracting(Template::getId)
                .contains(selectableId)
                .doesNotContain(disabledId);

        assertThat(result)
                .allMatch(Template::isSelectable);
    }

    // 선택 가능한 템플릿 목록의 ID 정렬을 검증
    @Test
    @DisplayName("선택 가능한 템플릿을 ID 오름차순으로 조회한다")
    void findTemplatesOrderByIdAsc() {
        // given
        Long firstId = templateRepository.save(
                createTemplate(uniqueCode(), true)
        ).getId();

        Long secondId = templateRepository.save(
                createTemplate(uniqueCode(), true)
        ).getId();

        Long thirdId = templateRepository.save(
                createTemplate(uniqueCode(), true)
        ).getId();

        flushAndClear();

        // when
        List<Template> result =
                templateRepository.findAllBySelectableTrueOrderByIdAsc();

        // then
        assertThat(result)
                .extracting(Template::getId)
                .contains(firstId, secondId, thirdId)
                .isSorted();
    }

    // 테스트용 템플릿 엔티티를 생성
    private Template createTemplate(String code, boolean selectable) {
        return Template.create(
                code,
                "페이퍼 커버",
                "한 권의 이야기처럼",
                "정사각형 사진과 세로 레터링",
                "minimal",
                List.of("미니멀", "사진 중심"),
                "/images/template-samples/camera-moment.png",
                "gowun-batang",
                JsonNodeFactory.instance.objectNode(),
                selectable
        );
    }

    // 기존 데이터와 중복되지 않는 템플릿 코드를 생성
    private String uniqueCode() {
        return "test-" + UUID.randomUUID();
    }

    // 변경 사항을 DB에 반영하고 영속성 컨텍스트를 초기화
    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}