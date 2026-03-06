package com.saeanyang.management.controller;

import com.saeanyang.management.model.BulletinData;
import com.saeanyang.management.model.CellGroup;
import com.saeanyang.management.model.EditableTextConfig;
import com.saeanyang.management.service.ExcelReaderService;
import com.saeanyang.management.service.TextConfigService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Controller
@RequestMapping
public class BulletinController {

    private final ExcelReaderService excelReaderService;
    private final TextConfigService textConfigService;

    @Value("${bulletin.excel.path}")
    private String excelFilePath;

    @Value("${bulletin.logo.path}")
    private String logoFilePath;

    @Value("${bulletin.illustration.folder}")
    private String illustrationFolder;

    public BulletinController(ExcelReaderService excelReaderService,
                              TextConfigService textConfigService) {
        this.excelReaderService = excelReaderService;
        this.textConfigService = textConfigService;
    }

    /** 텍스트 한 항목을 서버 쪽 설정 파일에 저장 */
    @PostMapping("/api/bulletin/text-config")
    public ResponseEntity<Void> updateText(@RequestBody TextUpdateRequest request) {
        if (request == null || request.key() == null) {
            return ResponseEntity.badRequest().build();
        }
        textConfigService.updateField(request.key(), request.value());
        return ResponseEntity.ok().build();
    }

    /** 텍스트 단일 항목 업데이트용 요청 바디 */
    public record TextUpdateRequest(String key, String value) {}

    @GetMapping("/bulletin")
    public String showBulletin(Model model) {
        try {
            BulletinData bulletinData = excelReaderService.readBulletinData(excelFilePath);
            // 서버에 저장된 텍스트 설정 반영
            EditableTextConfig textConfig = textConfigService.loadConfig();
            bulletinData.setHeadPastor(textConfig.getHeadPastor());
            bulletinData.setDirector(textConfig.getDirector());
            bulletinData.setAdvisors(textConfig.getAdvisors());
            bulletinData.setNewYouthLeader(textConfig.getNewYouthLeader());
            bulletinData.setWorshipLeader(textConfig.getWorshipLeader());

            model.addAttribute("bulletin", bulletinData);
            model.addAttribute("textConfig", textConfig);
        } catch (IOException e) {
            model.addAttribute("error", "엑셀 파일을 읽을 수 없습니다: " + e.getMessage());
        }
        return "weekly-bulletin";
    }

    @GetMapping("/attendance")
    public String showAttendance(@RequestParam(value = "month", required = false) String month, Model model) {
        try {
            YearMonth selectedMonth = parseAttendanceMonth(month);
            List<CellGroup> cellGroups = excelReaderService.readAttendanceData(excelFilePath, selectedMonth);
            model.addAttribute("cellGroups", cellGroups);
            model.addAttribute("selectedMonthInput", selectedMonth.toString()); // yyyy-MM
        } catch (IOException e) {
            model.addAttribute("error", "엑셀 파일을 읽을 수 없습니다: " + e.getMessage());
        }
        return "attendance";
    }

    private YearMonth parseAttendanceMonth(String month) {
        if (month == null || month.trim().isEmpty()) {
            return YearMonth.now();
        }
        try {
            return YearMonth.parse(month.trim());
        } catch (DateTimeParseException e) {
            return YearMonth.now();
        }
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
