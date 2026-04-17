package com.saeanyang.management.controller;

import com.saeanyang.management.model.BulletinData;
import com.saeanyang.management.model.CellGroup;
import com.saeanyang.management.model.EditableTextConfig;
import com.saeanyang.management.service.ExcelReaderService;
import com.saeanyang.management.service.RepresentativePrayerService;
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
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAdjusters;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Controller
@RequestMapping
public class BulletinController {

    private final ExcelReaderService excelReaderService;
    private final TextConfigService textConfigService;
    private final RepresentativePrayerService representativePrayerService;

    @Value("${bulletin.excel.path}")
    private String excelFilePath;

    @Value("${bulletin.logo.path}")
    private String logoFilePath;

    @Value("${bulletin.illustration.folder}")
    private String illustrationFolder;

    public BulletinController(ExcelReaderService excelReaderService,
                              TextConfigService textConfigService,
                              RepresentativePrayerService representativePrayerService) {
        this.excelReaderService = excelReaderService;
        this.textConfigService = textConfigService;
        this.representativePrayerService = representativePrayerService;
    }

    // ===== 경로 설정 API =====

    public record PathsConfig(String excelPath, String logoPath, String illustrationFolder) {}
    public record PathsUpdateRequest(String excelPath, String logoPath, String illustrationFolder) {}

    @GetMapping("/api/config/paths")
    @ResponseBody
    public PathsConfig getPathsConfig() {
        return new PathsConfig(
            textConfigService.getPathConfig("excelPath", excelFilePath),
            textConfigService.getPathConfig("logoPath", logoFilePath),
            textConfigService.getPathConfig("illustrationFolder", illustrationFolder)
        );
    }

    @PostMapping("/api/config/paths")
    @ResponseBody
    public ResponseEntity<Void> updatePathsConfig(@RequestBody PathsUpdateRequest request) {
        if (request == null) return ResponseEntity.badRequest().build();
        textConfigService.updatePathConfigs(request.excelPath(), request.logoPath(), request.illustrationFolder());
        return ResponseEntity.ok().build();
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

    private String effectiveExcelPath() {
        return textConfigService.getPathConfig("excelPath", excelFilePath);
    }

    private String effectiveLogoPath() {
        return textConfigService.getPathConfig("logoPath", logoFilePath);
    }

    private String effectiveIllustrationFolder() {
        return textConfigService.getPathConfig("illustrationFolder", illustrationFolder);
    }

    @GetMapping("/bulletin")
    public String showBulletin(@RequestParam(value = "date", required = false) String date, Model model) {
        // 기본값 먼저 세팅 — 오류가 나도 템플릿이 null 참조로 500이 나지 않도록
        EditableTextConfig textConfig = textConfigService.loadConfig();
        LocalDate targetSunday = parseBulletinDate(date);
        BulletinData bulletinData = new BulletinData();
        bulletinData.setDate(targetSunday.toString());
        bulletinData.setYear(String.valueOf(targetSunday.getYear()));
        bulletinData.setHeadPastor(textConfig.getHeadPastor());
        bulletinData.setDirector(textConfig.getDirector());
        bulletinData.setAdvisors(textConfig.getAdvisors());
        bulletinData.setNewYouthLeader(textConfig.getNewYouthLeader());
        bulletinData.setWorshipLeader(textConfig.getWorshipLeader());
        bulletinData.setTeams(java.util.Collections.emptyList());
        bulletinData.setOfferingDates(java.util.Collections.emptyList());
        bulletinData.setBirthdayMembers(java.util.Collections.emptyList());

        model.addAttribute("bulletin", bulletinData);
        model.addAttribute("textConfig", textConfig);
        model.addAttribute("selectedDate", targetSunday.toString());

        try {
            String excelPath = effectiveExcelPath();
            if (excelPath != null && !excelPath.isBlank()) {
                bulletinData = excelReaderService.readBulletinData(excelPath, targetSunday);
                bulletinData.setHeadPastor(textConfig.getHeadPastor());
                bulletinData.setDirector(textConfig.getDirector());
                bulletinData.setAdvisors(textConfig.getAdvisors());
                bulletinData.setNewYouthLeader(textConfig.getNewYouthLeader());
                bulletinData.setWorshipLeader(textConfig.getWorshipLeader());
                model.addAttribute("bulletin", bulletinData);
            } else {
                model.addAttribute("error", "엑셀 파일 경로가 설정되지 않았습니다. ⚙ 설정에서 경로를 지정해 주세요.");
            }

            try {
                LocalDate bulletinSunday = LocalDate.parse(bulletinData.getDate());
                bulletinData.setRepresentativePrayerLeader(
                        representativePrayerService.resolveLeader(bulletinSunday));
                bulletinData.setMonthlyRepresentativePrayers(
                        representativePrayerService.resolveLeadersForMonth(bulletinSunday));
                bulletinData.setMonthlyOfferingText(
                        representativePrayerService.resolveMonthlyOfferingText(bulletinSunday));
            } catch (Exception ex) {
                bulletinData.setRepresentativePrayerLeader(null);
                bulletinData.setMonthlyRepresentativePrayers(null);
                bulletinData.setMonthlyOfferingText(null);
            }
        } catch (IOException e) {
            model.addAttribute("error", "엑셀 파일을 읽을 수 없습니다: " + e.getMessage());
        }
        return "weekly-bulletin";
    }

    private LocalDate parseBulletinDate(String date) {
        if (date == null || date.trim().isEmpty()) {
            LocalDate today = LocalDate.now();
            return today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        }
        try {
            LocalDate parsed = LocalDate.parse(date.trim());
            return parsed.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        } catch (DateTimeParseException e) {
            LocalDate today = LocalDate.now();
            return today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        }
    }

    @GetMapping("/attendance")
    public String showAttendance(@RequestParam(value = "month", required = false) String month, Model model) {
        try {
            YearMonth selectedMonth = parseAttendanceMonth(month);
            List<CellGroup> cellGroups = excelReaderService.readAttendanceData(effectiveExcelPath(), selectedMonth);
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
        String logoPath = effectiveLogoPath();
        if (logoPath == null || logoPath.isBlank()) return ResponseEntity.notFound().build();
        File logoFile = new File(logoPath);

        if (!logoFile.exists() || !logoFile.isFile()) {
            return ResponseEntity.notFound().build();
        }

        return buildImageResponse(logoFile);
    }

    /** ===== 일러스트 (날짜 기반 선택) ===== */
    @GetMapping("/api/bulletin/illustration")
    public ResponseEntity<Resource> getIllustration() throws IOException {
        String folderPath = effectiveIllustrationFolder();
        if (folderPath == null || folderPath.isBlank()) return ResponseEntity.notFound().build();
        File folder = new File(folderPath);

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
