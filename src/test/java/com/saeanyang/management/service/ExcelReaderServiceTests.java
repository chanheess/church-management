package com.saeanyang.management.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.saeanyang.management.model.BulletinData;
import com.saeanyang.management.model.CellGroup;
import com.saeanyang.management.model.Person;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ExcelReaderServiceTests {

  private final ExcelReaderService excelReaderService = new ExcelReaderService();

  @TempDir Path tempDir;

  private byte[] workbookBytes;
  private Path workbookPath;

  @BeforeEach
  void setUp() throws IOException {
    workbookBytes = createWorkbookBytes();
    workbookPath = tempDir.resolve("roster.xlsx");
    Files.write(workbookPath, workbookBytes);
  }

  @Test
  void roster_is_identical_for_path_stream_and_workbook_inputs() throws IOException {
    List<Person> fromPath = excelReaderService.readRosterPeople(workbookPath.toString(), 2026);

    List<Person> fromStream;
    try (InputStream inputStream = new ByteArrayInputStream(workbookBytes)) {
      fromStream = excelReaderService.readRosterPeople(inputStream, 2026);
    }

    List<Person> fromWorkbook;
    try (Workbook workbook = workbook()) {
      fromWorkbook = excelReaderService.readRosterPeople(workbook, 2026);
    }

    assertThat(fromPath).containsExactlyElementsOf(fromStream).containsExactlyElementsOf(fromWorkbook);
    assertThat(fromPath).singleElement().extracting(Person::getName).isEqualTo("테스트사용자");
  }

  @Test
  void bulletin_is_identical_for_path_stream_and_workbook_inputs() throws IOException {
    LocalDate targetSunday = LocalDate.of(2026, 8, 9);
    BulletinData fromPath =
        excelReaderService.readBulletinData(workbookPath.toString(), targetSunday);

    BulletinData fromStream;
    try (InputStream inputStream = new ByteArrayInputStream(workbookBytes)) {
      fromStream = excelReaderService.readBulletinData(inputStream, targetSunday);
    }

    BulletinData fromWorkbook;
    try (Workbook workbook = workbook()) {
      fromWorkbook = excelReaderService.readBulletinData(workbook, targetSunday);
    }

    assertThat(fromPath).isEqualTo(fromStream).isEqualTo(fromWorkbook);
    assertThat(fromPath.getBirthdayMembers()).containsExactly("테스트사용자(10일)");
  }

  @Test
  void attendance_is_identical_for_path_stream_and_workbook_inputs() throws IOException {
    YearMonth selectedMonth = YearMonth.of(2026, 8);
    List<CellGroup> fromPath =
        excelReaderService.readAttendanceData(workbookPath.toString(), selectedMonth);

    List<CellGroup> fromStream;
    try (InputStream inputStream = new ByteArrayInputStream(workbookBytes)) {
      fromStream = excelReaderService.readAttendanceData(inputStream, selectedMonth);
    }

    List<CellGroup> fromWorkbook;
    try (Workbook workbook = workbook()) {
      fromWorkbook = excelReaderService.readAttendanceData(workbook, selectedMonth);
    }

    assertThat(fromPath).containsExactlyElementsOf(fromStream).containsExactlyElementsOf(fromWorkbook);
    assertThat(fromPath).singleElement().satisfies(group -> {
      assertThat(group.getCellName()).isEqualTo("테스트셀");
      assertThat(group.getMembers()).singleElement().satisfies(member ->
          assertThat(member.getName()).isEqualTo("테스트사용자"));
    });
  }

  private Workbook workbook() throws IOException {
    return WorkbookFactory.create(new ByteArrayInputStream(workbookBytes));
  }

  private byte[] createWorkbookBytes() throws IOException {
    try (Workbook workbook = new XSSFWorkbook();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
      Sheet sheet = workbook.createSheet("26년명단");
      Row header = sheet.createRow(0);
      String[] headers = {
        "이름", "직분", "목장", "셀", "생년월일", "상태", "연락처", "행삶", "양육", "8월 출석현황"
      };
      for (int i = 0; i < headers.length; i++) {
        header.createCell(i).setCellValue(headers[i]);
      }

      Row member = sheet.createRow(1);
      member.createCell(0).setCellValue("테스트사용자");
      member.createCell(1).setCellValue("리더");
      member.createCell(2).setCellValue("1목장");
      member.createCell(3).setCellValue("테스트셀");
      member.createCell(4).setCellValue("2000.8.10");
      member.createCell(5).setCellValue("");
      member.createCell(6).setCellValue("01012345678");
      member.createCell(7).setCellValue("진행중");
      member.createCell(8).setCellValue("완료");
      member.createCell(9).setCellValue("출석");

      workbook.write(outputStream);
      return outputStream.toByteArray();
    }
  }
}

