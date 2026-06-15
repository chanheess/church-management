package com.saeanyang.management.data.repository;

import com.saeanyang.management.data.entity.AttendanceMark;
import org.springframework.data.jpa.repository.JpaRepository;

/** 주일별 출석 표시 리포지토리. */
public interface AttendanceMarkRepository extends JpaRepository<AttendanceMark, Long> {
}
