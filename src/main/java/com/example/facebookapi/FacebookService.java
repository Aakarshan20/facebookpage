package com.example.facebookapi;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
public class FacebookService  {

    @Autowired
    private RestTemplate restTemplate;

    // 透過 @Value 自動讀取 properties 的設定值
    @Value("${facebook.page-id}")
    private String pageId;

    @Value("${facebook.page-access-token}")
    private String pageAccessToken;



    public void publishScheduledPost() {
        try {

            // 2. 設定預計發文時間
            LocalDateTime localDateTime = LocalDateTime.of(2026, 6, 1, 13, 0, 0);
            long unixTimestamp = localDateTime.atZone(ZoneId.of("Asia/Taipei")).toEpochSecond();

            // 3. 準備請求參數 (RestTemplate 傳送表單資料要用 MultiValueMap)
            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("message", "這是透過 Java RestTemplate 自動排程發送的測試貼文！2");
            params.add("published", "false");
            params.add("scheduled_publish_time", String.valueOf(unixTimestamp));
            params.add("access_token", pageAccessToken);

            // 4. 設定 Header (指定為表單格式 application/x-www-form-urlencoded)
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            // 包裝成 HttpEntity
            HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(params, headers);

            // 5. 發送 POST 請求
            String url = "https://graph.facebook.com/v20.0/" + pageId + "/feed";
            System.out.println("正在透過 RestTemplate 發送排程請求至 Facebook API...");

            String response = restTemplate.postForObject(url, requestEntity, String.class);

            // 6. 印出結果
            System.out.println("Facebook API 回應內容: " + response);

        } catch (Exception e) {
            System.err.println("呼叫 Facebook API 發生錯誤：");
            e.printStackTrace();
        }
    }

    // 記得在檔案頂部補上需要的 import（如果原本沒有的話）
// import org.springframework.http.HttpMethod;
// import org.springframework.http.ResponseEntity;

    public void getScheduledPosts() {
        try {
            // 1. 組裝查詢的 URL，指定要撈取的欄位（如：id, message, scheduled_publish_time）
            // 網址格式為: https://graph.facebook.com/v20.0/{page_id}/scheduled_posts
            String url = String.format(
                    "https://graph.facebook.com/v20.0/%s/scheduled_posts?fields=id,message,scheduled_publish_time&access_token=%s",
                    pageId, pageAccessToken
            );

            System.out.println("正在發送請求查詢已排程文章...");

            // 2. 使用 RestTemplate 發送 GET 請求
            String response = restTemplate.getForObject(url, String.class);

            // 3. 印出回傳結果
            System.out.println("====== 目前已排程文章清單 ======");
            System.out.println(response);
            System.out.println("================================");

        } catch (Exception e) {
            System.err.println("查詢排程文章發生錯誤：");
            e.printStackTrace();
        }
    }
}