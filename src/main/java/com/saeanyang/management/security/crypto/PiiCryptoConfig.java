package com.saeanyang.management.security.crypto;

import java.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * 개인정보 암호화 키를 설정에서 읽어 {@link PiiCipherHolder}에 주입한다.
 *
 * <p>키는 {@code app.security.pii-key}(base64 32바이트)로 주입한다 — {@code APP_PII_KEY}
 * 환경변수/.env(로컬)·Compose secret(운영)으로 공급하며 소스에는 커밋하지 않는다.
 * 키가 없으면 임시키로 조용히 기동하지 않고 <b>즉시 기동을 중단</b>한다(fail-fast). 임시키로
 * 기동하면 재시작 시 기존 암호문을 복호화할 수 없어 개인정보가 영구 손실되기 때문이다.
 * 테스트는 별도 테스트 키를 주입한다.
 */
@Configuration
public class PiiCryptoConfig {

  private static final Logger log = LoggerFactory.getLogger(PiiCryptoConfig.class);

  public PiiCryptoConfig(@Value("${app.security.pii-key:}") String base64Key) {
    if (base64Key == null || base64Key.isBlank()) {
      throw new IllegalStateException(
          "app.security.pii-key(APP_PII_KEY)가 설정되지 않았습니다. "
              + "개인정보 암호화 키 없이는 기동할 수 없습니다 — .env 또는 환경변수에 base64 32바이트 키를 설정하세요.");
    }
    PiiCipherHolder.set(new AesGcmCipher(Base64.getDecoder().decode(base64Key.trim())));
    log.info("PII 암호화 키 로드 완료 (app.security.pii-key).");
  }
}
