package com.saeanyang.management.service;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "excel.source", havingValue = "google-drive")
public class GoogleDriveExcelSource implements ExcelDataSource {

  private static final Logger log = LoggerFactory.getLogger(GoogleDriveExcelSource.class);

  private final GoogleDriveClient client;
  private final Path cachePath;
  private final Path revisionPath;

  @Autowired
  public GoogleDriveExcelSource(
      GoogleDriveClient client, @Value("${excel.google-drive.cache-path}") String cachePath) {
    this(client, Path.of(cachePath));
  }

  GoogleDriveExcelSource(GoogleDriveClient client, Path cachePath) {
    this.client = client;
    this.cachePath = cachePath.toAbsolutePath().normalize();
    this.revisionPath = this.cachePath.resolveSibling(this.cachePath.getFileName() + ".revision");
  }

  @Scheduled(
      initialDelayString = "${excel.google-drive.initial-delay:PT0S}",
      fixedDelayString = "${excel.google-drive.poll-interval:PT5M}")
  public void poll() {
    try {
      syncIfChanged();
    } catch (IOException e) {
      log.warn("Google Drive 엑셀 동기화 실패, 마지막 정상 캐시를 유지합니다: {}", e.getMessage());
    }
  }

  public synchronized boolean syncIfChanged() throws IOException {
    GoogleDriveClient.RemoteFileMetadata remote = client.fetchMetadata();
    if (Files.isRegularFile(cachePath) && remote.version().equals(readRevision())) {
      return false;
    }

    Path parent = cachePath.getParent();
    Files.createDirectories(parent);
    Path temporary = Files.createTempFile(parent, cachePath.getFileName().toString(), ".download");
    try {
      try (OutputStream output = Files.newOutputStream(temporary)) {
        client.download(output);
      }
      validateWorkbook(temporary);
      replace(temporary, cachePath);
      Files.setLastModifiedTime(cachePath, FileTime.from(remote.modifiedAt()));
      writeRevision(remote.version());
      return true;
    } finally {
      Files.deleteIfExists(temporary);
    }
  }

  @Override
  public Workbook getActiveWorkbook() throws IOException {
    if (!Files.isRegularFile(cachePath)) {
      syncIfChanged();
    }
    if (!Files.isRegularFile(cachePath)) {
      throw new IOException("Google Drive 엑셀의 마지막 정상 캐시가 없습니다.");
    }
    return WorkbookFactory.create(cachePath.toFile(), null, true);
  }

  @Override
  public Metadata getMetadata() throws IOException {
    if (!Files.isRegularFile(cachePath)) {
      return new Metadata(cachePath.toString(), readRevision(), "", null);
    }
    return new Metadata(
        cachePath.toString(),
        readRevision(),
        ExcelSourceFiles.sha256(cachePath),
        Files.getLastModifiedTime(cachePath).toInstant());
  }

  private void validateWorkbook(Path path) throws IOException {
    try (Workbook workbook = WorkbookFactory.create(path.toFile(), null, true)) {
      if (workbook.getNumberOfSheets() == 0) {
        throw new IOException("다운로드한 엑셀에 시트가 없습니다.");
      }
    } catch (IOException e) {
      throw e;
    } catch (RuntimeException e) {
      throw new IOException("다운로드한 파일이 올바른 엑셀이 아닙니다.", e);
    }
  }

  private String readRevision() throws IOException {
    return Files.isRegularFile(revisionPath) ? Files.readString(revisionPath).trim() : "";
  }

  private void writeRevision(String revision) throws IOException {
    Path temporary = Files.createTempFile(revisionPath.getParent(), "excel-revision", ".tmp");
    try {
      Files.writeString(temporary, revision);
      replace(temporary, revisionPath);
    } finally {
      Files.deleteIfExists(temporary);
    }
  }

  private void replace(Path source, Path target) throws IOException {
    try {
      Files.move(
          source,
          target,
          StandardCopyOption.ATOMIC_MOVE,
          StandardCopyOption.REPLACE_EXISTING);
    } catch (AtomicMoveNotSupportedException e) {
      Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
    }
  }
}
