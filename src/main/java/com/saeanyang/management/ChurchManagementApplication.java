package com.saeanyang.management;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;

import java.awt.*;
import java.net.URI;

@SpringBootApplication
public class ChurchManagementApplication {

    private static ConfigurableApplicationContext context;
    private static final String APP_URL = "http://localhost:8082/bulletin";

    public static void main(String[] args) {
        // AWT가 초기화되기 전에 반드시 설정해야 트레이/파일다이얼로그가 동작한다
        System.setProperty("java.awt.headless", "false");

        // macOS 독(Dock)에서 앱 이름 표시 (다른 OS에서는 무시됨)
        if (System.getProperty("os.name", "").toLowerCase().contains("mac")) {
            System.setProperty("apple.awt.application.name", "ChurchManagement");
        }

        // .env 파일 로드
        Dotenv dotenv = Dotenv.configure()
                .ignoreIfMissing()
                .load();

        dotenv.entries().forEach(entry ->
            System.setProperty(entry.getKey(), entry.getValue())
        );

        context = SpringApplication.run(ChurchManagementApplication.class, args);
    }

    /** 서버 시작 완료 후 브라우저 열기 + 트레이 아이콘 등록 */
    @Bean
    public ApplicationRunner appStartedRunner() {
        return args -> {
            openBrowser(APP_URL);
            setupSystemTray();
        };
    }

    private static void openBrowser(String url) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(url));
            }
        } catch (Exception e) {
            System.err.println("브라우저 자동 실행 실패: " + e.getMessage());
        }
    }

    private static void setupSystemTray() {
        if (!SystemTray.isSupported()) {
            System.out.println("시스템 트레이를 지원하지 않는 환경입니다.");
            return;
        }

        // 트레이 아이콘 이미지 생성 (16x16 단색 원)
        java.net.URL iconUrl = ChurchManagementApplication.class.getResource("/static/tray-icon.png");
        Image iconImage = (iconUrl != null)
            ? Toolkit.getDefaultToolkit().createImage(iconUrl)
            : createDefaultTrayIcon();

        PopupMenu menu = new PopupMenu();

        MenuItem openItem = new MenuItem("주보 열기");
        openItem.addActionListener(e -> openBrowser(APP_URL));

        MenuItem separatorItem = new MenuItem("-");

        MenuItem exitItem = new MenuItem("종료");
        exitItem.addActionListener(e -> {
            SystemTray.getSystemTray().remove(
                SystemTray.getSystemTray().getTrayIcons().length > 0
                    ? SystemTray.getSystemTray().getTrayIcons()[0] : null
            );
            int code = SpringApplication.exit(context, () -> 0);
            System.exit(code);
        });

        menu.add(openItem);
        menu.add(separatorItem);
        menu.add(exitItem);

        TrayIcon trayIcon = new TrayIcon(iconImage, "ChurchManagement", menu);
        trayIcon.setImageAutoSize(true);
        trayIcon.addActionListener(e -> openBrowser(APP_URL)); // 더블클릭

        try {
            SystemTray.getSystemTray().add(trayIcon);
        } catch (AWTException e) {
            System.err.println("트레이 아이콘 등록 실패: " + e.getMessage());
        }
    }

    /** 리소스 파일이 없을 때 사용할 기본 트레이 아이콘 (16x16 녹색 원) */
    private static Image createDefaultTrayIcon() {
        int size = 16;
        java.awt.image.BufferedImage img =
            new java.awt.image.BufferedImage(size, size, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(new Color(76, 175, 80));
        g2.fillOval(1, 1, size - 2, size - 2);
        g2.dispose();
        return img;
    }
}
