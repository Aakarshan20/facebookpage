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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
public class FacebookBotService implements CommandLineRunner {

    @Autowired
    private FacebookService facebookService;

    @Value("${facebook.bot.todo-dir:./workspace/todo}")
    private String todoDir;

    @Value("${facebook.bot.done-dir:./workspace/done}")
    private String doneDir;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    private final List<String> defaultCaptions = Arrays.asList(
            "日常補完 🤖✨ 每天都要來點不一樣的靈感刺激！ #上班族日常",
            "今日份的快樂已送達 ☕ 生活再忙，也別忘了笑一笑。 #日常系列",
            "看來今天又是一個適合放空的日子 🎨✨  #美好的一天",
            "隨機捕捉一隻出沒在虛擬世界的 美少女 🤖 #AIArt #酷妹日常",
            "下午茶時間過後... 靈感突然噴發的產物 🔮 不管怎樣先發文再說！ #發文挑戰",

            "平凡的日子，也值得好好收藏 📸✨ #日常紀錄 #生活點滴",
            "有些瞬間不用特別解釋，因為美好本身就是答案 🌸💫 #今日分享 #美好時刻",
            "今天也努力把快樂存進回憶裡 🫶✨ #開心日常 #生活日記",
            "留下一點屬於今天的小紀錄，希望未來回頭看依然會微笑 🌈📖 #日常隨拍 #紀錄生活",
            "每一天都有不同的故事，而今天也不例外 🌟📷 #今日份 #生活靈感",
            "分享一個平凡卻讓人心情變好的小片段 ☀️💛 #日常分享 #正能量",
            "偶爾放慢腳步，才發現生活藏著許多小驚喜 🍃✨ #慢生活 #療癒時刻",
            "新的一天，新的靈感，新的期待 🚀💡 #靈感日常 #探索生活",
            "隨手記錄當下，因為每個瞬間都值得被珍藏 📷💕 #美好日常 #生活紀錄",
            "不管今天過得如何，都別忘了替自己按個讚 👍✨ #每天都要開心 #KeepGoing",
            "只是簡單分享，但希望能替你帶來一點好心情 😊🌸 #分享日常 #療癒一下",
            "生活沒有標準答案，照著自己的步調前進就好 🌿✨ #生活哲學 #日常隨想",
            "又完成了一天的小小冒險，繼續期待下一個精彩時刻 🌙⭐ #日常更新 #美好生活",
            "每一次按下快門，都是為了留下此刻的溫度 📸❤️ #隨手拍 #回憶收藏",
            "把今天的心情打包分享給你，希望你也有美好的一天 🎁☀️ #GoodVibes #DailyLife"
    );

    private final Random random = new Random();

    @Override
    public void run(String... args) throws Exception {
        System.out.println("🤖 Facebook 自動發文機器人已啟動，每 20 秒監聽一次資料夾...");
        System.out.println("📂 目前監聽 todo 路徑: " + todoDir);
        System.out.println("📂 目前歸檔 done 路徑: " + doneDir);
        scheduler.scheduleWithFixedDelay(this::watchAndPublish, 5, 20, TimeUnit.SECONDS);
    }

    private void watchAndPublish() {
        try {
            File todoFolder = new File(todoDir);
            if (!todoFolder.exists() || !todoFolder.isDirectory()) {
                System.out.println("⚠️ 找不到監聽的 todo 資料夾，請確認路徑是否存在: " + todoDir);
                return;
            }

            // ========== 模式一：優先處理子資料夾（多圖貼文）==========
            File[] subFolders = todoFolder.listFiles(File::isDirectory);
            if (subFolders != null && subFolders.length > 0) {
                File subFolder = subFolders[0];
                System.out.println("📂 偵測到子資料夾: " + subFolder.getName() + "，進入多圖貼文模式...");

                // 取得資料夾內所有 png
                File[] pngFiles = subFolder.listFiles((dir, name) -> name.toLowerCase().endsWith(".png"));
                if (pngFiles == null || pngFiles.length == 0) {
                    System.out.println("⚠️ 資料夾 " + subFolder.getName() + " 內無 PNG 檔案，跳過並移至 done。");
                    moveFileToDone(subFolder);
                    return;
                }

                // 檢查是否有圖片還在寫入
                for (File png : pngFiles) {
                    if (isFileLocked(png)) {
                        System.out.println("⏳ 檔案 " + png.getName() + " 還在寫入中，跳過本次處理。");
                        return;
                    }
                }

                // 取得 txt 文案（不限制檔名，直接取第一個 txt）
                String caption = "";
                File[] txtFiles = subFolder.listFiles((dir, name) -> name.toLowerCase().endsWith(".txt"));
                if (txtFiles != null && txtFiles.length > 0) {
                    try {
                        byte[] encoded = Files.readAllBytes(txtFiles[0].toPath());
                        caption = new String(encoded, StandardCharsets.UTF_8);
                        System.out.println("📖 成功讀取資料夾文案 [" + txtFiles[0].getName() + "]");
                    } catch (IOException e) {
                        System.err.println("❌ 讀取文字檔失敗，將改用隨機預設文案。");
                    }
                }

                if (caption == null || caption.trim().isEmpty()) {
                    int randomIndex = random.nextInt(defaultCaptions.size());
                    caption = defaultCaptions.get(randomIndex);
                    System.out.println("🎲 未偵測到有效的 .txt，已自動隨機抽選第 " + (randomIndex + 1) + " 組預設文案。");
                }

                // 組成圖片路徑清單
                List<String> imagePaths = new ArrayList<>();
                for (File png : pngFiles) {
                    imagePaths.add(png.getAbsolutePath());
                }

                System.out.println("🚀 準備發送資料夾 [" + subFolder.getName() + "] 共 " + imagePaths.size() + " 張圖至 Facebook 排程...");
                facebookService.publishScheduledPostWithMultiplePhotos(imagePaths, caption);

                // 整個資料夾移至 done
                moveFileToDone(subFolder);
                System.out.println("✅ 資料夾 [" + subFolder.getName() + "] 處理完畢，已移至已處理區。");
                return;
            }

            // ========== 模式二：處理單一 PNG（原本邏輯不動）==========
            File[] pngFiles = todoFolder.listFiles((dir, name) -> name.toLowerCase().endsWith(".png"));
            if (pngFiles == null || pngFiles.length == 0) {
                return;
            }

            System.out.println("🔍 偵測到 todo 資料夾有 " + pngFiles.length + " 個待處理圖片...");

            File pngFile = pngFiles[0];
            String fileNameWithoutExt = getFileNameWithoutExtension(pngFile);
            File txtFile = new File(todoDir + File.separator + fileNameWithoutExt + ".txt");

            if (isFileLocked(pngFile)) {
                System.out.println("⏳ 檔案 " + pngFile.getName() + " 似乎還在寫入中，跳過本次處理。");
                return;
            }

            String caption = "";
            if (txtFile.exists() && txtFile.isFile()) {
                try {
                    byte[] encoded = Files.readAllBytes(txtFile.toPath());
                    caption = new String(encoded, StandardCharsets.UTF_8);
                    System.out.println("📖 成功讀取本地文案 [" + txtFile.getName() + "]");
                } catch (IOException e) {
                    System.err.println("❌ 讀取文字檔失敗: " + txtFile.getName() + "，將改用隨機預設文案。");
                }
            }

            if (caption == null || caption.trim().isEmpty()) {
                int randomIndex = random.nextInt(defaultCaptions.size());
                caption = defaultCaptions.get(randomIndex);
                System.out.println("🎲 未偵測到有效的 .txt 檔案，已自動隨機抽選第 " + (randomIndex + 1) + " 組預設文案。");
            }

            System.out.println("🚀 準備發送圖片 " + pngFile.getName() + " 至 Facebook 排程...");
            facebookService.publishPerfectScheduledPostWithCaption(pngFile.getAbsolutePath(), caption);

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

    private String getFileNameWithoutExtension(File file) {
        String name = file.getName();
        int lastIndexOf = name.lastIndexOf(".");
        if (lastIndexOf == -1) return name;
        return name.substring(0, lastIndexOf);
    }

    private void moveFileToDone(File sourceFile) {
        try {
            Path targetDir = Paths.get(doneDir);
            if (!Files.exists(targetDir)) {
                Files.createDirectories(targetDir);
            }
            Path targetPath = targetDir.resolve(sourceFile.getName());
            Files.move(sourceFile.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            System.err.println("❌ 移動檔案失敗: " + sourceFile.getName());
            e.printStackTrace();
        }
    }

    private boolean isFileLocked(File file) {
        long oldSize = file.length();
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        long newSize = file.length();
        return oldSize != newSize || newSize == 0;
    }
}