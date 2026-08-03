package com.saeanyang.management.security.crypto;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * AES-256-GCM 양방향 암호화. 매 암호화마다 12바이트 랜덤 IV를 생성하고, 결과를
 * {@code base64(IV ‖ ciphertext+tag)} 로 반환한다(인증 태그 128비트).
 *
 * <p>키 길이는 32바이트(256비트)만 허용한다. 스레드 안전(인스턴스 불변, Cipher는 호출마다 생성).
 */
public final class AesGcmCipher {

  private static final String TRANSFORMATION = "AES/GCM/NoPadding";
  private static final int IV_LENGTH = 12;
  private static final int TAG_BITS = 128;

  private final SecretKeySpec key;
  private final SecureRandom random = new SecureRandom();

  public AesGcmCipher(byte[] keyBytes) {
    if (keyBytes == null || keyBytes.length != 32) {
      throw new IllegalArgumentException("AES-256 키는 32바이트여야 합니다.");
    }
    this.key = new SecretKeySpec(keyBytes, "AES");
  }

  /** 평문을 암호화해 base64(IV‖ct) 반환. null은 그대로 null. */
  public String encrypt(String plaintext) {
    if (plaintext == null) {
      return null;
    }
    try {
      byte[] iv = new byte[IV_LENGTH];
      random.nextBytes(iv);

      Cipher cipher = Cipher.getInstance(TRANSFORMATION);
      cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
      byte[] ct = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

      byte[] out = new byte[iv.length + ct.length];
      System.arraycopy(iv, 0, out, 0, iv.length);
      System.arraycopy(ct, 0, out, iv.length, ct.length);
      return Base64.getEncoder().encodeToString(out);
    } catch (Exception e) {
      throw new IllegalStateException("암호화 실패", e);
    }
  }

  /** base64(IV‖ct)를 복호화. null은 그대로 null. 변조 시 예외. */
  public String decrypt(String encoded) {
    if (encoded == null) {
      return null;
    }
    try {
      byte[] in = Base64.getDecoder().decode(encoded);
      if (in.length <= IV_LENGTH) {
        throw new IllegalArgumentException("암호문 길이가 비정상입니다.");
      }
      byte[] iv = new byte[IV_LENGTH];
      System.arraycopy(in, 0, iv, 0, IV_LENGTH);
      byte[] ct = new byte[in.length - IV_LENGTH];
      System.arraycopy(in, IV_LENGTH, ct, 0, ct.length);

      Cipher cipher = Cipher.getInstance(TRANSFORMATION);
      cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
      return new String(cipher.doFinal(ct), StandardCharsets.UTF_8);
    } catch (Exception e) {
      throw new IllegalStateException("복호화 실패(키 불일치 또는 변조 가능)", e);
    }
  }
}
