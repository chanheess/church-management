package com.saeanyang.management.data.member;

import static org.assertj.core.api.Assertions.assertThat;

import com.saeanyang.management.security.crypto.AesGcmCipher;
import com.saeanyang.management.security.crypto.PiiCipherHolder;
import jakarta.persistence.EntityManager;
import java.security.SecureRandom;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

/** 전화·생년월일은 DB에 암호문으로 저장되고 조회 시 복호화되며, 이름은 평문임을 검증한다. */
@DataJpaTest
class MemberEncryptionTests {

  @Autowired private MemberRepository memberRepository;
  @Autowired private EntityManager em;

  @BeforeAll
  static void initCipher() {
    byte[] key = new byte[32];
    new SecureRandom().nextBytes(key);
    PiiCipherHolder.set(new AesGcmCipher(key));
  }

  @Test
  void pii_is_encrypted_at_rest_and_decrypted_on_read() {
    Member member = new Member();
    member.setRosterYear(2026);
    member.setName("홍길동");
    member.setPhone("01012345678");
    member.setBirthDate(LocalDate.of(1996, 10, 13));
    Member saved = memberRepository.saveAndFlush(member);
    em.clear(); // 1차 캐시 비워 DB에서 다시 읽도록

    // 1) DB 원본 컬럼은 평문이 아니다 (네이티브 조회)
    Object[] raw =
        (Object[])
            em.createNativeQuery("select name, phone, birth_date from members where id = ?1")
                .setParameter(1, saved.getId())
                .getSingleResult();
    String rawName = (String) raw[0];
    String rawPhone = (String) raw[1];
    String rawBirth = (String) raw[2];

    // 이름은 공개 정보라 평문, 전화·생년월일은 암호문
    assertThat(rawName).isEqualTo("홍길동");
    assertThat(rawPhone).isNotEqualTo("01012345678");
    assertThat(rawBirth).doesNotContain("1996");

    // 2) JPA로 읽으면 복호화되어 평문
    Member loaded = memberRepository.findById(saved.getId()).orElseThrow();
    assertThat(loaded.getName()).isEqualTo("홍길동");
    assertThat(loaded.getPhone()).isEqualTo("01012345678");
    assertThat(loaded.getBirthDate()).isEqualTo(LocalDate.of(1996, 10, 13));
  }
}
