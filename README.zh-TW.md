# Gree PC Controller

[English](README.md) | [繁體中文](README.zh-TW.md)

適用於相容格力（Gree）Wi-Fi 冷氣機的本機瀏覽器控制面板，包含 Windows 啟動程式及 Java REST 後端。

## 控制功能

- 開啟或關閉冷氣機。
- 設定 16–30°C 溫度，並透過明確的「Apply」按鈕套用。
- 調整運作模式、風速及上下擺風。
- 頁面可見時，每 15 秒讀取一次即時設定。
- 重新整理時保留尚未送出的修改；斷線時顯示最後確認的設定。
- 可選擇讓受信任家居網絡內的其他裝置使用。

指令使用 Gree 的 AES-GCM 協定進行驗證，面板亦會讀回設定以確認結果。部分模式及型號會限制風速、溫度、暖氣或擺風選項。

## 相容性

此改版已在一部使用 AES-ECB 探索回覆、並以 **AES-GCM 協定 v2** 進行綁定、狀態查詢及控制的 Gree Wi-Fi 冷氣機上測試。冷氣機必須已透過 GREE+ 或製造商應用程式連接 Wi-Fi。本程式不會替冷氣機設定 Wi-Fi，亦不是適用於所有 Gree 型號的通用驅動程式。此改版尚未實作純 ECB 控制及 GCM 探索。

## 在 Windows 建置及執行

安裝 **Java 8 JDK** 及 **Maven 3.5 或以上版本**，將 `JAVA_HOME` 設為 JDK 目錄，並讓 `PATH` 包含 Maven。

在儲存庫目錄執行：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\Build-Windows.ps1
```

建置會執行測試，並產生可獨立使用的 `dist` 資料夾，內含從你的 JDK 複製的 Java 執行環境。請一併保留該執行環境的授權及聲明檔案。

1. 按兩下 `dist\Start-Gree.cmd`。
2. 首次啟動時輸入冷氣機的本機 IPv4 位址，可在路由器的已連接裝置清單中查看。
3. 開啟[控制面板](http://127.0.0.1:8081/)。
4. 使用 `Stop-Gree.cmd` 停止電腦上的服務。這不會改變冷氣機的電源狀態。

你可以把整個 `dist` 資料夾移到其他位置。電腦必須保持喚醒，並與冷氣機位於同一網絡。本程式不會安裝開機啟動工作。如路由器更改了冷氣機位址，請更新 `ac-address.txt` 並重新啟動服務。

### 直接使用 Java 執行

```powershell
mvn -DforkCount=0 clean package
java -Dgree.address=YOUR_AC_IP -jar target/airconditioner-remote-1.0-SNAPSHOT.jar
```

在同一程序內執行測試可避開 Windows 上舊版 Surefire 的 fork 問題。API 文件位於 [Swagger UI](http://127.0.0.1:8081/swagger-ui.html)。

## 可選的家居網絡存取

預設**只監聽 127.0.0.1:8081**。如需區域網絡存取：

1. 在啟動程式旁建立 `lan-address.txt`，填入這部電腦的區域網絡 IPv4 位址。
2. 使用系統管理員 PowerShell，在同一資料夾執行 `Enable-LAN.ps1 -RemoteSubnet YOUR_HOME_SUBNET_CIDR`。例如，只有家居網絡確實相符時才使用 `192.168.0.0/24`。
3. 重新啟動控制程式，然後在該子網內的其他裝置開啟 `http://YOUR_PC_IP:8081/`。

可選的連接器會同時綁定指定的區域網絡位址及 localhost。防火牆規則僅適用於這個 Java 執行檔、本機位址、TCP 8081 連接埠及指定的遠端子網。清空 `lan-address.txt` 並重新啟動，即可停用區域網絡監聽。如電腦位址改變，請更新檔案，並使用正確子網重新執行防火牆腳本。

**這是一個較舊、未設存取驗證的 HTTP 服務。任何能透過網絡存取它的人都可以控制冷氣機。只應在受信任的本機網絡使用，切勿直接開放至互聯網。** 它沿用較舊的 Spring Boot 相依套件，並非經過強化的公開網頁服務。

## API

| 端點 | 方法 | 用途 |
| --- | --- | --- |
| `/status` | GET | 讀取冷氣機目前設定 |
| `/powerOn` | GET | 開啟冷氣機 |
| `/powerOff` | GET | 關閉冷氣機 |
| `/temperature?temperature=25` | GET | 設定攝氏溫度 |
| `/settings` | POST | 修改模式、風速及／或擺風 |

`/settings` 接受 URL 編碼的表單欄位，只修改有提供的設定：

- `mode`：`AUTO`、`COOL`、`DRY`、`FAN`、`HEAT`
- `fanSpeed`：`AUTO`、`LOW`、`MEDIUM_LOW`、`MEDIUM`、`MEDIUM_HIGH`、`HIGH`
- `swing`：整數 `0`–`11`

每次控制一部已設定的冷氣機。為相容舊版本，仍保留使用 GET 修改狀態的端點。不需要雲端登入資料或 GREE+ 帳戶 token。

## 驗證

Java 測試涵蓋探索序列化、指令傳輸值、韌體 metadata 相容性、獨立產生的 AES-GCM 測試向量、驗證標記拒絕，以及設定 API 輸入驗證。原有的實體裝置測試仍然略過。已在一部相容冷氣機上檢查即時綁定、狀態、溫度及設定指令的確認回覆；並未測試所有運作模式。

## 來源及授權狀態

以 [alexmuntean/gree-airconditioner-rest](https://github.com/alexmuntean/gree-airconditioner-rest) 的快照 `1afb96585857bd7655255f3a0d634751c6172437` 為基礎。原作者鳴謝 [tomikaa87/gree-remote](https://github.com/tomikaa87/gree-remote) 的協定研究。協定 v2 常數已與 [cmroche/greeclimate](https://github.com/cmroche/greeclimate/blob/master/greeclimate/cipher.py) 交叉核對；本專案的 Java AES-GCM 實作用到 JDK 密碼學 API。

修改包括瀏覽器 GUI、協定 v2 支援、額外韌體 metadata 處理、有上限的逾時、循序 UDP 指令、經驗證的控制回覆、模式／風速／擺風端點、Windows 打包及可選的區域網絡監聽。

上游快照沒有授權檔案。本儲存庫不會額外授予上游程式碼的使用授權。重新散佈或作商業用途前，請確認原作者的許可及相依套件授權。

下載的執行環境、編譯檔案、日誌、本機位址及裝置專屬設定均排除於 Git 之外。
