package com.saeanyang.management.data.repository;

import com.saeanyang.management.data.entity.Member;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** 성도 스냅샷 리포지토리. */
public interface MemberRepository extends JpaRepository<Member, Long> {

  List<Member> findByRosterYear(Integer rosterYear);

  List<Member> findByRosterYearAndCellName(Integer rosterYear, String cellName);
}
