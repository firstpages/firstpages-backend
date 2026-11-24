package com.firstpages.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

// 청첩장 사진 저장과 조회 URL 발급에 필요한 S3 클라이언트를 구성
@Configuration(proxyBeanMethods = false)
public class S3Config {

    // 사진 업로드와 삭제에 사용할 S3 클라이언트를 생성
    @Bean
    public S3Client s3Client(
            @Value("${app.invitation-photos.region}") String region
    ) {
        return S3Client.builder()
                .region(Region.of(region))
                .build();
    }

    // 비공개 사진의 조회 URL을 서명할 객체를 생성
    @Bean
    public S3Presigner s3Presigner(
            @Value("${app.invitation-photos.region}") String region
    ) {
        return S3Presigner.builder()
                .region(Region.of(region))
                .build();
    }
}
