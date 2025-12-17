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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

@Controller
public class BulletinController {

    private final ExcelReaderService excelReaderService;

    @Value("${bulletin.excel.path}")
    private String excelFilePath;

    @Value("${bulletin.logo.path}")
    private String logoFilePath;

    public BulletinController(ExcelReaderService excelReaderService) {
        this.excelReaderService = excelReaderService;
    }

    @GetMapping("/bulletin")
    public String showBulletin(Model model) {
        try {
            BulletinData bulletinData = excelReaderService.readBulletinData(excelFilePath);
            model.addAttribute("bulletin", bulletinData);
        } catch (IOException e) {
            model.addAttribute("error", "엑셀 파일을 읽을 수 없습니다: " + e.getMessage());
        }
        return "weekly-bulletin";
    }

    @GetMapping("/logo.png")
    public ResponseEntity<Resource> getLogo() throws IOException {
        File logoFile = new File(logoFilePath);

        if (!logoFile.exists()) {
            return ResponseEntity.notFound().build();
        }

        Resource resource = new FileSystemResource(logoFile);
        String contentType = Files.probeContentType(logoFile.toPath());

        if (contentType == null) {
            contentType = "image/png";
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"logo.png\"")
                .body(resource);
    }
}
