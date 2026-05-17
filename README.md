Facebook 粉絲專頁自動化排程 API 系統

本專案為粉絲專頁自動化系統的發文端核心（Spring Boot）。透過整合 Meta Graph API 與本地排程邏輯，實現不重啟伺服器即可隨時手動觸發「排程貼文」與「查詢現有排程貼文」的功能。
🚀 快速開始
1. 環境設定

先將 src/main/resources/.application.properties 複製一份 並改名為 application.properties

確認 src/main/resources/application.properties 已正確配置以下參數：
Properties

# 伺服器埠號（預設為 8080）
server.port=8080

# Facebook 粉絲專頁配置
facebook.page-id=你的_PAGE_ID
facebook.page-access-token=你的_PAGE_ACCESS_TOKEN

    ⚠️ 注意：facebook.page-access-token 必須是具備 pages_manage_posts 與 pages_read_engagement 權限的 Page Access Token（粉絲專頁權杖），切勿填成 User Token。

2. 啟動專案

在 IntelliJ IDEA 中點擊 FacebookapiApplication 旁的綠色三角形啟動服務。
🛣️ API 介面與測試指令

為了避免頻繁重啟 Spring Boot 造成資源耗損，系統提供 HTTP 介面供開發者隨時透過命令列（Command Line）進行即時測試。
1. 觸發自動排程發文

    功能： 讀取下一張待發布圖片資訊，並向 Facebook API 發送排程貼文請求。

    路由： POST /api/fb/publish

    PowerShell 測試指令：
    PowerShell

    Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/fb/publish

    Git Bash / Linux cURL 測試指令：
    Bash

    curl -X POST http://localhost:8080/api/fb/publish

    預期回傳： "發送排程請求完成！"（詳細回應請至 IDE Console 控制台查看日誌）

2. 查詢目前已排程的文章清單

    功能： 向 Facebook 查詢目前粉專後台「已排程但尚未發布」的所有貼文列表、排程時間戳記與內容。

    路由： GET /api/fb/scheduled

    PowerShell 測試指令：
    PowerShell

    Invoke-RestMethod -Method Get -Uri http://localhost:8080/api/fb/scheduled

    Git Bash / Linux cURL 測試指令：
    Bash

    curl http://localhost:8080/api/fb/scheduled

    預期回傳： "查詢完成！請看 IDE 控制台日誌。"

🪵 控制台日誌說明 (IDE Console)

當你透過上述指令觸發 GET /api/fb/scheduled 查詢時，請回到 IntelliJ IDEA 的 Console 視窗，系統會印出格式如下的 Facebook 原生 JSON 資料：
JSON

====== 目前已排程文章清單 ======
{
  "data": [
    {
      "id": "1234567890_987654321",
      "message": "這是透過自動化系統排程發送的貼文！",
      "scheduled_publish_time": 1777694400
    }
  ]
}
================================

    💡 提示： 如果 data 欄位顯示為空陣列 []，代表目前後台沒有任何排程中的貼文。

🛠️ 常見錯誤排查 (Troubleshooting)

    錯誤 403 Forbidden (#200)：
    代表你的 Token 權限不足，或者不小心把 page-id 填成了「社團（Group）ID」或「個人用戶 ID」。請至 Meta 權杖檢查器確認 Scopes 包含 pages_manage_posts。

    錯誤 400 Bad Request (Invalid OAuth token)：
    Token 格式錯誤或已過期。請確認 application.properties 裡的 Token 前後沒有夾帶到無形空格，且必須是未經過手動 URL Encode 的原始字串。

    PowerShell 找不到 -X 參數：
    Windows 預設的 curl 是 Invoke-WebRequest 的別名。請務必使用本文件提供之 Invoke-RestMethod 指令，或改用 curl.exe -X POST ...
