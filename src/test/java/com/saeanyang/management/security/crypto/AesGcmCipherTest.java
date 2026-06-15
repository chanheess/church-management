package com.saeanyang.management.security.crypto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.security.SecureRandom;
import org.junit.jupiter.api.Test;

class AesGcmCipherTest {

  private static AesGcmCipher cipherWithRandomKey() {
    byte[] key = new byte[32];
    new SecureRandom().nextBytes(key);
    return new AesGcmCipher(key);
  }

  @Test
  void round_trips() {
    AesGcmCipher cipher = cipherWithRandomKey();
    String plaintext = "홍길동 01012345678";
    assertThat(cipher.decrypt(cipher.encrypt(plaintext))).isEqualTo(plaintext);
  }

  @Test
  void same_plaintext_yields_different_ciphertext() {
    AesGcmCipher cipher = cipherWithRandomKey();
    // 랜덤 IV라 매번 달라야 한다 (결정적 암호화 아님 → 동등성 노출 없음)
    assertThat(cipher.encrypt("홍길동")).isNotEqualTo(cipher.encrypt("홍길동"));
  }

  @Test
  void null_passes_through() {
    AesGcmCipher cipher = cipherWithRandomKey();
    assertThat(cipher.encrypt(null)).isNull();
    assertThat(cipher.decrypt(null)).isNull();
  }

  @Test
  void wrong_key_fails_to_decrypt() {
    String token = cipherWithRandomKey().encrypt("비밀");
    assertThatThrownBy(() -> cipherWithRandomKey().decrypt(token))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void tampered_ciphertext_fails() {
    AesGcmCipher cipher = cipherWithRandomKey();
    String token = cipher.encrypt("비밀");
    // 마지막 글자 변조 → GCM 인증 태그 검증 실패
    char last = token.charAt(token.length() - 1);
    String tampered = token.substring(0, token.length() - 1) + (last == 'A' ? 'B' : 'A');
    assertThatThrownBy(() -> cipher.decrypt(tampered)).isInstanceOf(IllegalStateException.class);
  }

  @Test
  void rejects_non_256bit_key() {
    assertThatThrownBy(() -> new AesGcmCipher(new byte[16]))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
