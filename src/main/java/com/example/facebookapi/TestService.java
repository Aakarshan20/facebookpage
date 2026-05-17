package com.example.facebookapi;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class TestService implements CommandLineRunner {

    @Autowired
    private RestTemplate restTemplate;

    @Override
    public void run(String... args) throws Exception {
        System.out.println("====== 開始測試 RestTemplate 呼叫 API ======");

        // 這裡用一個公開的測試 API 當範例（獲取一筆資料）
        String url = "https://jsonplaceholder.typicode.com/posts/1";

        // 發送 GET 請求，並直接將結果轉為 String 印出來
        String response = restTemplate.getForObject(url, String.class);

        System.out.println("API 回傳結果：");
        System.out.println(response);

        System.out.println("====== 測試結束 ======");
    }
}