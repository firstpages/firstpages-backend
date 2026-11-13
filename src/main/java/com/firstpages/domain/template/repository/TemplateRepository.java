package com.firstpages.domain.template.repository;

import com.firstpages.domain.template.entity.Template;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

// 디자인 템플릿의 데이터 접근을 담당하는 리포지토리
public interface TemplateRepository extends JpaRepository<Template, Long> {

    // 템플릿 코드로 단일 템플릿 조회
    Optional<Template> findByCode(String code);

    // 선택 가능한 템플릿 전체 조회 (페이징 없음)
    List<Template> findBySelectableTrue();

    // 선택 가능한 템플릿 목록 페이징 조회
    Page<Template> findBySelectableTrue(Pageable pageable);

    // 카테고리별 선택 가능한 템플릿 전체 조회
    List<Template> findByCategoryAndSelectableTrue(String category);

    // 카테고리별 선택 가능한 템플릿 목록 페이징 조회
    Page<Template> findByCategoryAndSelectableTrue(
            String category,
            Pageable pageable
    );
}
