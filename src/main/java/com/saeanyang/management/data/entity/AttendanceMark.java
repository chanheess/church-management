package com.saeanyang.management.data.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 특정 월 출석의 주일별 표시 한 칸.
 *
 * <p>예: sundayLabel="7일", mark="O".
 */
@Entity
@Table(name = "attendance_marks")
@Getter
@Setter
@NoArgsConstructor
public class AttendanceMark {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /** 소속 월 출석. */
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "monthly_attendance_id")
  private MonthlyAttendance monthlyAttendance;

  /** 주일 라벨 (예: "7일"). */
  private String sundayLabel;

  /** 출석 표시값 (예: O/X/공백). */
  private String mark;
}
