package com.saeanyang.management.data;

import static org.assertj.core.api.Assertions.assertThat;

import com.saeanyang.management.data.entity.Member;
import com.saeanyang.management.data.entity.Position;
import com.saeanyang.management.data.repository.MemberRepository;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

/** Member 매핑(직분 enum·생년월일·행삶/양육)이 실제 DB에 저장/조회되는지 검증한다. */
@DataJpaTest
class MemberEntityMappingTests {

  @Autowired private MemberRepository memberRepository;

  @Test
  void member_round_trips_with_enum_positions_and_birthdate() {
    Member member = new Member();
    member.setRosterYear(2026);
    member.setName("김정훈");
    member.setGroupName("1목장");
    member.setCellName("김정훈셀");
    member.setBirthDate(LocalDate.of(1996, 10, 13));
    member.setPhone("010-1234-5678");
    member.setStatus("재적");
    member.setAction("O");
    member.setTraining("진행중");
    member.getPositions().add(Position.LEADER);
    member.getPositions().add(Position.SHEPHERD);

    memberRepository.save(member);

    List<Member> found = memberRepository.findByRosterYearAndCellName(2026, "김정훈셀");
    assertThat(found).hasSize(1);
    Member loaded = found.get(0);
    assertThat(loaded.getName()).isEqualTo("김정훈");
    assertThat(loaded.getBirthDate()).isEqualTo(LocalDate.of(1996, 10, 13));
    assertThat(loaded.getPhone()).isEqualTo("010-1234-5678");
    assertThat(loaded.getAction()).isEqualTo("O");
    assertThat(loaded.getTraining()).isEqualTo("진행중");
    assertThat(loaded.getPositions())
        .containsExactlyInAnyOrder(Position.LEADER, Position.SHEPHERD);
  }

  @Test
  void position_resolves_from_korean_label() {
    assertThat(Position.fromLabel("간사")).isEqualTo(Position.STAFF);
    assertThat(Position.fromLabel("성도")).isEqualTo(Position.MEMBER);
    assertThat(Position.fromLabel("없는직분")).isEqualTo(Position.MEMBER);
    assertThat(Position.fromLabel(null)).isEqualTo(Position.MEMBER);
  }
}
