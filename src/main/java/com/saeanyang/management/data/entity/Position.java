package com.saeanyang.management.data.entity;

import java.util.Arrays;

/** 직분. 엑셀 라벨(간사/목자/리더/인턴/성도)과 1:1 대응한다. */
public enum Position {
  STAFF("간사"),
  SHEPHERD("목자"),
  LEADER("리더"),
  INTERN("인턴"),
  MEMBER("성도");

  private final String label;

  Position(String label) {
    this.label = label;
  }

  public String getLabel() {
    return label;
  }

  /** 엑셀의 한글 라벨로 enum을 찾는다. 매칭 실패 시 {@code MEMBER}(성도)로 본다. */
  public static Position fromLabel(String label) {
    if (label == null) {
      return MEMBER;
    }
    String trimmed = label.trim();
    return Arrays.stream(values())
        .filter(p -> p.label.equals(trimmed))
        .findFirst()
        .orElse(MEMBER);
  }
}
