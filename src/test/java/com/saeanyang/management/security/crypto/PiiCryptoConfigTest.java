package com.saeanyang.management.security.crypto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Base64;
import org.junit.jupiter.api.Test;

/** 키 미설정 시 fail-fast, 정상 키 주입 시 홀더에 cipher를 세팅함을 검증한다. */
class PiiCryptoConfigTest {

  private static final String VALID_KEY_BASE64 =
      Base64.getEncoder().encodeToString(new byte[32]);

  @Test
  void fails_fast_when_key_is_blank() {
    assertThatThrownBy(() -> new PiiCryptoConfig("")).isInstanceOf(IllegalStateException.class);
  }

  @Test
  void fails_fast_when_key_is_null() {
    assertThatThrownBy(() -> new PiiCryptoConfig(null)).isInstanceOf(IllegalStateException.class);
  }

  @Test
  void loads_cipher_when_key_provided() {
    new PiiCryptoConfig(VALID_KEY_BASE64);
    AesGcmCipher cipher = PiiCipherHolder.require();
    assertThat(cipher.decrypt(cipher.encrypt("비밀"))).isEqualTo("비밀");
  }
}
