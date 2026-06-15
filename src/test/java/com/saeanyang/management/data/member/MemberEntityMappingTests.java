package com.saeanyang.management.data.member;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.saeanyang.management.data.member.enums.MemberStatus;
import com.saeanyang.management.data.member.enums.Position;
import com.saeanyang.management.data.member.enums.ProgressStatus;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

/** Member 매핑(직분 enum·생년월일·행삶/양육·연락처 검증)이 실제 DB에 저장/조회되는지 검증한다. */
@DataJpaTest
class MemberEntityMappingTests {

  @Autowired private MemberRepository memberRepository;

  @Test
  void member_round_trips_with_enum_positions_and_birthdate() {
    Member member = new Member();
    member.setRosterYear(2026);
    member.setName("홍길동");
    member.setGroupName("1목장");
    member.setCellName("홍길동셀");
    member.setBirthDate(LocalDate.of(1996, 10, 13));
    member.setPhone("01012345678");
    member.setStatus(MemberStatus.MILITARY);
    member.setAction(ProgressStatus.DONE);
    member.setTraining(ProgressStatus.IN_PROGRESS);
    member.getPositions().add(Position.LEADER);
    member.getPositions().add(Position.SHEPHERD);

    memberRepository.save(member);

    List<Member> found = memberRepository.findByRosterYearAndCellName(2026, "홍길동셀");
    assertThat(found).hasSize(1);
    Member loaded = found.get(0);
    assertThat(loaded.getName()).isEqualTo("홍길동");
    assertThat(loaded.getBirthDate()).isEqualTo(LocalDate.of(1996, 10, 13));
    assertThat(loaded.getPhone()).isEqualTo("01012345678");
    assertThat(loaded.getStatus()).isEqualTo(MemberStatus.MILITARY);
    assertThat(loaded.getAction()).isEqualTo(ProgressStatus.DONE);
    assertThat(loaded.getTraining()).isEqualTo(ProgressStatus.IN_PROGRESS);
    assertThat(loaded.getPositions())
        .containsExactlyInAnyOrder(Position.LEADER, Position.SHEPHERD);
  }

  @Test
  void position_resolves_from_korean_label() {
    assertThat(Position.fromLabel("간사")).contains(Position.STAFF);
    assertThat(Position.fromLabel("인턴")).contains(Position.INTERN);
    // 성도(무직분)·미상·null 은 직분 없음 → 빈 Optional
    assertThat(Position.fromLabel("성도")).isEmpty();
    assertThat(Position.fromLabel("없는직분")).isEmpty();
    assertThat(Position.fromLabel(null)).isEmpty();
  }

  @Test
  void status_resolves_from_korean_label() {
    assertThat(MemberStatus.fromLabel("해외")).contains(MemberStatus.OVERSEAS);
    assertThat(MemberStatus.fromLabel("장결자")).contains(MemberStatus.LONG_ABSENT);
    assertThat(MemberStatus.fromLabel("군대")).contains(MemberStatus.MILITARY);
    // 정상(상태 없음)·미상·null 은 빈 Optional → status는 null
    assertThat(MemberStatus.fromLabel("정상")).isEmpty();
    assertThat(MemberStatus.fromLabel(null)).isEmpty();
  }

  @Test
  void progress_status_resolves_from_korean_label() {
    assertThat(ProgressStatus.fromLabel("미진행")).contains(ProgressStatus.NOT_STARTED);
    assertThat(ProgressStatus.fromLabel("진행중")).contains(ProgressStatus.IN_PROGRESS);
    assertThat(ProgressStatus.fromLabel("완료")).contains(ProgressStatus.DONE);
    assertThat(ProgressStatus.fromLabel("")).isEmpty();
    assertThat(ProgressStatus.fromLabel(null)).isEmpty();
  }

  @Test
  void phone_allows_digits_only() {
    Member member = new Member();

    // 숫자 외 문자가 섞이면 예외
    assertThatThrownBy(() -> member.setPhone("010-1234-5678"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> member.setPhone("010 1234 5678"))
        .isInstanceOf(IllegalArgumentException.class);

    // 숫자만, null, 빈 값은 허용
    member.setPhone("01012345678");
    assertThat(member.getPhone()).isEqualTo("01012345678");
    member.setPhone(null);
    assertThat(member.getPhone()).isNull();
  }
}
