package com.saeanyang.management.service;

import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;
import com.saeanyang.management.model.Person;
import com.saeanyang.management.model.representativeprayer.PrayerSwap;
import com.saeanyang.management.model.representativeprayer.RepresentativePrayerConfig;
import com.saeanyang.management.model.representativeprayer.RepresentativePrayerMonthBlock;
import com.saeanyang.management.model.representativeprayer.RepresentativePrayerPageModel;
import com.saeanyang.management.model.representativeprayer.RepresentativePrayerScheduleRow;
import com.saeanyang.management.model.representativeprayer.RepresentativePrayerTableDay;
import com.saeanyang.management.model.representativeprayer.RepresentativePrayerYearView;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.text.Collator;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class RepresentativePrayerService {

    private static final String[] ROLE_ORDER = {"간사", "목자", "리더", "인턴"};
    private static final String[] MOKJANG_ORDER = {"1목장", "2목장", "3목장"};
    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    private final ExcelReaderService excelReaderService;
    private final TextConfigService textConfigService;
    private final JsonMapper objectMapper;

    @Value("${bulletin.excel.path:}")
    private String excelFilePath;

    @Value("${bulletin.text.path:}")
    private String textConfigPath;

    @Value("${bulletin.representative-prayer.path:}")
    private String prayerConfigPath;

    public RepresentativePrayerService(ExcelReaderService excelReaderService,
                                      TextConfigService textConfigService) {
        this.excelReaderService = excelReaderService;
        this.textConfigService = textConfigService;
        this.objectMapper = JsonMapper.builder()
                .findAndAddModules()
                .enable(SerializationFeature.INDENT_OUTPUT)
                .build();
    }

    public RepresentativePrayerPageModel buildPrayerPage(int year) throws IOException {
        LocalDate today = LocalDate.now();
        RepresentativePrayerConfig cfg = loadAndPruneConfig();
        List<RepresentativePrayerScheduleRow> rows = buildScheduleRowsWithConfig(year, cfg);
        RepresentativePrayerYearView yearView = buildYearView(rows, cfg);
        return new RepresentativePrayerPageModel(yearView, rows);
    }

    public List<RepresentativePrayerScheduleRow> buildScheduleRows(int year) throws IOException {
        return buildScheduleRowsWithConfig(year, loadAndPruneConfig());
    }

    private List<RepresentativePrayerScheduleRow> buildScheduleRowsWithConfig(int year,
                                                                              RepresentativePrayerConfig cfg)
            throws IOException {
        List<Person> people = readPeopleForYear(year);
        List<String> rotation = buildRotationOrder(people);
        // 적용 순서: base(순번) → swap → nameOverride
        // nameOverride가 swap보다 나중에 적용되어야 override 값이 swap에 의해 뒤바뀌지 않음
        Map<LocalDate, String> base = buildBaseSchedule(year, rotation, cfg.getDateOverrides());
        Map<LocalDate, String> afterSwaps = applySwaps(base, cfg.getSwaps());
        Map<LocalDate, String> resolved = applyNameOverrides(afterSwaps, cfg.getNameOverrides());

        List<LocalDate> sundays = sundaysInYear(year);
        Map<LocalDate, LocalDate> partner = swapPartnerMap(cfg.getSwaps());

        List<RepresentativePrayerScheduleRow> rows = new ArrayList<>();
        for (LocalDate sun : sundays) {
            String iso = sun.toString();
            String label = formatKoreanDayLabel(sun);
            LocalDate p = partner.get(sun);
            rows.add(new RepresentativePrayerScheduleRow(
                    iso,
                    label,
                    base.getOrDefault(sun, "-"),
                    resolved.getOrDefault(sun, "-"),
                    p != null ? p.toString() : null,
                    p != null ? formatKoreanDayLabel(p) : null
            ));
        }
        return rows;
    }

    private RepresentativePrayerYearView buildYearView(List<RepresentativePrayerScheduleRow> rows,
                                                         RepresentativePrayerConfig cfg) {
        RepresentativePrayerYearView view = new RepresentativePrayerYearView();
        view.setLastModifiedCaption(formatLastModifiedCaption(cfg));

        Map<Integer, List<RepresentativePrayerScheduleRow>> byMonth = new LinkedHashMap<>();
        for (int m = 1; m <= 12; m++) {
            byMonth.put(m, new ArrayList<>());
        }
        for (RepresentativePrayerScheduleRow r : rows) {
            LocalDate d = LocalDate.parse(r.getIsoDate());
            byMonth.get(d.getMonthValue()).add(r);
        }

        List<RepresentativePrayerMonthBlock> all = new ArrayList<>();
        for (int m = 1; m <= 12; m++) {
            RepresentativePrayerMonthBlock block = new RepresentativePrayerMonthBlock();
            block.setMonth(m);
            block.setMonthLabel(m + "월");
            block.setOfferingText(offeringForMonth(cfg, m));

            List<RepresentativePrayerTableDay> days = new ArrayList<>();
            for (RepresentativePrayerScheduleRow r : byMonth.get(m)) {
                LocalDate d = LocalDate.parse(r.getIsoDate());
                boolean highlight = !Objects.equals(r.getDefaultLeader(), r.getResolvedLeader());
                days.add(new RepresentativePrayerTableDay(
                        r.getIsoDate(),
                        d.getDayOfMonth() + "일",
                        r.getLabel(),
                        r.getResolvedLeader(),
                        r.getDefaultLeader(),
                        highlight,
                        r.getSwapPartnerLabel()
                ));
            }
            if (days.isEmpty()) {
                days.add(new RepresentativePrayerTableDay("", "—", "—", "—", "", false, null));
            }
            block.setDays(days);
            block.setSundayCount(days.size());
            all.add(block);
        }

        view.setLeftHalf(new ArrayList<>(all.subList(0, 6)));
        view.setRightHalf(new ArrayList<>(all.subList(6, 12)));
        return view;
    }

    private static String formatLastModifiedCaption(RepresentativePrayerConfig cfg) {
        String iso = cfg.getLastModifiedAt();
        if (iso == null || iso.isBlank()) {
            return "(*수정날짜: —.)";
        }
        try {
            Instant instant = Instant.parse(iso);
            String d = DateTimeFormatter.ofPattern("yyyy.MM.dd").withZone(SEOUL).format(instant);
            return "(*수정날짜: " + d + ".)";
        } catch (Exception e) {
            return "(*수정날짜: —.)";
        }
    }

    private static String offeringForMonth(RepresentativePrayerConfig cfg, int month) {
        Map<String, String> map = cfg.getMonthlyOffering();
        if (map == null) {
            return "—";
        }
        String v = map.get(String.valueOf(month));
        if (v != null && !v.isBlank()) {
            return v;
        }
        return "—";
    }

    /**
     * 주보에 표시할 이번 주 대표기도 담당 (기본 순서 + 스왑 + 날짜 예외).
     */
    public String resolveLeader(LocalDate sunday) throws IOException {
        Objects.requireNonNull(sunday, "sunday");
        int year = sunday.getYear();
        LocalDate today = LocalDate.now();
        RepresentativePrayerConfig cfg = loadAndPruneConfig();
        List<Person> people = readPeopleForYear(year);
        List<String> rotation = buildRotationOrder(people);
        Map<LocalDate, String> base = buildBaseSchedule(year, rotation, cfg.getDateOverrides());
        Map<LocalDate, String> afterSwaps = applySwaps(base, cfg.getSwaps());
        Map<LocalDate, String> resolved = applyNameOverrides(afterSwaps, cfg.getNameOverrides());
        return resolved.getOrDefault(sunday, "-");
    }

    /**
     * 주보 '대표기도·헌금위원' 표에 쓸, 해당 월의 모든 주일별 담당 (스왑·날짜 예외 반영).
     * {@link com.saeanyang.management.service.ExcelReaderService} 의 월별 주일 나열과 같은 순서다.
     */
    public List<String> resolveLeadersForMonth(LocalDate referenceSunday) throws IOException {
        Objects.requireNonNull(referenceSunday, "referenceSunday");
        int year = referenceSunday.getYear();
        int month = referenceSunday.getMonthValue();
        LocalDate today = LocalDate.now();
        RepresentativePrayerConfig cfg = loadAndPruneConfig();
        List<Person> people = readPeopleForYear(year);
        List<String> rotation = buildRotationOrder(people);
        Map<LocalDate, String> base = buildBaseSchedule(year, rotation, cfg.getDateOverrides());
        Map<LocalDate, String> afterSwaps = applySwaps(base, cfg.getSwaps());
        Map<LocalDate, String> resolved = applyNameOverrides(afterSwaps, cfg.getNameOverrides());

        LocalDate first = LocalDate.of(year, month, 1);
        LocalDate last = first.with(TemporalAdjusters.lastDayOfMonth());
        LocalDate d = first.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        List<String> out = new ArrayList<>();
        while (!d.isAfter(last)) {
            out.add(resolved.getOrDefault(d, "-"));
            d = d.plusWeeks(1);
        }
        return out;
    }

    public void addSwap(LocalDate dateA, LocalDate dateB) throws IOException {
        if (dateA.equals(dateB)) {
            throw new IllegalArgumentException("같은 날짜는 스왑할 수 없습니다.");
        }
        if (dateA.getDayOfWeek() != DayOfWeek.SUNDAY || dateB.getDayOfWeek() != DayOfWeek.SUNDAY) {
            throw new IllegalArgumentException("주일만 선택할 수 있습니다.");
        }
        LocalDate today = LocalDate.now();
        RepresentativePrayerConfig cfg = loadAndPruneConfig();
        String a = dateA.toString();
        String b = dateB.toString();

        cfg.getSwaps().add(new PrayerSwap(a, b));
        saveConfig(cfg);
    }

    public String resolveMonthlyOfferingText(LocalDate referenceSunday) throws IOException {
        RepresentativePrayerConfig cfg = loadAndPruneConfig();
        return offeringForMonth(cfg, referenceSunday.getMonthValue());
    }

    public void setMonthlyOffering(int month, String text) throws IOException {
        RepresentativePrayerConfig cfg = loadAndPruneConfig();
        if (text == null || text.isBlank()) {
            cfg.getMonthlyOffering().remove(String.valueOf(month));
        } else {
            cfg.getMonthlyOffering().put(String.valueOf(month), text.trim());
        }
        saveConfig(cfg);
    }

    public void setOverride(LocalDate date, String name) throws IOException {
        RepresentativePrayerConfig cfg = loadAndPruneConfig();
        String iso = date.toString();
        if (name == null || name.isBlank()) {
            cfg.getNameOverrides().remove(iso);
        } else {
            cfg.getNameOverrides().put(iso, name.trim());
        }
        saveConfig(cfg);
    }

    public void removeSwap(LocalDate date) throws IOException {
        String iso = date.toString();
        RepresentativePrayerConfig cfg = loadAndPruneConfig();
        boolean removed = cfg.getSwaps().removeIf(s -> containsDate(s, iso));
        if (!removed) {
            throw new IllegalArgumentException("해당 날짜의 스왑을 찾을 수 없습니다.");
        }
        saveConfig(cfg);
    }

    private static boolean containsDate(PrayerSwap s, String iso) {
        return iso.equals(s.getDateA()) || iso.equals(s.getDateB());
    }

    private Map<LocalDate, LocalDate> swapPartnerMap(List<PrayerSwap> swaps) {
        Map<LocalDate, LocalDate> map = new HashMap<>();
        for (PrayerSwap s : swaps) {
            try {
                LocalDate a = LocalDate.parse(s.getDateA());
                LocalDate b = LocalDate.parse(s.getDateB());
                map.put(a, b);
                map.put(b, a);
            } catch (Exception ignored) {
                // skip invalid
            }
        }
        return map;
    }

    private Map<LocalDate, String> applySwaps(Map<LocalDate, String> base, List<PrayerSwap> swaps) {
        Map<LocalDate, String> map = new LinkedHashMap<>(base);
        for (PrayerSwap s : swaps) {
            try {
                LocalDate a = LocalDate.parse(s.getDateA());
                LocalDate b = LocalDate.parse(s.getDateB());
                String va = map.get(a);
                String vb = map.get(b);
                if (va != null && vb != null) {
                    map.put(a, vb);
                    map.put(b, va);
                }
            } catch (Exception ignored) {
                // skip invalid swap
            }
        }
        return map;
    }

    private Map<LocalDate, String> buildBaseSchedule(int year, List<String> rotation,
                                                     Map<String, String> dateOverrides) {
        Map<LocalDate, String> map = new LinkedHashMap<>();
        List<LocalDate> sundays = sundaysInYear(year);
        if (rotation.isEmpty()) {
            for (LocalDate sun : sundays) {
                map.put(sun, "-");
            }
            return map;
        }

        int rotIndex = 0;
        for (LocalDate sun : sundays) {
            String key = sun.toString();
            if (dateOverrides != null && dateOverrides.containsKey(key)) {
                // 특수 문구(추석 등) — 순번 소비 없음
                map.put(sun, dateOverrides.get(key));
            } else {
                map.put(sun, rotation.get(rotIndex % rotation.size()));
                rotIndex++;
            }
        }
        return map;
    }

    // nameOverride는 swap보다 나중에 적용 — swap 결과를 덮어써야 함
    private Map<LocalDate, String> applyNameOverrides(Map<LocalDate, String> resolved,
                                                      Map<String, String> nameOverrides) {
        if (nameOverrides == null || nameOverrides.isEmpty()) return resolved;
        Map<LocalDate, String> map = new LinkedHashMap<>(resolved);
        nameOverrides.forEach((key, value) -> {
            try {
                LocalDate date = LocalDate.parse(key);
                if (value != null && !value.isBlank()) {
                    map.put(date, value);
                }
            } catch (Exception ignored) {}
        });
        return map;
    }

    private static String formatKoreanDayLabel(LocalDate d) {
        return d.getMonthValue() + "월 " + d.getDayOfMonth() + "일";
    }

    private List<LocalDate> sundaysInYear(int year) {
        List<LocalDate> list = new ArrayList<>();
        LocalDate d = LocalDate.of(year, 1, 1).with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        LocalDate end = LocalDate.of(year, 12, 31);
        while (!d.isAfter(end)) {
            list.add(d);
            d = d.plusWeeks(1);
        }
        return list;
    }

    /**
     * 기본 순서: 간사 → 목자 → 리더 → 인턴, 각 직분 내 1목장→2목장→3목장, 동일 칸은 이름 가나다순.
     */
    List<String> buildRotationOrder(List<Person> people) {
        Collator collator = Collator.getInstance(Locale.KOREAN);
        collator.setStrength(Collator.PRIMARY);
        Comparator<String> nameOrder = collator::compare;

        List<String> rotation = new ArrayList<>();
        for (String role : ROLE_ORDER) {
            for (String mokjang : MOKJANG_ORDER) {
                List<Person> bucket = people.stream()
                        .filter(p -> mokjangKey(p).equals(mokjang))
                        .filter(p -> role.equals(primaryRoleLabel(p)))
                        .sorted(Comparator.comparing(Person::getName, nameOrder))
                        .collect(Collectors.toList());
                for (Person p : bucket) {
                    rotation.add(p.getName() + " " + roleLabelForDisplay(role));
                }
            }
        }
        return rotation;
    }

    private static String roleLabelForDisplay(String role) {
        return switch (role) {
            case "간사" -> "간사";
            case "목자" -> "목자";
            case "리더" -> "리더";
            case "인턴" -> "인턴";
            default -> role;
        };
    }

    private String mokjangKey(Person p) {
        String g = p.getGroup() == null ? "" : p.getGroup().trim();
        if (g.contains("1")) return "1목장";
        if (g.contains("2")) return "2목장";
        if (g.contains("3")) return "3목장";
        return "";
    }

    /**
     * 직분 목록에서 가장 높은 순위(간사>목자>리더>인턴) 하나만 사용.
     */
    private String primaryRoleLabel(Person p) {
        List<String> positions = p.getPositions();
        if (positions == null || positions.isEmpty()) {
            return null;
        }
        for (String role : ROLE_ORDER) {
            for (String pos : positions) {
                if (pos == null) continue;
                String s = pos.trim();
                if ("리더".equals(role) && (s.contains("셀리더") || s.contains("리더"))) {
                    return "리더";
                }
                if (s.contains(role)) {
                    return role;
                }
            }
        }
        return null;
    }

    private List<Person> readPeopleForYear(int year) throws IOException {
        String effectiveExcelPath = textConfigService.getPathConfig("excelPath", excelFilePath);
        if (effectiveExcelPath == null || effectiveExcelPath.isBlank()) {
            return List.of();
        }
        return excelReaderService.readRosterPeople(effectiveExcelPath, year);
    }

    private RepresentativePrayerConfig loadAndPruneConfig() throws IOException {
        return loadConfigRaw();
    }

    private RepresentativePrayerConfig loadConfigRaw() throws IOException {
        File file = resolvedConfigFile();
        if (!file.exists()) {
            return new RepresentativePrayerConfig();
        }
        byte[] bytes = Files.readAllBytes(file.toPath());
        if (bytes.length == 0) {
            return new RepresentativePrayerConfig();
        }
        RepresentativePrayerConfig cfg = objectMapper.readValue(bytes, RepresentativePrayerConfig.class);
        ensureConfigCollections(cfg);
        return cfg;
    }

    private static void ensureConfigCollections(RepresentativePrayerConfig cfg) {
        if (cfg.getSwaps() == null) {
            cfg.setSwaps(new ArrayList<>());
        }
        if (cfg.getDateOverrides() == null) {
            cfg.setDateOverrides(new LinkedHashMap<>());
        }
        if (cfg.getNameOverrides() == null) {
            cfg.setNameOverrides(new LinkedHashMap<>());
        }
        if (cfg.getMonthlyOffering() == null) {
            cfg.setMonthlyOffering(new LinkedHashMap<>());
        }
    }

    private void saveConfig(RepresentativePrayerConfig cfg) throws IOException {
        cfg.setLastModifiedAt(Instant.now().toString());
        File file = resolvedConfigFile();
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            //noinspection ResultOfMethodCallIgnored
            parent.mkdirs();
        }
        objectMapper.writeValue(file, cfg);
    }

    private File resolvedConfigFile() {
        if (prayerConfigPath != null && !prayerConfigPath.isBlank()) {
            return new File(prayerConfigPath);
        }
        if (textConfigPath != null && !textConfigPath.isBlank()) {
            File parent = new File(textConfigPath).getParentFile();
            if (parent != null) {
                return new File(parent, "representative-prayer.json");
            }
        }
        return new File("representative-prayer.json");
    }
}
