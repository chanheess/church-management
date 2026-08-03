package com.saeanyang.management.security.crypto;

/**
 * JPA {@code AttributeConverter}는 Hibernate가 직접 생성(DI 없음)하므로, 암호화 키를
 * 주입할 통로가 필요하다. 이 정적 홀더가 그 다리 역할을 한다.
 *
 * <p>운영에서는 {@link PiiCryptoConfig}가 시작 시 {@link #set} 한다. 테스트는 직접 {@link #set} 한다.
 *
 * <p>전역 정적 상태다. 현재 테스트는 순차 실행(JUnit 병렬 미설정)을 전제로 각 테스트가 자기 키를
 * {@link #set} 한다. 병렬 테스트를 도입하면 여러 테스트가 이 단일 필드를 덮어써 교차 오염될 수
 * 있으므로, 그때는 테스트별 키 격리가 필요하다.
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
