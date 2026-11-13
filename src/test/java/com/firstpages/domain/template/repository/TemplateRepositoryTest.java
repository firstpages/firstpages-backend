
package com.firstpages.domain.template.repository;

import com.firstpages.domain.template.entity.Template;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// 디자인 템플릿의 저장 및 조회 기능 검증
@SpringBootTest
@Transactional
class TemplateRepositoryTest {

    @Autowired
    private TemplateRepository templateRepository;

    @Autowired
    private EntityManager entityManager;

    // 템플릿 테스트 데이터 생성
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
                selectable
        );
    }

    @Test
    void 템플릿_저장_및_기본키_자동생성() {
        // given
        Template template = createTemplate("test-paper-cover", true);

        // when
        Template saved = templateRepository.save(template);
        entityManager.flush();

        // then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCode()).isEqualTo("test-paper-cover");
    }

    @Test
    void 템플릿_코드로_조회() {
        // given
        Template template = templateRepository.save(
                createTemplate("test-paper-cover", true)
        );

        entityManager.flush();
        entityManager.clear();

        // when
        Template found = templateRepository
                .findByCode("test-paper-cover")
                .orElseThrow();

        // then
        assertThat(found.getId()).isEqualTo(template.getId());
        assertThat(found.getName()).isEqualTo("페이퍼 커버");
    }

    @Test
    void JSON_태그_저장_및_조회() {
        // given
        Long templateId = templateRepository.save(
                createTemplate("test-paper-cover", true)
        ).getId();

        entityManager.flush();
        entityManager.clear();

        // when
        Template found = templateRepository.findById(templateId)
                .orElseThrow();

        // then
        assertThat(found.getTags())
                .containsExactly("미니멀", "사진 중심");
    }

    @Test
    void 선택_가능한_템플릿만_조회() {
        // given
        templateRepository.save(
                createTemplate("test-selectable", true)
        );
        templateRepository.save(
                createTemplate("test-disabled", false)
        );

        entityManager.flush();

        // when
        List<Template> templates =
                templateRepository.findBySelectableTrue();

        // then
        assertThat(templates)
                .extracting(Template::getCode)
                .contains("test-selectable")
                .doesNotContain("test-disabled");
    }

    @Test
    void 선택_가능한_템플릿_페이징_조회() {
        // given
        templateRepository.save(
                createTemplate("test-page-1", true)
        );
        templateRepository.save(
                createTemplate("test-page-2", true)
        );

        entityManager.flush();

        // when
        Page<Template> result = templateRepository.findBySelectableTrue(
                PageRequest.of(0, 1)
        );

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isGreaterThanOrEqualTo(2);
    }
}
