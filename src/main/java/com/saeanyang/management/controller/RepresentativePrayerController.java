package com.saeanyang.management.controller;

import com.saeanyang.management.model.representativeprayer.RepresentativePrayerPageModel;
import com.saeanyang.management.model.representativeprayer.RepresentativePrayerScheduleRow;
import com.saeanyang.management.service.RepresentativePrayerService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Controller
public class RepresentativePrayerController {

    private final RepresentativePrayerService representativePrayerService;

    public RepresentativePrayerController(RepresentativePrayerService representativePrayerService) {
        this.representativePrayerService = representativePrayerService;
    }

    @GetMapping("/representative-prayer")
    public String page(@RequestParam(value = "year", required = false) Integer year, Model model) {
        int y = year != null ? year : LocalDate.now().getYear();
        model.addAttribute("year", y);
        try {
            RepresentativePrayerPageModel prayerPage = representativePrayerService.buildPrayerPage(y);
            model.addAttribute("prayerPage", prayerPage);
            model.addAttribute("scheduleError", null);
        } catch (IOException e) {
            model.addAttribute("prayerPage", null);
            model.addAttribute("scheduleError", e.getMessage());
        }
        return "representative-prayer";
    }

    @GetMapping(value = "/api/representative-prayer/schedule", produces = "application/json")
    @ResponseBody
    public List<RepresentativePrayerScheduleRow> scheduleJson(@RequestParam("year") int year) throws IOException {
        return representativePrayerService.buildScheduleRows(year);
    }

    @PostMapping("/api/representative-prayer/swap")
    @ResponseBody
    public ResponseEntity<?> swap(@RequestBody SwapRequest request) {
        if (request == null || request.dateA() == null || request.dateB() == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "날짜가 필요합니다."));
        }
        try {
            LocalDate a = LocalDate.parse(request.dateA().trim());
            LocalDate b = LocalDate.parse(request.dateB().trim());
            representativePrayerService.addSwap(a, b);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(Map.of("message", e.getMessage()));
        }
    }

    @DeleteMapping("/api/representative-prayer/swap")
    @ResponseBody
    public ResponseEntity<?> removeSwap(@RequestBody RemoveSwapRequest request) {
        if (request == null || request.date() == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "날짜가 필요합니다."));
        }
        try {
            LocalDate date = LocalDate.parse(request.date().trim());
            representativePrayerService.removeSwap(date);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(Map.of("message", e.getMessage()));
        }
    }

    @PutMapping("/api/representative-prayer/override")
    @ResponseBody
    public ResponseEntity<?> setOverride(@RequestBody OverrideRequest request) {
        if (request == null || request.date() == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "날짜가 필요합니다."));
        }
        try {
            LocalDate date = LocalDate.parse(request.date().trim());
            representativePrayerService.setOverride(date, request.name());
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(Map.of("message", e.getMessage()));
        }
    }

    @PutMapping("/api/representative-prayer/offering")
    @ResponseBody
    public ResponseEntity<?> setOffering(@RequestBody OfferingRequest request) {
        if (request == null || request.month() == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "월이 필요합니다."));
        }
        try {
            representativePrayerService.setMonthlyOffering(request.month(), request.text());
            return ResponseEntity.ok().build();
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(Map.of("message", e.getMessage()));
        }
    }

    public record SwapRequest(String dateA, String dateB) {}
    public record RemoveSwapRequest(String date) {}
    public record OverrideRequest(String date, String name) {}
    public record OfferingRequest(Integer month, String text) {}
}
