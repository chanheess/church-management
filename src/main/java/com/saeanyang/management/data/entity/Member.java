package com.saeanyang.management.data.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
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

  /** 이름. */
  @Column(nullable = false)
  private String name;

  /** 목장. */
  private String groupName;

  /** 셀. */
  private String cellName;

  /**
   * 생년월일. 엑셀 값(예: {@code 19961013})을 파싱해 채운다. 연도가 비어 있으면 매퍼가
   * 현재 연도로 채운다. (파싱 규칙은 엑셀→엔티티 매퍼(#21~#23) 책임)
   */
  private LocalDate birthDate;

  /** 연락처. 개인정보(PII)라 문자열로 보관한다(서식·국가코드 보존, 추후 암호화 컨버터 여지). */
  private String phone;

  /** 상태. */
  private String status;

  /** 행삶 (행함이 있는 삶). */
  private String action;

  /** 양육. */
  private String training;

  /** 직분 (간사/목자/리더/인턴/성도 — 다중). */
  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(name = "member_positions", joinColumns = @JoinColumn(name = "member_id"))
  @Column(name = "position")
  @Enumerated(EnumType.STRING)
  private Set<Position> positions = new LinkedHashSet<>();
}
