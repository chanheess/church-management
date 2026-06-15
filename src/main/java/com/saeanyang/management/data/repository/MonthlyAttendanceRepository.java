package com.saeanyang.management.data.repository;

import com.saeanyang.management.data.entity.Member;
import com.saeanyang.management.data.entity.MonthlyAttendance;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** 월 출석 리포지토리. */
public interface MonthlyAttendanceRepository extends JpaRepository<MonthlyAttendance, Long> {

  List<MonthlyAttendance> findByMember(Member member);

  Optional<MonthlyAttendance> findByMemberAndYearAndMonth(Member member, Integer year, Integer month);
}
