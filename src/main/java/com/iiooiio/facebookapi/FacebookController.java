package com.iiooiio.facebookapi;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/fb")
public class FacebookController {

    @Autowired
    private FacebookService facebookService;

    // 設定成 POST 請求
    @PostMapping("/publish")
    public String triggerPublish() {
        try {
            facebookService.publishScheduledPost();
            return "發送請求完成！請看 IDE 控制台日誌。";
        } catch (Exception e) {
            return "觸發失敗: " + e.getMessage();
        }
    }

    @PostMapping("/publishWithPhotos")
    public String triggerPublishWithPhotos() {
        try {
            facebookService.publishScheduledPostWithPhoto();
            return "發送請求完成！請看 IDE 控制台日誌。";
        } catch (Exception e) {
            return "觸發失敗: " + e.getMessage();
        }
    }

    @GetMapping("/publishWithLocalPhotos")
    public String triggerPublishWithLocalPhotos() {
        try {
            facebookService.publishScheduledPostWithLocalPhoto();
            return "發送請求完成！請看 IDE 控制台日誌。";
        } catch (Exception e) {
            return "觸發失敗: " + e.getMessage();
        }
    }

    @GetMapping("/publishImmediatePostWithLocalPhoto")
    public String publishImmediatePostWithLocalPhoto() {
        try {
            facebookService.publishPostToFeedWithPhoto(
                    "happy days!",
                    "C:\\Users\\test_.png"
            );;
            return "發送請求完成！請看 IDE 控制台日誌。";
        } catch (Exception e) {
            return "觸發失敗: " + e.getMessage();
        }
    }

    @GetMapping ("/scheduledList")
    public String scheduledList() {
        try {
            facebookService.getScheduledPosts();
            return "發送請求完成！請看 IDE 控制台日誌。";
        } catch (Exception e) {
            return "觸發失敗: " + e.getMessage();
        }
    }
}