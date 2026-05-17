package com.iiooiio.facebookapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
public class FacebookapiApplication {

    public static void main(String[] args) {
        SpringApplication.run(FacebookapiApplication.class, args);
    }

    // 加上這個 Bean，之後全專案都可以直接 @Autowired 注入使用
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}