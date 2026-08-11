package com.saeanyang.management.service;

import java.io.IOException;
import java.time.Instant;
import org.apache.poi.ss.usermodel.Workbook;

/** 활성 엑셀 원본과 원본 식별용 메타데이터를 제공한다. */
public interface ExcelDataSource {

  /** 새 Workbook을 반환한다. 반환된 Workbook은 호출자가 닫아야 한다. */
  Workbook getActiveWorkbook() throws IOException;

  /** 현재 설정된 원본의 경로, 버전, SHA-256 해시와 수정 시각을 반환한다. */
  Metadata getMetadata() throws IOException;

  record Metadata(String path, String version, String hash, Instant modifiedAt) {}
}
