package com.saeanyang.management.controller;

import com.saeanyang.management.model.BulletinData;
import com.saeanyang.management.service.ExcelReaderService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Controller
@RequestMapping
public class BulletinController {

    private final ExcelReaderService excelReaderService;

    @Value("${bulletin.excel.path}")
    private String excelFilePath;

    @Value("${bulletin.logo.path}")
    private String logoFilePath;

    @Value("${bulletin.illustration.folder}")
    private String illustrationFolder;

    public BulletinController(ExcelReaderService excelReaderService) {
        this.excelReaderService = excelReaderService;
    }

    @GetMapping({"/", "/bulletin"})
    public String redirectRoot() {
        return "redirect:/api/bulletin";
    }

    @GetMapping("/api/bulletin")
    public String showBulletin(Model model) {
        try {
            BulletinData bulletinData = excelReaderService.readBulletinData(excelFilePath);
            model.addAttribute("bulletin", bulletinData);
        } catch (IOException e) {
            model.addAttribute("error", "엑셀 파일을 읽을 수 없습니다: " + e.getMessage());
        }
        return "weekly-bulletin";
    }

    /** ===== 로고 ===== */
    @GetMapping("/api/bulletin/logo")
    public ResponseEntity<Resource> getLogo() throws IOException {
        File logoFile = new File(logoFilePath);

        if (!logoFile.exists()) {
            return ResponseEntity.notFound().build();
        }

        return buildImageResponse(logoFile);
    }

    /** ===== 일러스트 (날짜 기반 선택) ===== */
    @GetMapping("/api/bulletin/illustration")
    public ResponseEntity<Resource> getIllustration() throws IOException {
        File folder = new File(illustrationFolder);

        if (!folder.exists() || !folder.isDirectory()) {
            return ResponseEntity.notFound().build();
        }

        File[] imageFiles = folder.listFiles((dir, name) -> {
            String lower = name.toLowerCase();
            return lower.endsWith(".png") || lower.endsWith(".jpg")
                    || lower.endsWith(".jpeg") || lower.endsWith(".gif")
                    || lower.endsWith(".svg");
        });

        if (imageFiles == null || imageFiles.length == 0) {
            return ResponseEntity.notFound().build();
        }

        List<File> sortedFiles = Arrays.asList(imageFiles);
        Collections.sort(sortedFiles);

        int day = LocalDate.now().getDayOfMonth();
        File selected = sortedFiles.get(day % sortedFiles.size());

        return buildImageResponse(selected);
    }

    /** ===== 공통 이미지 응답 빌더 ===== */
    private ResponseEntity<Resource> buildImageResponse(File file) throws IOException {
        Resource resource = new FileSystemResource(file);
        String contentType = Files.probeContentType(file.toPath());

        if (contentType == null) {
            contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + file.getName() + "\"")
                .body(resource);
    }
}
