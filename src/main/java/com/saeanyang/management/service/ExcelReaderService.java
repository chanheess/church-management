package com.saeanyang.management.service;

import com.saeanyang.management.model.AttendanceMember;
import com.saeanyang.management.model.BulletinData;
import com.saeanyang.management.model.CellGroup;
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

            // 이번 주 생일자 계산 (일요일~토요일, 상태값이 없는 사람만)
            bulletinData.setBirthdayMembers(getWeekBirthdays(people, sunday));
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
     * 이번 주 생일자 리스트를 가져온다 (일요일~토요일, 상태값이 없는 사람만)
     * @param people 전체 사람 리스트
     * @param sunday 이번 주 일요일 날짜
     * @return 생일자 리스트 (예: ["이상민(14일)", "문다비(16일)"])
     */
    private List<String> getWeekBirthdays(List<Person> people, LocalDate sunday) {
        List<String> birthdays = new ArrayList<>();

        // 이번 주 범위: 일요일부터 토요일까지
        LocalDate weekStart = sunday;
        LocalDate weekEnd = sunday.plusDays(6);

        System.out.println("===== 생일자 체크 (주간: " + weekStart + " ~ " + weekEnd + ") =====");

        for (Person person : people) {
            // 상태값이 있으면 제외
            if (person.getStatus() != null && !person.getStatus().trim().isEmpty()) {
                continue;
            }

            String birthdayStr = person.getBirthday();
            if (birthdayStr == null || birthdayStr.trim().isEmpty()) {
                continue;
            }

            try {
                // 생일 파싱 (MM-dd, M-d, MM/dd, M/d 등 다양한 형식 지원)
                String cleaned = birthdayStr.trim().replace("/", "-");
                String[] parts = cleaned.split("-");
                if (parts.length != 2) continue;

                int month = Integer.parseInt(parts[0].trim());
                int day = Integer.parseInt(parts[1].trim());

                // 이번 주 범위 안에 생일이 있는지 확인
                LocalDate birthThisYear = LocalDate.of(weekStart.getYear(), month, day);

                if (!birthThisYear.isBefore(weekStart) && !birthThisYear.isAfter(weekEnd)) {
                    birthdays.add(person.getName() + "(" + day + "일)");
                    System.out.println("  → 생일자 발견: " + person.getName() + " (생일: " + month + "월 " + day + "일)");
                }
            } catch (Exception e) {
                System.out.println("  → 생일 파싱 실패: " + person.getName() + ", 생일값: " + birthdayStr);
            }
        }

        System.out.println("===== 총 " + birthdays.size() + "명의 생일자 =====");
        return birthdays;
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
        int nameCol = -1, positionCol = -1, groupCol = -1, cellCol = -1, birthdayCol = -1, statusCol = -1;
        for (int i = 0; i < lastCol; i++) {
            String header = getCellValue(headerRow, i).trim();
            if (header.contains("이름")) nameCol = i;
            if (header.contains("직분")) positionCol = i;
            if (header.contains("목장")) groupCol = i;
            if (header.contains("셀")) cellCol = i;
            if (header.contains("생일")) birthdayCol = i;
            if (header.contains("상태")) statusCol = i;
        }

        System.out.println("===== 컬럼 인덱스 =====");
        System.out.println("이름: " + nameCol + ", 직분: " + positionCol + ", 목장: " + groupCol + ", 셀: " + cellCol + ", 생일: " + birthdayCol + ", 상태: " + statusCol);

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
            String birthday = birthdayCol >= 0 ? getCellValue(row, birthdayCol) : "";
            String status = statusCol >= 0 ? getCellValue(row, statusCol) : "";

            // 이름이 없으면 스킵
            if (name == null || name.trim().isEmpty()) {
                continue;
            }

            System.out.println("행 " + i + ": 이름=" + name + ", 직분=" + positionStr + ", 목장=" + group + ", 셀=" + cell + ", 생일=" + birthday + ", 상태=" + status);

            // 직분 파싱
            List<String> positions = parsePositions(positionStr);

            // 모든 사람 추가 (직분이 없어도 생일 체크를 위해)
            Person person = new Person();
            person.setName(name.trim());
            person.setPositions(positions);
            person.setGroup(group == null ? "" : group.trim());
            person.setCell(cell == null ? "" : cell.trim());
            person.setBirthday(birthday == null ? "" : birthday.trim());
            person.setStatus(status == null ? "" : status.trim());
            people.add(person);

            if (!positions.isEmpty()) {
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
            // 직분이 있는 사람만 목장별로 분류
            if (person.getPositions() == null || person.getPositions().isEmpty()) {
                continue;
            }

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

    /**
     * 생일 셀의 값을 M.d 형식으로 반환한다
     */
    private String getBirthdayValue(Row row, int cellIndex) {
        if (row == null) return "";
        Cell cell = row.getCell(cellIndex);
        if (cell == null) return "";

        switch (cell.getCellType()) {
            case STRING:
                // 이미 문자열로 저장된 경우
                String value = cell.getStringCellValue().trim();
                if (value.isEmpty()) return "";

                // yyyy.M.d 형식 (예: "2000.1.15", "1990.12.31")
                if (value.contains(".")) {
                    String[] parts = value.split("\\.");
                    if (parts.length == 3) {
                        try {
                            int month = Integer.parseInt(parts[1].trim());
                            int day = Integer.parseInt(parts[2].trim());
                            return month + "." + day;
                        } catch (NumberFormatException e) {
                            // 파싱 실패 시 원본 반환
                        }
                    } else if (parts.length == 2) {
                        // M.d 형식 - 이미 원하는 형식이므로 그대로 반환
                        try {
                            int month = Integer.parseInt(parts[0].trim());
                            int day = Integer.parseInt(parts[1].trim());
                            return month + "." + day;
                        } catch (NumberFormatException e) {
                            // 파싱 실패 시 원본 반환
                        }
                    }
                }

                // MM-dd 또는 M-d 형식을 M.d로 변환
                if (value.contains("-") || value.contains("/")) {
                    String[] parts = value.replace("/", "-").split("-");
                    if (parts.length == 2) {
                        try {
                            int month = Integer.parseInt(parts[0].trim());
                            int day = Integer.parseInt(parts[1].trim());
                            return month + "." + day;
                        } catch (NumberFormatException e) {
                            return value;
                        }
                    } else if (parts.length == 3) {
                        // yyyy-M-d 형식
                        try {
                            int month = Integer.parseInt(parts[1].trim());
                            int day = Integer.parseInt(parts[2].trim());
                            return month + "." + day;
                        } catch (NumberFormatException e) {
                            return value;
                        }
                    }
                }
                return value;

            case NUMERIC:
                // 엑셀 날짜 시리얼 번호로 저장된 경우 (예: 45996)
                try {
                    java.util.Date date = cell.getDateCellValue();
                    java.time.LocalDate localDate = date.toInstant()
                        .atZone(java.time.ZoneId.systemDefault())
                        .toLocalDate();
                    return localDate.getMonthValue() + "." + localDate.getDayOfMonth();
                } catch (Exception e) {
                    // 날짜가 아닌 일반 숫자인 경우
                    int numValue = (int) cell.getNumericCellValue();
                    if (numValue > 0 && numValue <= 1231) {
                        int month = numValue / 100;
                        int day = numValue % 100;
                        if (month >= 1 && month <= 12 && day >= 1 && day <= 31) {
                            return month + "." + day;
                        }
                    }
                    return "";
                }

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

    /**
     * 엑셀에서 연락처 및 출석현황 데이터를 읽어온다.
     * 셀별로 그룹화하여 반환한다.
     */
    public List<CellGroup> readAttendanceData(String filePath) throws IOException {
        List<CellGroup> cellGroups = new ArrayList<>();

        try (FileInputStream fis = new FileInputStream(filePath);
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheet("26년명단");
            if (sheet == null) {
                sheet = workbook.getSheetAt(0);
            }

            // 헤더 행 찾기
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                return cellGroups;
            }

            // 컬럼 인덱스 찾기
            int nameCol = -1, birthdayCol = -1, phoneCol = -1, actionCol = -1,
                trainingCol = -1, attendanceCol = -1, cellCol = -1, statusCol = -1, positionCol = -1;

            int lastCol = headerRow.getLastCellNum();
            for (int i = 0; i < lastCol; i++) {
                String header = getCellValue(headerRow, i).trim();
                if (header.contains("이름")) nameCol = i;
                if (header.contains("생일") || header.contains("생년월일")) birthdayCol = i;
                if (header.contains("연락처") || header.contains("핸드폰") || header.contains("전화")) phoneCol = i;
                if (header.contains("행삶")) actionCol = i;
                if (header.contains("양육")) trainingCol = i;
                if (header.contains("출석") || header.contains("12월")) attendanceCol = i;
                if (header.contains("셀")) cellCol = i;
                if (header.contains("상태")) statusCol = i;
                if (header.contains("직분")) positionCol = i;
            }

            if (nameCol == -1 || cellCol == -1) {
                return cellGroups;
            }

            // 현재 월 계산
            LocalDate today = LocalDate.now();
            String currentMonth = today.getMonthValue() + "월 출석현황";

            // 해당 월의 모든 일요일 날짜 계산
            List<String> sundayDates = calculateSundaysInMonth(today);

            // 셀별로 데이터 그룹화
            Map<String, List<AttendanceMember>> cellMap = new LinkedHashMap<>();

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                String name = getCellValue(row, nameCol);
                if (name == null || name.trim().isEmpty()) continue;

                // 상태가 있는 사람은 제외
                String status = statusCol >= 0 ? getCellValue(row, statusCol) : "";
                if (status != null && !status.trim().isEmpty()) {
                    continue;
                }

                String cell = cellCol >= 0 ? getCellValue(row, cellCol) : "";
                String birthday = birthdayCol >= 0 ? getBirthdayValue(row, birthdayCol) : "";
                String phone = phoneCol >= 0 ? getCellValue(row, phoneCol) : "";
                String action = actionCol >= 0 ? getCellValue(row, actionCol) : "";
                String training = trainingCol >= 0 ? getCellValue(row, trainingCol) : "";
                String attendance = attendanceCol >= 0 ? getCellValue(row, attendanceCol) : "";
                String position = positionCol >= 0 ? getCellValue(row, positionCol) : "";

                AttendanceMember member = new AttendanceMember();
                member.setName(name.trim());
                member.setBirthday(birthday.trim());
                member.setPhone(phone.trim());
                member.setAction(action.trim());
                member.setTraining(training.trim());
                member.setAttendance(attendance.trim());
                member.setPosition(position.trim());

                String cellKey = cell.trim().isEmpty() ? "미지정" : cell.trim();
                cellMap.computeIfAbsent(cellKey, k -> new ArrayList<>()).add(member);
            }

            // CellGroup으로 변환 (미지정 제외)
            for (Map.Entry<String, List<AttendanceMember>> entry : cellMap.entrySet()) {
                // "미지정" 셀은 제외
                if ("미지정".equals(entry.getKey())) {
                    continue;
                }

                // 직분 순서대로 정렬
                List<AttendanceMember> members = entry.getValue();
                members.sort((m1, m2) -> {
                    int priority1 = getPositionPriority(m1.getPosition());
                    int priority2 = getPositionPriority(m2.getPosition());
                    return Integer.compare(priority1, priority2);
                });

                CellGroup group = new CellGroup();
                group.setCellName(entry.getKey());
                group.setAttendanceMonth(currentMonth);
                group.setMembers(members);
                group.setSundayDates(sundayDates);
                cellGroups.add(group);
            }
        }

        return cellGroups;
    }

    /**
     * 직분의 우선순위를 반환한다 (숫자가 작을수록 우선순위가 높음)
     * @param position 직분 문자열 (예: "간사", "목자", "리더/간사" 등)
     * @return 우선순위 (1: 간사, 2: 목자, 3: 리더, 4: 인턴, 9999: 일반)
     */
    private int getPositionPriority(String position) {
        if (position == null || position.trim().isEmpty()) {
            return 9999; // 일반 멤버 (직분 없음)
        }

        String pos = position.toLowerCase();

        // 직분이 여러 개 있을 수 있으므로 가장 높은 우선순위 반환
        if (pos.contains("간사")) {
            return 1;
        }
        if (pos.contains("목자")) {
            return 2;
        }
        if (pos.contains("셀리더") || pos.contains("리더")) {
            return 3;
        }
        if (pos.contains("인턴")) {
            return 4;
        }

        return 9999; // 기타
    }
}
