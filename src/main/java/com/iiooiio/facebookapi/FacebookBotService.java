package com.iiooiio.facebookapi;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
public class FacebookBotService implements CommandLineRunner {

    @Autowired
    private FacebookService facebookService;

    // ✨ 透過 @Value 動態讀取 properties 設定，若讀不到則以右側路徑保底
    @Value("${facebook.bot.todo-dir:./workspace/todo}")
    private String todoDir;

    @Value("${facebook.bot.done-dir:./workspace/done}")
    private String doneDir;

    // 建立單執行緒的排程執行器
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    @Override
    public void run(String... args) throws Exception {
        System.out.println("🤖 Facebook 自動發文機器人已啟動，每 20 秒監聽一次資料夾...");
        System.out.println("📂 目前監聽 todo 路徑: " + todoDir);
        System.out.println("📂 目前歸檔 done 路徑: " + doneDir);

        // 專案啟動後，延遲 5 秒開始執行，之後每隔 20 秒執行一次
        scheduler.scheduleWithFixedDelay(this::watchAndPublish, 5, 20, TimeUnit.SECONDS);
    }

    private void watchAndPublish() {
        try {
            File todoFolder = new File(todoDir);
            if (!todoFolder.exists() || !todoFolder.isDirectory()) {
                System.out.println("⚠️ 找不到監聽的 todo 資料夾，請確認路徑是否存在: " + todoDir);
                return;
            }

            // 1. 尋找資料夾底下的所有 .png 檔案
            File[] pngFiles = todoFolder.listFiles((dir, name) -> name.toLowerCase().endsWith(".png"));

            if (pngFiles == null || pngFiles.length == 0) {
                // 資料夾為空，安靜地結束，等待下個 20 秒
                return;
            }

            System.out.println("🔍 偵測到 todo 資料夾有 " + pngFiles.length + " 個待處理圖片...");

            // 每次循環只處理「第一張」圖片，確保按照排程列表依序接龍
            File pngFile = pngFiles[0];
            String fileNameWithoutExt = getFileNameWithoutExtension(pngFile);

            // 尋找同名的 .txt 檔案
            File txtFile = new File(todoDir + File.separator + fileNameWithoutExt + ".txt");

            // 安全機制：檢查檔案是否正在被 ComfyUI 寫入
            if (isFileLocked(pngFile)) {
                System.out.println("⏳ 檔案 " + pngFile.getName() + " 似乎還在寫入中，跳過本次處理。");
                return;
            }

            // 2. 讀取同名 .txt 檔案取得發文文案
            String caption = "";
            if (txtFile.exists() && txtFile.isFile()) {
                try {
                    byte[] encoded = Files.readAllBytes(txtFile.toPath());
                    caption = new String(encoded, StandardCharsets.UTF_8);
                    System.out.println("📖 成功讀取文案 [" + txtFile.getName() + "]");
                } catch (IOException e) {
                    System.err.println("❌ 讀取文字檔失敗: " + txtFile.getName() + "，將使用預設文案發送。");
                }
            } else {
                System.out.println("⚠️ 未找到同名文字檔 [" + fileNameWithoutExt + ".txt]，將以無內文形式發佈。");
            }

            // 3. 呼叫終極接龍流水線
            System.out.println("🚀 準備發送圖片 " + pngFile.getName() + " 至 Facebook 排程...");
            facebookService.publishPerfectScheduledPostWithCaption(pngFile.getAbsolutePath(), caption);

            // 4. 發送成功後，將檔案移至 done 資料夾
            moveFileToDone(pngFile);
            if (txtFile.exists()) {
                moveFileToDone(txtFile);
            }

            System.out.println("✅ " + fileNameWithoutExt + " 組件處理完畢，已移至已處理區。");

        } catch (Exception e) {
            System.err.println("❌ 機器人監聽循環發生異常：");
            e.printStackTrace();
        }
    }

    /**
     * 移除副檔名取得純檔名
     */
    private String getFileNameWithoutExtension(File file) {
        String name = file.getName();
        int lastIndexOf = name.lastIndexOf(".");
        if (lastIndexOf == -1) {
            return name;
        }
        return name.substring(0, lastIndexOf);
    }

    /**
     * 將檔案安全移動到 done 資料夾
     */
    private void moveFileToDone(File sourceFile) {
        try {
            Path targetDir = Paths.get(doneDir);
            if (!Files.exists(targetDir)) {
                Files.createDirectories(targetDir); // 如果 done 資料夾不存在就自動建立
            }

            Path targetPath = targetDir.resolve(sourceFile.getName());
            // 使用 REPLACE_EXISTING 確保安全覆蓋與移動
            Files.move(sourceFile.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            System.err.println("❌ 移動檔案失敗: " + sourceFile.getName());
            e.printStackTrace();
        }
    }

    /**
     * 檢查檔案是否還在被其他程序寫入中
     */
    private boolean isFileLocked(File file) {
        long oldSize = file.length();
        try {
            Thread.sleep(500); // 稍微等半秒鐘
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        long newSize = file.length();
        return oldSize != newSize || newSize == 0;
    }
}