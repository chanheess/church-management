package com.saeanyang.management.data.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 성도(엑셀 명단 1행)의 DB 스냅샷.
 *
 * <p>엑셀이 원본이고 이 엔티티는 단방향 읽기전용 캐시다. {@code model.Person}에 대응한다.
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

  /** 생일 (MM-dd, 기존 POJO와 동일 형식). */
  private String birthday;

  /** 연락처. */
  private String phone;

  /** 상태. */
  private String status;

  /** 직분 (간사/목자/리더/인턴 등 다중). */
  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(name = "member_positions", joinColumns = @JoinColumn(name = "member_id"))
  @Column(name = "position")
  private Set<String> positions = new LinkedHashSet<>();
}
