package com.saeanyang.management.service;

import com.saeanyang.management.model.BulletinData;
import com.saeanyang.management.model.Person;
import com.saeanyang.management.model.TeamMember;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ExcelReaderService {

    public BulletinData readBulletinData(String filePath) throws IOException {
        BulletinData bulletinData = new BulletinData();

        try (FileInputStream fis = new FileInputStream(filePath);
             Workbook workbook = new XSSFWorkbook(fis)) {

            // 시트 목록 출력
            System.out.println("===== 엑셀 시트 목록 =====");
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                System.out.println("시트 " + i + ": " + workbook.getSheetName(i));
            }

            // "26년 명단" 시트 찾기
            Sheet sheet = workbook.getSheet("26년명단");
            if (sheet == null) {
                // 시트를 못 찾으면 첫 번째 시트 사용
                System.out.println("'26년명단' 시트를 찾을 수 없습니다. 첫 번째 시트를 사용합니다.");
                sheet = workbook.getSheetAt(0);
            } else {
                System.out.println("'26년명단' 시트를 찾았습니다!");
            }

            // 하드코딩된 값 설정 (엑셀에 없는 정보)
            bulletinData.setHeadPastor("김한욱 목사");
            bulletinData.setDirector("고은옥 사모");
            bulletinData.setAdvisors("김기현 장로, 황윤식 장로");

            // 엑셀에서 사람 데이터 읽기 (헤더 다음 행부터)
            List<Person> people = readPeopleFromExcel(sheet);

            // 목장별로 데이터 정리
            List<TeamMember> teams = organizeByGroup(people);
            bulletinData.setTeams(teams);

            // 새청년 담당 및 찬양팀 리더 (하드코딩)
            bulletinData.setNewYouthLeader("임유빈, 신영찬");
            bulletinData.setWorshipLeader("임유빈 간사");

            // 이번 주(오늘을 포함해 다음 일요일까지)를 기준으로 하는 주일 날짜 및 연도 계산
            LocalDate sunday = getCurrentWeekSunday();
            bulletinData.setDate(sunday.toString());
            bulletinData.setYear(String.valueOf(sunday.getYear()));

            // 헌금위원 월 및 날짜 계산
            bulletinData.setOfferingMonth(sunday.getMonthValue() + "월");
            bulletinData.setOfferingDates(calculateSundaysInMonth(sunday));
        }

        return bulletinData;
    }

    /**
     * 오늘을 기준으로 "이번 주의 주일(일요일)" 날짜를 계산한다.
     * - 오늘이 일요일이면 오늘 날짜
     * - 그 외 요일이면, 이번 주의 다가오는 일요일 날짜
     */
    private LocalDate getCurrentWeekSunday() {
        LocalDate today = LocalDate.now();
        return today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
    }

    /**
     * 주어진 날짜가 속한 월의 모든 주일(일요일) 날짜를 계산한다.
     * @param referenceDate 기준 날짜
     * @return 해당 월의 모든 주일 날짜 리스트 (예: ["7일", "14일", "21일", "28일"] 또는 5주가 있으면 5개)
     */
    private List<String> calculateSundaysInMonth(LocalDate referenceDate) {
        List<String> sundays = new ArrayList<>();

        // 해당 월의 첫 번째 날
        LocalDate firstDayOfMonth = referenceDate.withDayOfMonth(1);

        // 해당 월의 첫 번째 일요일 찾기
        LocalDate firstSunday = firstDayOfMonth.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));

        // 해당 월의 마지막 날
        LocalDate lastDayOfMonth = referenceDate.with(TemporalAdjusters.lastDayOfMonth());

        // 첫 번째 일요일부터 시작해서 해당 월의 모든 일요일 수집
        LocalDate currentSunday = firstSunday;
        while (!currentSunday.isAfter(lastDayOfMonth)) {
            sundays.add(currentSunday.getDayOfMonth() + "일");
            currentSunday = currentSunday.plusWeeks(1);
        }

        return sundays;
    }

    private List<Person> readPeopleFromExcel(Sheet sheet) {
        List<Person> people = new ArrayList<>();

        System.out.println("===== 엑셀 파일 읽기 시작 =====");
        System.out.println("총 행 수: " + sheet.getLastRowNum());

        // 헤더 행 찾기 및 컬럼 인덱스 파악
        Row headerRow = sheet.getRow(0);
        if (headerRow == null) {
            System.out.println("헤더 행을 찾을 수 없습니다!");
            return people;
        }

        // 헤더 출력
        System.out.println("===== 헤더 정보 =====");
        int lastCol = headerRow.getLastCellNum();
        for (int i = 0; i < lastCol; i++) {
            String header = getCellValue(headerRow, i);
            System.out.println("컬럼 " + i + ": " + header);
        }

        // 컬럼 인덱스 찾기
        int nameCol = -1, positionCol = -1, groupCol = -1, cellCol = -1;
        for (int i = 0; i < lastCol; i++) {
            String header = getCellValue(headerRow, i).trim();
            if (header.contains("이름")) nameCol = i;
            if (header.contains("직분")) positionCol = i;
            if (header.contains("목장")) groupCol = i;
            if (header.contains("셀")) cellCol = i;
        }

        System.out.println("===== 컬럼 인덱스 =====");
        System.out.println("이름: " + nameCol + ", 직분: " + positionCol + ", 목장: " + groupCol + ", 셀: " + cellCol);

        if (nameCol == -1) {
            System.out.println("이름 컬럼을 찾을 수 없습니다!");
            return people;
        }

        // 데이터 읽기 (헤더 다음 행부터)
        System.out.println("===== 데이터 읽기 시작 =====");
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;

            String name = getCellValue(row, nameCol);
            String positionStr = positionCol >= 0 ? getCellValue(row, positionCol) : "";
            String group = groupCol >= 0 ? getCellValue(row, groupCol) : "";
            String cell = cellCol >= 0 ? getCellValue(row, cellCol) : "";

            // 이름이 없으면 스킵
            if (name == null || name.trim().isEmpty()) {
                continue;
            }

            System.out.println("행 " + i + ": 이름=" + name + ", 직분=" + positionStr + ", 목장=" + group + ", 셀=" + cell);

            // 직분 파싱
            List<String> positions = parsePositions(positionStr);

            // 직분이 있는 사람만 추가
            if (!positions.isEmpty()) {
                Person person = new Person();
                person.setName(name.trim());
                person.setPositions(positions);
                person.setGroup(group == null ? "" : group.trim());
                person.setCell(cell == null ? "" : cell.trim());
                people.add(person);
                System.out.println("  → 파싱된 직분: " + positions + " - 추가됨!");
            }
        }

        System.out.println("===== 총 " + people.size() + "명 읽음 =====");
        return people;
    }

    private List<TeamMember> organizeByGroup(List<Person> people) {
        // 목장별로 그룹화 (1목장, 2목장, 3목장)
        Map<String, List<Person>> groupMap = new LinkedHashMap<>();
        groupMap.put("1목장", new ArrayList<>());
        groupMap.put("2목장", new ArrayList<>());
        groupMap.put("3목장", new ArrayList<>());

        for (Person person : people) {
            String group = person.getGroup();
            if (group.contains("1") || group.equals("1목장")) {
                groupMap.get("1목장").add(person);
            } else if (group.contains("2") || group.equals("2목장")) {
                groupMap.get("2목장").add(person);
            } else if (group.contains("3") || group.equals("3목장")) {
                groupMap.get("3목장").add(person);
            }
        }

        List<TeamMember> teams = new ArrayList<>();
        for (Map.Entry<String, List<Person>> entry : groupMap.entrySet()) {
            TeamMember team = new TeamMember();
            team.setDivision(entry.getKey());

            List<String> secretaries = new ArrayList<>();
            List<String> pastors = new ArrayList<>();
            List<String> leaders = new ArrayList<>();
            List<String> interns = new ArrayList<>();

            for (Person person : entry.getValue()) {
                for (String position : person.getPositions()) {
                    if (position.contains("간사")) {
                        secretaries.add(person.getName());
                    }
                    if (position.contains("목자")) {
                        pastors.add(person.getName());
                    }
                    if (position.contains("리더") || position.contains("셀리더")) {
                        leaders.add(person.getName());
                    }
                    if (position.contains("인턴")) {
                        interns.add(person.getName());
                    }
                }
            }

            team.setSecretaries(secretaries);
            team.setPastors(pastors);
            team.setLeaders(leaders);
            team.setInterns(interns);
            teams.add(team);

            System.out.println("===== " + entry.getKey() + " =====");
            System.out.println("  간사: " + secretaries);
            System.out.println("  목자: " + pastors);
            System.out.println("  셀리더: " + leaders);
            System.out.println("  인턴: " + interns);
        }

        return teams;
    }

    private List<String> parsePositions(String positionStr) {
        if (positionStr == null || positionStr.trim().isEmpty()) {
            return new ArrayList<>();
        }

        // 쉼표 또는 슬래시로 구분
        return Arrays.stream(positionStr.split("[,/]"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .filter(s -> s.contains("간사") || s.contains("목자") ||
                           s.contains("리더") || s.contains("인턴"))
                .collect(Collectors.toList());
    }

    private String getCellValue(Row row, int cellIndex) {
        if (row == null) return "";
        Cell cell = row.getCell(cellIndex);
        if (cell == null) return "";

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    return String.valueOf((int) cell.getNumericCellValue());
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return "";
        }
    }

    private List<String> parseList(String value) {
        if (value == null || value.trim().isEmpty() || value.equals("-")) {
            return new ArrayList<>();
        }
        return Arrays.stream(value.split("/"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }
}
