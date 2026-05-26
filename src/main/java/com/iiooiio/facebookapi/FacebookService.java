package com.iiooiio.facebookapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
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

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 手動單獨觸發本地圖文上傳排程測試 (已修正換行符號為 \n)
     */
    public void publishScheduledPostWithLocalPhoto() {
        try {
            LocalDateTime localDateTime = LocalDateTime.of(2026, 5, 24, 14, 0, 0);
            long unixTimestamp = localDateTime.atZone(ZoneId.of("Asia/Taipei")).toEpochSecond();

            MultiValueMap<String, Object> params = new LinkedMultiValueMap<>();
            // 修正點：將 \\n 改為 \n 才能在臉書上正確呈現換行
            params.add("caption", "下午茶就是......\n\n睡到下午才來喝! ☕ \n#悠閒 #睡過頭 #財富自由");

            Resource imageFile = new FileSystemResource("C:\\Users\\Tom\\Downloads\\Gemini_Generated_Image_kwsspfkwsspfkwss(1).png");
            params.add("source", imageFile);

            params.add("published", "false");
            params.add("scheduled_publish_time", String.valueOf(unixTimestamp));
            params.add("access_token", pageAccessToken);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(params, headers);

            String url = "https://graph.facebook.com/v20.0/" + pageId + "/photos";
            System.out.println("正在發送【本地圖文二進位排程】請求至 Facebook API...");

            String response = restTemplate.postForObject(url, requestEntity, String.class);
            System.out.println("Facebook API 回應內容: " + response);

        } catch (Exception e) {
            System.err.println("呼叫 Facebook API 發生錯誤：");
            e.printStackTrace();
        }
    }

    /**
     * 自動接龍算法：查詢後台排程時間，並精確計算下一棒時間點
     */
    public long calculateNextSlot() {
        long targetTimestamp;

        try {
            String url = String.format(
                    "https://graph.facebook.com/v20.0/%s/scheduled_posts?fields=scheduled_publish_time&access_token=%s",
                    pageId, pageAccessToken
            );

            System.out.println("🔍 正在查詢 FB 後台現有排程以計算接龍時間...");
            String jsonResponse = restTemplate.getForObject(url, String.class);

            JsonNode root = objectMapper.readTree(jsonResponse);
            JsonNode dataArray = root.get("data");

            long latestTimestamp = 0;

            if (dataArray != null && dataArray.isArray() && dataArray.size() > 0) {
                for (JsonNode post : dataArray) {
                    if (post.has("scheduled_publish_time")) {
                        long postTime = post.get("scheduled_publish_time").asLong();
                        if (postTime > latestTimestamp) {
                            latestTimestamp = postTime;
                        }
                    }
                }
            }

            if (latestTimestamp > 0) {
                targetTimestamp = latestTimestamp + 86400; // 往後推 24 小時

                LocalDateTime nextTimeStr = LocalDateTime.ofInstant(Instant.ofEpochSecond(targetTimestamp), ZoneId.of("Asia/Taipei"));
                System.out.println("📌 發現既有排程！將承接最後一篇，排程時間設定為 (台北時間): " + nextTimeStr);

            } else {
                LocalDateTime nowPlus24Hours = LocalDateTime.now(ZoneId.of("Asia/Taipei")).plusHours(24);
                targetTimestamp = nowPlus24Hours.atZone(ZoneId.of("Asia/Taipei")).toEpochSecond();

                System.out.println("📌 目前後台無既有排程。排程時間設定為 (現在+24小時): " + nowPlus24Hours);
            }

            return targetTimestamp;

        } catch (Exception e) {
            System.err.println("❌ 計算接龍時間發生異常，降級改用【現在時間 + 24小時】保底");
            e.printStackTrace();

            return LocalDateTime.now(ZoneId.of("Asia/Taipei")).plusHours(24)
                    .atZone(ZoneId.of("Asia/Taipei")).toEpochSecond();
        }
    }

    /**
     * 🤖 專屬機器人的終極完美接龍方法 (動態支援 txt 內文傳入)
     * 注意：這裡加入了 throws Exception，確保失敗時不會把 todo 資料夾移到 done
     */
    public void publishPerfectScheduledPostWithCaption(String fileAbsolutePath, String caption) throws Exception {
        try {
            // 1. 算出最新接龍時間戳記
            long nextScheduledTimestamp = calculateNextSlot();

            // 2. STEP 1: 直接上傳本地圖片到 /photos 取得隱藏素材 ID
            MultiValueMap<String, Object> photoParams = new LinkedMultiValueMap<>();
            Resource imageFile = new FileSystemResource(fileAbsolutePath);
            photoParams.add("source", imageFile);
            photoParams.add("published", "false");
            photoParams.add("access_token", pageAccessToken);

            HttpHeaders photoHeaders = new HttpHeaders();
            photoHeaders.setContentType(MediaType.MULTIPART_FORM_DATA);

            HttpEntity<MultiValueMap<String, Object>> photoEntity = new HttpEntity<>(photoParams, photoHeaders);

            System.out.println("【步驟 1】正在上傳實體圖檔至 Meta 伺服器...");
            java.util.Map<String, Object> photoResponse = restTemplate.postForObject(
                    "https://graph.facebook.com/v20.0/" + pageId + "/photos", photoEntity, java.util.Map.class);

            if (photoResponse == null || !photoResponse.containsKey("id")) {
                throw new RuntimeException("無法取得照片素材 ID，回應內容為空或異常。");
            }

            String photoId = (String) photoResponse.get("id");
            System.out.println("【步驟 1】成功，取得照片素材 ID: " + photoId);

            // 3. STEP 2: 用 photoId 去 /feed 建立標準排程貼文
            MultiValueMap<String, String> feedParams = new LinkedMultiValueMap<>();

            // 如果從 txt 讀出的內文為空，就採用預設罐頭文案
            String finalMessage = (caption != null && !caption.trim().isEmpty()) ? caption : "Greta Chiu AI 生成藝術日常補完 🤖✨";
            feedParams.add("message", finalMessage);

            feedParams.add("published", "false");
            feedParams.add("scheduled_publish_time", String.valueOf(nextScheduledTimestamp));
            String attachedMediaJson = "[{\"media_fbid\":\"" + photoId + "\"}]";
            feedParams.add("attached_media", attachedMediaJson);

            feedParams.add("access_token", pageAccessToken);

            HttpHeaders feedHeaders = new HttpHeaders();
            feedHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            HttpEntity<MultiValueMap<String, String>> feedEntity = new HttpEntity<>(feedParams, feedHeaders);

            System.out.println("【步驟 2】正在向 /feed 綁定照片並建立排程...");
            String feedResponse = restTemplate.postForObject(
                    "https://graph.facebook.com/v20.0/" + pageId + "/feed", feedEntity, String.class);

            System.out.println("🎉 機器人自動接龍排程成功！Facebook API 回應: " + feedResponse);

        } catch (Exception e) {
            System.err.println("❌ 自動化排程流水線發生錯誤：");
            e.printStackTrace();
            // 關鍵點：一定要向上拋出錯誤，讓呼叫此方法的 BotService 能夠進 catch 區塊阻止移動檔案
            throw e;
        }
    }

    /**
     * 保持向下相容：如果舊的 Controller 還有呼叫舊的無 caption 方法，自動導向預設內文
     */
    public void publishPerfectScheduledPost(String fileAbsolutePath) {
        try {
            publishPerfectScheduledPostWithCaption(fileAbsolutePath, null);
        } catch (Exception e) {
            System.err.println("調用舊版完美發文方法出錯。");
        }
    }

    public void publishScheduledPostWithPhoto() {
        try {
            LocalDateTime localDateTime = LocalDateTime.of(2026, 6, 1, 14, 0, 0);
            long unixTimestamp = localDateTime.atZone(ZoneId.of("Asia/Taipei")).toEpochSecond();

            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("caption", "這是透過 Java RestTemplate 自動排程發送的【圖文貼文】！📸");
            params.add("url", "https://meee.com.tw/CwPV3Ly");
            params.add("published", "false");
            params.add("scheduled_publish_time", String.valueOf(unixTimestamp));
            params.add("access_token", pageAccessToken);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(params, headers);

            String url = "https://graph.facebook.com/v20.0/" + pageId + "/photos";
            System.out.println("正在發送【圖文排程】請求至 Facebook API...");

            String response = restTemplate.postForObject(url, requestEntity, String.class);
            System.out.println("Facebook API 回應內容: " + response);

        } catch (Exception e) {
            System.err.println("呼叫 Facebook API 發生錯誤：");
            e.printStackTrace();
        }
    }

    public void publishScheduledPost() {
        try {
            LocalDateTime localDateTime = LocalDateTime.of(2026, 6, 1, 13, 0, 0);
            long unixTimestamp = localDateTime.atZone(ZoneId.of("Asia/Taipei")).toEpochSecond();

            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("message", "這是透過 Java RestTemplate 自動排程發送的測試貼文！2");
            params.add("published", "false");
            params.add("scheduled_publish_time", String.valueOf(unixTimestamp));
            params.add("access_token", pageAccessToken);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(params, headers);

            String url = "https://graph.facebook.com/v20.0/" + pageId + "/feed";
            System.out.println("正在透過 RestTemplate 發送排程請求至 Facebook API...");

            String response = restTemplate.postForObject(url, requestEntity, String.class);
            System.out.println("Facebook API 回應內容: " + response);

        } catch (Exception e) {
            System.err.println("呼叫 Facebook API 發生錯誤：");
            e.printStackTrace();
        }
    }

    public void getScheduledPosts() {
        try {
            System.out.println("pageId:" + pageId);
            System.out.println("pageAccessToken:" + pageAccessToken);
            String url = String.format(
                    "https://graph.facebook.com/v20.0/%s/scheduled_posts?fields=id,message,scheduled_publish_time&access_token=%s",
                    pageId, pageAccessToken
            );

            System.out.println("正在發送請求查詢已排程文章...");

            String response = restTemplate.getForObject(url, String.class);

            System.out.println("====== 目前已排程文章清單 ======");
            System.out.println(response);
            System.out.println("================================");

        } catch (Exception e) {
            System.err.println("查詢排程文章發生錯誤：");
            e.printStackTrace();
        }
    }
}