package com.saeanyang.management.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "excel.source", havingValue = "local", matchIfMissing = true)
public class LocalFileExcelSource implements ExcelDataSource {

  private final TextConfigService textConfigService;
  private final String defaultPath;

  public LocalFileExcelSource(
      TextConfigService textConfigService, @Value("${bulletin.excel.path:}") String defaultPath) {
    this.textConfigService = textConfigService;
    this.defaultPath = defaultPath;
  }

  @Override
  public Workbook getActiveWorkbook() throws IOException {
    Path path = configuredPath();
    if (path == null) {
      throw new IOException("엑셀 파일 경로가 설정되지 않았습니다.");
    }
    if (!Files.isRegularFile(path)) {
      throw new IOException("엑셀 파일을 찾을 수 없습니다: " + path);
    }
    return WorkbookFactory.create(path.toFile(), null, true);
  }

  @Override
  public Metadata getMetadata() throws IOException {
    Path path = configuredPath();
    if (path == null) {
      return new Metadata("", "", "", null);
    }
    if (!Files.isRegularFile(path)) {
      return new Metadata(path.toString(), "", "", null);
    }

    BasicFileAttributes attributes =
        Files.readAttributes(path, BasicFileAttributes.class);
    String version = attributes.size() + "-" + attributes.lastModifiedTime().toMillis();
    return new Metadata(
        path.toString(),
        version,
        ExcelSourceFiles.sha256(path),
        attributes.lastModifiedTime().toInstant());
  }

  private Path configuredPath() throws IOException {
    String value = textConfigService.getPathConfig("excelPath", defaultPath);
    if (value == null || value.isBlank()) {
      return null;
    }
    try {
      return Path.of(value).toAbsolutePath().normalize();
    } catch (InvalidPathException e) {
      throw new IOException("올바르지 않은 엑셀 파일 경로입니다.", e);
    }
  }
}
