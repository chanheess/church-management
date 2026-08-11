package com.saeanyang.management.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LocalFileExcelSourceTests {

  @TempDir Path tempDir;

  @Test
  void opensConfiguredWorkbookAndProvidesMetadata() throws Exception {
    Path workbookPath = createWorkbook("roster.xlsx");
    LocalFileExcelSource source =
        new LocalFileExcelSource(pathConfig(workbookPath.toString()), "ignored.xlsx");

    try (Workbook workbook = source.getActiveWorkbook()) {
      assertThat(workbook.getSheet("26년명단")).isNotNull();
    }

    ExcelDataSource.Metadata metadata = source.getMetadata();
    assertThat(metadata.path()).isEqualTo(workbookPath.toAbsolutePath().normalize().toString());
    assertThat(metadata.version()).isNotBlank();
    assertThat(metadata.hash()).matches("[0-9a-f]{64}");
    assertThat(metadata.modifiedAt()).isEqualTo(Files.getLastModifiedTime(workbookPath).toInstant());
  }

  @Test
  void reportsUnconfiguredSourceWithoutOpeningWorkbook() throws Exception {
    LocalFileExcelSource source = new LocalFileExcelSource(pathConfig(""), "");

    assertThat(source.getMetadata())
        .isEqualTo(new ExcelDataSource.Metadata("", "", "", null));
    assertThatThrownBy(source::getActiveWorkbook)
        .isInstanceOf(IOException.class)
        .hasMessage("엑셀 파일 경로가 설정되지 않았습니다.");
  }

  private Path createWorkbook(String fileName) throws IOException {
    Path path = tempDir.resolve(fileName);
    try (Workbook workbook = new XSSFWorkbook();
        OutputStream output = Files.newOutputStream(path)) {
      workbook.createSheet("26년명단");
      workbook.write(output);
    }
    return path;
  }

  private TextConfigService pathConfig(String path) {
    return new TextConfigService() {
      @Override
      public String getPathConfig(String key, String defaultValue) {
        return path;
      }
    };
  }
}
