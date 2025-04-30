package org.example.controlkafkacluster.Config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    // CORS 설정 추가
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // 모든 엔드포인트에 대해 CORS를 허용
        registry.addMapping("/**")
                .allowedOrigins("http://localhost:3000","http://localhost:3001")  // 프론트엔드에서 요청을 보내는 도메인
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")  // 허용할 HTTP 메소드
                .allowedHeaders("*")  // 모든 헤더를 허용
                .allowCredentials(true);  // 쿠키와 인증 정보를 허용
    }
}
