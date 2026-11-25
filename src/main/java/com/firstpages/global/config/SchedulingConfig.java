package com.firstpages.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

// 사진 정리 스케줄러를 활성화
// proxyBeanMethods는 false로 하여 CGLIB 동적 프록시 생성을 생략
@Configuration(proxyBeanMethods = false)
// @Scheduled 애노테이션을 활성화
@EnableScheduling
public class SchedulingConfig {
}
