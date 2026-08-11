package com.saeanyang.management.service;

import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

interface GoogleDriveClient {
  RemoteFileMetadata fetchMetadata() throws IOException;

  void download(OutputStream output) throws IOException;

  record RemoteFileMetadata(String version, Instant modifiedAt) {}
}

@Component
@ConditionalOnProperty(name = "excel.source", havingValue = "google-drive")
class GoogleDriveApiClient implements GoogleDriveClient {

  private static final String DRIVE_READONLY = "https://www.googleapis.com/auth/drive.readonly";
  private static final Pattern FILE_ID = Pattern.compile("[A-Za-z0-9_-]+");
  private static final JsonMapper JSON = JsonMapper.builder().build();

  private final String fileId;
  private final String credentialsPath;
  private final HttpClient httpClient =
      HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20)).build();
  private GoogleCredentials credentials;

  GoogleDriveApiClient(
      @Value("${excel.google-drive.file-id:}") String fileId,
      @Value("${excel.google-drive.credentials-path:}") String credentialsPath) {
    this.fileId = fileId;
    this.credentialsPath = credentialsPath;
  }

  @Override
  public RemoteFileMetadata fetchMetadata() throws IOException {
    HttpRequest request = request(metadataUri()).GET().build();
    HttpResponse<String> response = send(request, HttpResponse.BodyHandlers.ofString());
    requireSuccess(response.statusCode());

    JsonNode json = JSON.readTree(response.body());
    String version = json.path("version").asString("");
    String modifiedTime = json.path("modifiedTime").asString("");
    if (version.isBlank() || modifiedTime.isBlank()) {
      throw new IOException("Google Drive 파일 메타데이터가 올바르지 않습니다.");
    }
    try {
      return new RemoteFileMetadata(version, Instant.parse(modifiedTime));
    } catch (RuntimeException e) {
      throw new IOException("Google Drive 수정 시각이 올바르지 않습니다.", e);
    }
  }

  @Override
  public void download(OutputStream output) throws IOException {
    HttpRequest request = request(downloadUri()).GET().build();
    HttpResponse<InputStream> response = send(request, HttpResponse.BodyHandlers.ofInputStream());
    try (InputStream input = response.body()) {
      requireSuccess(response.statusCode());
      input.transferTo(output);
    }
  }

  private HttpRequest.Builder request(URI uri) throws IOException {
    return HttpRequest.newBuilder(uri)
        .timeout(Duration.ofSeconds(60))
        .header("Authorization", "Bearer " + accessToken())
        .header("Accept", "application/json");
  }

  private synchronized String accessToken() throws IOException {
    if (credentials == null) {
      Path path = credentialsPath();
      if (!Files.isRegularFile(path)) {
        throw new IOException("Google Drive 서비스계정 파일을 찾을 수 없습니다.");
      }
      try (InputStream input = Files.newInputStream(path)) {
        credentials =
            ServiceAccountCredentials.fromStream(input).createScoped(List.of(DRIVE_READONLY));
      }
    }
    credentials.refreshIfExpired();
    AccessToken token = credentials.getAccessToken();
    if (token == null || token.getTokenValue() == null) {
      throw new IOException("Google Drive 액세스 토큰을 발급받지 못했습니다.");
    }
    return token.getTokenValue();
  }

  private URI metadataUri() throws IOException {
    validateFileId();
    return URI.create(
        "https://www.googleapis.com/drive/v3/files/"
            + fileId
            + "?fields=version,modifiedTime&supportsAllDrives=true");
  }

  private URI downloadUri() throws IOException {
    validateFileId();
    return URI.create(
        "https://www.googleapis.com/drive/v3/files/"
            + fileId
            + "?alt=media&supportsAllDrives=true");
  }

  private void validateFileId() throws IOException {
    if (fileId == null || !FILE_ID.matcher(fileId).matches()) {
      throw new IOException("GOOGLE_DRIVE_FILE_ID가 올바르지 않습니다.");
    }
  }

  private Path credentialsPath() throws IOException {
    if (credentialsPath == null || credentialsPath.isBlank()) {
      throw new IOException("GOOGLE_DRIVE_CREDENTIALS_PATH가 설정되지 않았습니다.");
    }
    try {
      return Path.of(credentialsPath).toAbsolutePath().normalize();
    } catch (InvalidPathException e) {
      throw new IOException("GOOGLE_DRIVE_CREDENTIALS_PATH가 올바르지 않습니다.", e);
    }
  }

  private <T> HttpResponse<T> send(
      HttpRequest request, HttpResponse.BodyHandler<T> bodyHandler) throws IOException {
    try {
      return httpClient.send(request, bodyHandler);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IOException("Google Drive 요청이 중단되었습니다.", e);
    }
  }

  private void requireSuccess(int statusCode) throws IOException {
    if (statusCode < 200 || statusCode >= 300) {
      throw new IOException("Google Drive 요청에 실패했습니다. HTTP " + statusCode);
    }
  }
}
