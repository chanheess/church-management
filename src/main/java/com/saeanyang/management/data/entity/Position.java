package com.saeanyang.management.data.entity;

import java.util.Arrays;
import java.util.Optional;

/**
 * 직분. 엑셀 라벨(간사/목자/리더/인턴)과 1:1 대응한다.
 *
 * <p>"성도"(무직분)는 별도 값을 두지 않고 <b>빈 직분 집합</b>으로 표현한다.
 */
public enum Position {
  STAFF("간사"),
  SHEPHERD("목자"),
  LEADER("리더"),
  INTERN("인턴");

  private final String label;

  Position(String label) {
    this.label = label;
  }

  public String getLabel() {
    return label;
  }

  /**
   * 엑셀의 한글 라벨로 직분을 찾는다. 매칭되는 직분이 없으면(성도·무직분·미상·null) 비어 있다 —
   * 호출 측은 이 경우 직분을 추가하지 않으면 된다(빈 집합 = 성도).
   */
  public static Optional<Position> fromLabel(String label) {
    if (label == null) {
      return Optional.empty();
    }
    String trimmed = label.trim();
    return Arrays.stream(values()).filter(p -> p.label.equals(trimmed)).findFirst();
  }
}
