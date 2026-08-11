package com.saeanyang.management.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GoogleDriveExcelSourceTests {

  @TempDir Path tempDir;

  @Test
  void downloadsOnlyChangedRevisions() throws Exception {
    FakeDriveClient client = new FakeDriveClient("1", workbook("첫 버전"));
    GoogleDriveExcelSource source =
        new GoogleDriveExcelSource(client, tempDir.resolve("active.xlsx"));

    assertThat(source.syncIfChanged()).isTrue();
    assertThat(source.syncIfChanged()).isFalse();
    assertThat(client.downloadCount).isEqualTo(1);

    client.version = "2";
    client.content = workbook("둘째 버전");
    assertThat(source.syncIfChanged()).isTrue();
    assertThat(client.downloadCount).isEqualTo(2);
    try (Workbook workbook = source.getActiveWorkbook()) {
      assertThat(workbook.getSheet("둘째 버전")).isNotNull();
    }
    assertThat(source.getMetadata().version()).isEqualTo("2");
    assertThat(source.getMetadata().hash()).matches("[0-9a-f]{64}");
  }

  @Test
  void failedDownloadKeepsLastGoodCache() throws Exception {
    FakeDriveClient client = new FakeDriveClient("1", workbook("정상"));
    GoogleDriveExcelSource source =
        new GoogleDriveExcelSource(client, tempDir.resolve("active.xlsx"));
    source.syncIfChanged();

    client.version = "2";
    client.failure = new IOException("download failed");
    assertThatThrownBy(source::syncIfChanged)
        .isInstanceOf(IOException.class)
        .hasMessage("download failed");

    try (Workbook workbook = source.getActiveWorkbook()) {
      assertThat(workbook.getSheet("정상")).isNotNull();
    }
    assertThat(source.getMetadata().version()).isEqualTo("1");
    assertThat(Files.readString(tempDir.resolve("active.xlsx.revision"))).isEqualTo("1");
  }

  private byte[] workbook(String sheetName) throws IOException {
    try (Workbook workbook = new XSSFWorkbook();
        ByteArrayOutputStream output = new ByteArrayOutputStream()) {
      workbook.createSheet(sheetName);
      workbook.write(output);
      return output.toByteArray();
    }
  }

  private static class FakeDriveClient implements GoogleDriveClient {
    private String version;
    private byte[] content;
    private IOException failure;
    private int downloadCount;

    FakeDriveClient(String version, byte[] content) {
      this.version = version;
      this.content = content;
    }

    @Override
    public RemoteFileMetadata fetchMetadata() {
      return new RemoteFileMetadata(version, Instant.parse("2026-08-11T12:00:00Z"));
    }

    @Override
    public void download(OutputStream output) throws IOException {
      downloadCount++;
      if (failure != null) {
        throw failure;
      }
      output.write(content);
    }
  }
}
