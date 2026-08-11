package com.saeanyang.management.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
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
        path.toString(), version, sha256(path), attributes.lastModifiedTime().toInstant());
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

  private String sha256(Path path) throws IOException {
    MessageDigest digest;
    try {
      digest = MessageDigest.getInstance("SHA-256");
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256을 사용할 수 없습니다.", e);
    }

    try (InputStream input = new DigestInputStream(Files.newInputStream(path), digest)) {
      input.transferTo(OutputStream.nullOutputStream());
    }
    return HexFormat.of().formatHex(digest.digest());
  }
}
