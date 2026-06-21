package com.saeanyang.management.data.member;

import com.saeanyang.management.data.member.enums.MemberStatus;
import com.saeanyang.management.data.member.enums.Position;
import com.saeanyang.management.data.member.enums.ProgressStatus;
import com.saeanyang.management.security.crypto.EncryptedLocalDateConverter;
import com.saeanyang.management.security.crypto.EncryptedStringConverter;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 성도(엑셀 명단 1행)의 DB 스냅샷. 규모가 작아(최대 ~100명) 단일 테이블로 평면화한다.
 *
 * <p>엑셀이 원본이고 이 엔티티는 단방향 읽기전용 캐시다. 출석 표시 등은 조회 시 값만
 * 가져와 그리므로 별도 출석 엔티티를 두지 않고, 월 단위 속성(행삶/양육)만 여기에 둔다.
 */
@Entity
@Table(name = "members")
@Getter
@Setter
@NoArgsConstructor
public class Member {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /** 명단 연도 (예: 2026) — 적재 단위 식별. */
  @Column(nullable = false)
  private Integer rosterYear;

  /** 이름. 주보에 공개되는 정보이고 검색·정렬이 잦아 평문으로 둔다(암호화 제외). 접근통제로 보호. */
  @Column(nullable = false)
  private String name;

  /** 목장. */
  private String groupName;

  /** 셀. */
  private String cellName;

  /**
   * 생년월일. 엑셀 값(예: {@code 19961013})을 파싱해 채운다. 연도가 비어 있으면 매퍼가
   * 현재 연도로 채운다. (파싱 규칙은 엑셀→엔티티 매퍼(#21~#23) 책임)
   *
   * <p>PII — 저장 시 암호화.
   */
  @Convert(converter = EncryptedLocalDateConverter.class)
  private LocalDate birthDate;

  /** 연락처. 숫자만 허용한다(PII — 저장 시 암호화). 하이픈 등 서식은 매퍼에서 제거. */
  @Convert(converter = EncryptedStringConverter.class)
  private String phone;

  /** 연락처는 숫자만 허용한다. 숫자 외 문자가 있으면 {@link IllegalArgumentException}. (null·빈 값은 허용) */
  public void setPhone(String phone) {
    if (phone != null && !phone.isBlank() && !phone.matches("\\d+")) {
      throw new IllegalArgumentException("연락처는 숫자만 입력할 수 있습니다: " + phone);
    }
    this.phone = phone;
  }

  /** 상태. 정상(상태 없음)은 {@code null}, 예외 상태(해외/장결자/군대)만 값을 둔다. */
  @Enumerated(EnumType.STRING)
  private MemberStatus status;

  /** 행삶 (행함이 있는 삶). 진행 상태(미진행/진행중/완료), 미상은 null. */
  @Enumerated(EnumType.STRING)
  private ProgressStatus action;

  /** 양육. 진행 상태(미진행/진행중/완료), 미상은 null. */
  @Enumerated(EnumType.STRING)
  private ProgressStatus training;

  /** 직분 (간사/목자/리더/인턴/성도 — 다중). */
  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(name = "member_positions", joinColumns = @JoinColumn(name = "member_id"))
  @Column(name = "position")
  @Enumerated(EnumType.STRING)
  private Set<Position> positions = new LinkedHashSet<>();
}
