package com.saeanyang.management.security.crypto;

import java.security.SecureRandom;
import java.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * 개인정보 암호화 키를 설정에서 읽어 {@link PiiCipherHolder}에 주입한다.
 *
 * <p>키는 {@code app.security.pii-key}(base64 32바이트)로 주입한다 — 운영은 {@code APP_PII_KEY}
 * 환경변수/Compose secret 사용(소스·.env 커밋 금지). 키가 없으면 임시 랜덤키로 기동하되 경고를
 * 남긴다(데이터는 재시작 후 복호화 불가). <b>운영 배포 전 반드시 키를 설정해야 한다.</b>
 */
@Configuration
public class PiiCryptoConfig {

  private static final Logger log = LoggerFactory.getLogger(PiiCryptoConfig.class);

  public PiiCryptoConfig(@Value("${app.security.pii-key:}") String base64Key) {
    AesGcmCipher cipher;
    if (base64Key == null || base64Key.isBlank()) {
      byte[] ephemeral = new byte[32];
      new SecureRandom().nextBytes(ephemeral);
      cipher = new AesGcmCipher(ephemeral);
      log.warn(
          "⚠️ app.security.pii-key 미설정 — 임시 랜덤키로 PII 암호화를 기동합니다. "
              + "재시작 시 기존 암호문 복호화 불가. 운영에서는 반드시 APP_PII_KEY를 설정하세요.");
    } else {
      cipher = new AesGcmCipher(Base64.getDecoder().decode(base64Key.trim()));
      log.info("PII 암호화 키 로드 완료 (app.security.pii-key).");
    }
    PiiCipherHolder.set(cipher);
  }
}
