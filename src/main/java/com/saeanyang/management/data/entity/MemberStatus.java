package com.saeanyang.management.data.entity;

import java.util.Arrays;
import java.util.Optional;

/**
 * 성도의 예외 상태. 엑셀 라벨(해외/장결자/군대)과 1:1 대응한다.
 *
 * <p>정상(상태 없음)은 별도 값을 두지 않고 <b>{@code null}</b>로 표현한다(대부분이 정상).
 */
public enum MemberStatus {
  OVERSEAS("해외"),
  LONG_ABSENT("장결자"),
  MILITARY("군대");

  private final String label;

  MemberStatus(String label) {
    this.label = label;
  }

  public String getLabel() {
    return label;
  }

  /**
   * 엑셀의 한글 라벨로 상태를 찾는다. 매칭되는 상태가 없으면(정상·미상·null) 비어 있다 —
   * 호출 측은 이 경우 status를 null로 두면 된다.
   */
  public static Optional<MemberStatus> fromLabel(String label) {
    if (label == null) {
      return Optional.empty();
    }
    String trimmed = label.trim();
    return Arrays.stream(values()).filter(s -> s.label.equals(trimmed)).findFirst();
  }
}
