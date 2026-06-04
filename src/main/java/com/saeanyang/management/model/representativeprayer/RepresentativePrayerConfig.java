package com.saeanyang.management.model.representativeprayer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class RepresentativePrayerConfig {
  private List<PrayerSwap> swaps = new ArrayList<>();

  /** ISO 날짜(yyyy-MM-dd) → 해당 주일에 고정 표시할 문구 (예: 추석 등) — 순번 소비 안 함 */
  private Map<String, String> dateOverrides = new LinkedHashMap<>();

  /** ISO 날짜(yyyy-MM-dd) → 직접 입력한 담당자 이름 — 순번은 그대로 소비하고 이름만 변경 */
  private Map<String, String> nameOverrides = new LinkedHashMap<>();

  /** 월별 헌금위원 표기 (키: "1"~"12"). 주보 명단 이미지와 같이 월 단위로 한 칸에 표시. */
  private Map<String, String> monthlyOffering = new LinkedHashMap<>();

  /** 설정 저장 시각 (ISO-8601 instant 문자열). 스왑·자동 정리 시 갱신. */
  private String lastModifiedAt;
}
