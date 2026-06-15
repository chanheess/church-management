package com.saeanyang.management.security.crypto;

/**
 * JPA {@code AttributeConverter}는 Hibernate가 직접 생성(DI 없음)하므로, 암호화 키를
 * 주입할 통로가 필요하다. 이 정적 홀더가 그 다리 역할을 한다.
 *
 * <p>운영에서는 {@link PiiCryptoConfig}가 시작 시 {@link #set} 한다. 테스트는 직접 {@link #set} 한다.
 */
public final class PiiCipherHolder {

  private static volatile AesGcmCipher cipher;

  private PiiCipherHolder() {}

  public static void set(AesGcmCipher cipher) {
    PiiCipherHolder.cipher = cipher;
  }

  public static AesGcmCipher require() {
    AesGcmCipher current = cipher;
    if (current == null) {
      throw new IllegalStateException("PII 암호화 키가 초기화되지 않았습니다 (PiiCryptoConfig 미로딩).");
    }
    return current;
  }
}
