package com.saeanyang.management.controller;

import com.saeanyang.management.service.ExcelDataSource;
import com.saeanyang.management.service.GoogleDriveExcelSource;
import java.io.IOException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@ConditionalOnProperty(name = "excel.source", havingValue = "google-drive")
public class ExcelSyncController {

  private final GoogleDriveExcelSource source;

  public ExcelSyncController(GoogleDriveExcelSource source) {
    this.source = source;
  }

  @PostMapping("/api/excel/sync")
  public SyncResponse sync() throws IOException {
    boolean updated = source.syncIfChanged();
    return new SyncResponse(updated, source.getMetadata());
  }

  public record SyncResponse(boolean updated, ExcelDataSource.Metadata metadata) {}
}
