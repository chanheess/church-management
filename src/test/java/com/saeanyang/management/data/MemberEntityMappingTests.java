package com.saeanyang.management.data;

import static org.assertj.core.api.Assertions.assertThat;

import com.saeanyang.management.data.entity.AttendanceMark;
import com.saeanyang.management.data.entity.Member;
import com.saeanyang.management.data.entity.MonthlyAttendance;
import com.saeanyang.management.data.repository.MemberRepository;
import com.saeanyang.management.data.repository.MonthlyAttendanceRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

/** 엔티티 매핑·관계가 실제 DB에 저장/조회되는지 검증한다. */
@DataJpaTest
class MemberEntityMappingTests {

  @Autowired private MemberRepository memberRepository;
  @Autowired private MonthlyAttendanceRepository monthlyAttendanceRepository;

  @Test
  void member_with_positions_round_trips() {
    Member member = new Member();
    member.setRosterYear(2026);
    member.setName("김정훈");
    member.setGroupName("1목장");
    member.setCellName("김정훈셀");
    member.setBirthday("03-14");
    member.setPhone("010-1234-5678");
    member.setStatus("재적");
    member.getPositions().add("리더");
    member.getPositions().add("목자");

    memberRepository.save(member);

    List<Member> found = memberRepository.findByRosterYearAndCellName(2026, "김정훈셀");
    assertThat(found).hasSize(1);
    assertThat(found.get(0).getPositions()).containsExactlyInAnyOrder("리더", "목자");
    assertThat(found.get(0).getName()).isEqualTo("김정훈");
  }

  @Test
  void monthly_attendance_cascades_marks() {
    Member member = new Member();
    member.setRosterYear(2026);
    member.setName("이서연");
    memberRepository.save(member);

    MonthlyAttendance attendance = new MonthlyAttendance();
    attendance.setMember(member);
    attendance.setYear(2026);
    attendance.setMonth(12);
    attendance.setCellName("이서연셀");
    attendance.setAction("O");
    attendance.setTraining("진행중");

    AttendanceMark first = new AttendanceMark();
    first.setSundayLabel("7일");
    first.setMark("O");
    AttendanceMark second = new AttendanceMark();
    second.setSundayLabel("14일");
    second.setMark("X");
    attendance.addMark(first);
    attendance.addMark(second);

    monthlyAttendanceRepository.save(attendance);

    MonthlyAttendance reloaded =
        monthlyAttendanceRepository.findByMemberAndYearAndMonth(member, 2026, 12).orElseThrow();
    assertThat(reloaded.getMarks()).hasSize(2);
    assertThat(reloaded.getMarks())
        .extracting(AttendanceMark::getSundayLabel)
        .containsExactlyInAnyOrder("7일", "14일");
    assertThat(reloaded.getAction()).isEqualTo("O");
  }
}
