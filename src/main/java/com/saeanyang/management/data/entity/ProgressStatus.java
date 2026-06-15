package com.saeanyang.management.data.entity;

import java.util.Arrays;
import java.util.Optional;

/**
 * 진행 상태. 행삶·양육 등 진척을 나타내며 엑셀 라벨(미진행/진행중/완료)과 1:1 대응한다.
 */
public enum ProgressStatus {
  NOT_STARTED("미진행"),
  IN_PROGRESS("진행중"),
  DONE("완료");

  private final String label;

  ProgressStatus(String label) {
    this.label = label;
  }

  public String getLabel() {
    return label;
  }

  /**
   * 엑셀의 한글 라벨로 진행 상태를 찾는다. 매칭되는 값이 없으면(빈 값·미상·null) 비어 있다 —
   * 호출 측은 이 경우 필드를 null로 두면 된다.
   */
  public static Optional<ProgressStatus> fromLabel(String label) {
    if (label == null) {
      return Optional.empty();
    }
    String trimmed = label.trim();
    return Arrays.stream(values()).filter(s -> s.label.equals(trimmed)).findFirst();
  }
}
