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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class FacebookService {

    @Autowired
    private RestTemplate restTemplate;

    @Value("${facebook.page-id}")
    private String pageId;

    @Value("${facebook.page-access-token}")
    private String pageAccessToken;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // ✅ 新增：多圖排程發文
    public void publishScheduledPostWithMultiplePhotos(List<String> imagePaths, String caption) throws Exception {
        try {
            long nextScheduledTimestamp = calculateNextSlot();

            if (nextScheduledTimestamp == 0) {
                throw new RuntimeException("時間已超過29天 隔天再試");
            }

            // 步驟 1：逐一上傳圖片取得 photo_id
            List<String> photoIds = new ArrayList<>();
            for (int i = 0; i < imagePaths.size(); i++) {
                String imagePath = imagePaths.get(i);
                System.out.println("【步驟 1-" + (i + 1) + "】正在上傳圖片: " + imagePath);

                MultiValueMap<String, Object> uploadParams = new LinkedMultiValueMap<>();
                uploadParams.add("source", new FileSystemResource(imagePath));
                uploadParams.add("published", "false");
                uploadParams.add("temporary", "true");
                uploadParams.add("access_token", pageAccessToken);

                HttpHeaders uploadHeaders = new HttpHeaders();
                uploadHeaders.setContentType(MediaType.MULTIPART_FORM_DATA);

                HttpEntity<MultiValueMap<String, Object>> uploadEntity =
                        new HttpEntity<>(uploadParams, uploadHeaders);

                String uploadUrl = "https://graph.facebook.com/v20.0/" + pageId + "/photos";
                String uploadResponse = restTemplate.postForObject(uploadUrl, uploadEntity, String.class);

                JsonNode uploadJson = objectMapper.readTree(uploadResponse);
                String photoId = uploadJson.get("id").asText();
                photoIds.add(photoId);
                System.out.println("【步驟 1-" + (i + 1) + "】✅ 上傳成功，photo_id: " + photoId);
            }

            System.out.println("📸 共上傳 " + photoIds.size() + " 張，準備建立排程貼文...");

            // 步驟 2：組成 attached_media JSON 字串並發文
            StringBuilder attachedMediaJson = new StringBuilder("[");
            for (int i = 0; i < photoIds.size(); i++) {
                attachedMediaJson.append("{\"media_fbid\":\"").append(photoIds.get(i)).append("\"}");
                if (i < photoIds.size() - 1) attachedMediaJson.append(",");
            }
            attachedMediaJson.append("]");

            String finalMessage = (caption != null && !caption.trim().isEmpty()) ? caption : "日常補完 🤖✨";

            MultiValueMap<String, String> feedParams = new LinkedMultiValueMap<>();
            feedParams.add("message", finalMessage);
            feedParams.add("attached_media", attachedMediaJson.toString());
            feedParams.add("published", "false");
            feedParams.add("scheduled_publish_time", String.valueOf(nextScheduledTimestamp));
            feedParams.add("access_token", pageAccessToken);

            HttpHeaders feedHeaders = new HttpHeaders();
            feedHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            HttpEntity<MultiValueMap<String, String>> feedEntity = new HttpEntity<>(feedParams, feedHeaders);

            System.out.println("【步驟 2】正在向 /feed 綁定所有照片並建立排程...");
            String feedResponse = restTemplate.postForObject(
                    "https://graph.facebook.com/v20.0/" + pageId + "/feed", feedEntity, String.class);

            JsonNode feedJson = objectMapper.readTree(feedResponse);
            String postId = feedJson.get("id").asText();

            LocalDateTime scheduledLocalTime = LocalDateTime.ofInstant(
                    Instant.ofEpochSecond(nextScheduledTimestamp), ZoneId.of("Asia/Taipei"));
            System.out.println("🎉 多圖排程發文成功！貼文 ID: " + postId);
            System.out.println("   排程時間 (台北時間): " + scheduledLocalTime);
            System.out.println("   附圖數量: " + photoIds.size() + " 張");

        } catch (Exception e) {
            System.err.println("❌ 多圖排程發文流水線發生錯誤：");
            e.printStackTrace();
            throw e;
        }
    }

    // ====== 以下原有 method 全部保留不動 ======

    public void publishScheduledPostWithLocalPhoto() {
        try {
            LocalDateTime localDateTime = LocalDateTime.of(2026, 5, 24, 14, 0, 0);
            long unixTimestamp = localDateTime.atZone(ZoneId.of("Asia/Taipei")).toEpochSecond();

            MultiValueMap<String, Object> params = new LinkedMultiValueMap<>();
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

    public void publishPostToFeedWithPhoto(String message, String imagePath) {
        try {
            ObjectMapper mapper = new ObjectMapper();

            System.out.println("【步驟 1】正在上傳圖片...");
            MultiValueMap<String, Object> uploadParams = new LinkedMultiValueMap<>();
            uploadParams.add("source", new FileSystemResource(imagePath));
            uploadParams.add("published", "false");
            uploadParams.add("access_token", pageAccessToken);

            HttpHeaders uploadHeaders = new HttpHeaders();
            uploadHeaders.setContentType(MediaType.MULTIPART_FORM_DATA);
            HttpEntity<MultiValueMap<String, Object>> uploadEntity = new HttpEntity<>(uploadParams, uploadHeaders);

            String uploadUrl = "https://graph.facebook.com/v25.0/" + pageId + "/photos";
            String uploadResponse = restTemplate.postForObject(uploadUrl, uploadEntity, String.class);

            JsonNode uploadJson = mapper.readTree(uploadResponse);
            String photoId = uploadJson.get("id").asText();
            System.out.println("【步驟 1】✅ 圖片上傳成功，photo_id: " + photoId);

            System.out.println("【步驟 2】正在發文至動態牆...");
            String feedUrl = "https://graph.facebook.com/v25.0/" + pageId + "/feed";

            HttpHeaders feedHeaders = new HttpHeaders();
            feedHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            Map<String, String> mediaItem = new HashMap<>();
            mediaItem.put("media_fbid", photoId);
            List<Map<String, String>> attachedMedia = new ArrayList<>();
            attachedMedia.add(mediaItem);
            String attachedMediaJson = mapper.writeValueAsString(attachedMedia);

            MultiValueMap<String, String> feedBody = new LinkedMultiValueMap<>();
            feedBody.add("message", message);
            feedBody.add("attached_media", attachedMediaJson);
            feedBody.add("published", "true");
            feedBody.add("access_token", pageAccessToken);

            HttpEntity<MultiValueMap<String, String>> feedEntity = new HttpEntity<>(feedBody, feedHeaders);
            String feedResponse = restTemplate.postForObject(feedUrl, feedEntity, String.class);

            JsonNode feedJson = mapper.readTree(feedResponse);
            String postId = feedJson.get("id").asText();
            System.out.println("【步驟 2】✅ 動態牆發文成功，貼文 ID: " + postId);
        } catch (Exception e) {
            System.err.println("❌ 發文失敗：");
            e.printStackTrace();
        }
    }

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
                        if (postTime > latestTimestamp) latestTimestamp = postTime;
                    }
                }
            }

            if (latestTimestamp > 0) {
                targetTimestamp = latestTimestamp + 86400;
                //targetTimestamp = latestTimestamp + 20000;
                LocalDateTime nextTimeStr = LocalDateTime.ofInstant(
                        Instant.ofEpochSecond(targetTimestamp), ZoneId.of("Asia/Taipei"));
                System.out.println("📌 發現既有排程！將承接最後一篇，排程時間設定為 (台北時間): " + nextTimeStr);

                long now = System.currentTimeMillis() / 1000;
                long minAllowed = now + 600;
                long maxAllowed = now + (29L * 24 * 60 * 60);

                if (targetTimestamp < minAllowed) {
                    System.out.println("⚠️ 排程時間已過期，重設為現在起 10 分鐘後");
                    targetTimestamp = minAllowed;
                }
                if (targetTimestamp > maxAllowed) {
                    System.out.println("⚠️ 排程時間超過上限，截斷為 29 天後");
                    targetTimestamp = 0;
                }
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

    public void publishPerfectScheduledPostWithCaption(String fileAbsolutePath, String caption) throws Exception {
        try {
            long nextScheduledTimestamp = calculateNextSlot();
            if (nextScheduledTimestamp == 0) throw new RuntimeException("時間已超過29天 隔天再試");

            MultiValueMap<String, Object> photoParams = new LinkedMultiValueMap<>();
            Resource imageFile = new FileSystemResource(fileAbsolutePath);
            photoParams.add("source", imageFile);
            photoParams.add("published", "false");
            photoParams.add("access_token", pageAccessToken);

            HttpHeaders photoHeaders = new HttpHeaders();
            photoHeaders.setContentType(MediaType.MULTIPART_FORM_DATA);
            HttpEntity<MultiValueMap<String, Object>> photoEntity = new HttpEntity<>(photoParams, photoHeaders);

            System.out.println("【步驟 1】正在上傳實體圖檔至 Meta 伺服器...");
            Map photoResponse = restTemplate.postForObject(
                    "https://graph.facebook.com/v20.0/" + pageId + "/photos", photoEntity, Map.class);

            if (photoResponse == null || !photoResponse.containsKey("id")) {
                throw new RuntimeException("無法取得照片素材 ID，回應內容為空或異常。");
            }

            String photoId = (String) photoResponse.get("id");
            System.out.println("【步驟 1】成功，取得照片素材 ID: " + photoId);

            String finalMessage = (caption != null && !caption.trim().isEmpty()) ? caption : "日常補完 🤖✨";
            MultiValueMap<String, String> feedParams = new LinkedMultiValueMap<>();
            feedParams.add("message", finalMessage);
            feedParams.add("published", "false");
            feedParams.add("scheduled_publish_time", String.valueOf(nextScheduledTimestamp));
            feedParams.add("attached_media", "[{\"media_fbid\":\"" + photoId + "\"}]");
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
            throw e;
        }
    }

    public long getLatestScheduledTimestamp() {
        try {
            String url = String.format(
                    "https://graph.facebook.com/v20.0/%s/scheduled_posts?fields=scheduled_publish_time&access_token=%s",
                    pageId, pageAccessToken
            );
            String jsonResponse = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(jsonResponse);
            JsonNode dataArray = root.get("data");

            long latestTimestamp = 0;
            if (dataArray != null && dataArray.isArray()) {
                for (JsonNode post : dataArray) {
                    if (post.has("scheduled_publish_time")) {
                        long postTime = post.get("scheduled_publish_time").asLong();
                        if (postTime > latestTimestamp) latestTimestamp = postTime;
                    }
                }
            }
            return latestTimestamp;

        } catch (Exception e) {
            System.err.println("❌ 取得最後排程時間失敗：" + e.getMessage());
            // fallback：用現在時間當基準
            return System.currentTimeMillis() / 1000;
        }
    }

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