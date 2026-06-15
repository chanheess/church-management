package com.saeanyang.management.data.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 한 성도의 특정 월 셀 출석 행. {@code model.AttendanceMember}의 월 단위 속성(행삶/양육)에 대응한다.
 *
 * <p>주일별 세부 표시는 {@link AttendanceMark}로 1:N 분리한다.
 */
@Entity
@Table(name = "monthly_attendance")
@Getter
@Setter
@NoArgsConstructor
public class MonthlyAttendance {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /** 대상 성도. */
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "member_id")
  private Member member;

  /** 연도. ({@code year}는 SQL 예약어라 컬럼명을 분리한다.) */
  @Column(name = "attendance_year", nullable = false)
  private Integer year;

  /** 월 (1-12). ({@code month}는 SQL 예약어라 컬럼명을 분리한다.) */
  @Column(name = "attendance_month", nullable = false)
  private Integer month;

  /** 셀 (출석 시트 기준). */
  private String cellName;

  /** 행삶 (행함이 있는 삶). */
  private String action;

  /** 양육. */
  private String training;

  /** 주일별 출석 표시. */
  @OneToMany(
      mappedBy = "monthlyAttendance",
      cascade = CascadeType.ALL,
      orphanRemoval = true)
  private List<AttendanceMark> marks = new ArrayList<>();

  /** 주일 표시를 양방향 일관성 있게 추가한다. */
  public void addMark(AttendanceMark mark) {
    marks.add(mark);
    mark.setMonthlyAttendance(this);
  }
}
