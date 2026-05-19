package com.iiooiio.facebookapi;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import java.nio.file.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class ComfyUiFolderWatcher implements CommandLineRunner {

    @Autowired
    private FacebookService facebookService;

    // 設定你要監聽的 ComfyUI 產圖目錄
    private final String WATCH_PATH = "C:/data/ComfyUI/output";

    @Override
    public void run(String... args) throws Exception {
        System.out.println("🚀 啟動 ComfyUI 檔案監聽服務，目標目錄: " + WATCH_PATH);

        // 使用執行緒池監聽，避免阻塞 Spring Boot 主執行緒
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            try {
                WatchService watchService = FileSystems.getDefault().newWatchService();
                Path path = Paths.get(WATCH_PATH);
                path.register(watchService, StandardWatchEventKinds.ENTRY_CREATE);

                while (true) {
                    WatchKey key = watchService.take(); // 阻塞等待新檔案
                    for (WatchEvent<?> event : key.pollEvents()) {
                        WatchEvent.Kind<?> kind = event.kind();

                        if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
                            Path fileName = (Path) event.context();
                            String fullPath = WATCH_PATH + "/" + fileName.toString();

                            // 排除 ComfyUI 產圖過程中可能產生的暫存檔，只抓 png/jpg
                            if (fullPath.endsWith(".png") || fullPath.endsWith(".jpg")) {
                                System.out.println("\n🔥 偵測到 ComfyUI 產生新圖片: " + fileName);

                                // 消除檔案寫入的時間差，安全延時 2 秒確保 AI 已經完全存檔
                                Thread.sleep(2000);

                                // 一棒入魂：直接觸發排程發文邏輯！
                                facebookService.publishPerfectScheduledPost(fullPath);
                            }
                        }
                    }
                    if (!key.reset()) break;
                }
            } catch (Exception e) {
                System.err.println("監聽服務發生異常: " + e.getMessage());
            }
        });
    }
}