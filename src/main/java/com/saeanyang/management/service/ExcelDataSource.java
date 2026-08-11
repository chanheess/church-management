package com.saeanyang.management.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import org.apache.poi.ss.usermodel.Workbook;

/** 활성 엑셀 원본과 원본 식별용 메타데이터를 제공한다. */
public interface ExcelDataSource {

  /** 새 Workbook을 반환한다. 반환된 Workbook은 호출자가 닫아야 한다. */
  Workbook getActiveWorkbook() throws IOException;

  /** 현재 설정된 원본의 경로, 버전, SHA-256 해시와 수정 시각을 반환한다. */
  Metadata getMetadata() throws IOException;

  record Metadata(String path, String version, String hash, Instant modifiedAt) {}
}

final class ExcelSourceFiles {

  private ExcelSourceFiles() {}

  static String sha256(Path path) throws IOException {
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
