package com.saeanyang.management.model.representativeprayer;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RepresentativePrayerScheduleRow {
  private String isoDate;

  /** 표시용: "4월 13일" */
  private String label;

  private String defaultLeader;
  private String resolvedLeader;

  /** 스왑으로 바뀐 경우 상대 주일 (ISO) */
  private String swapPartnerDate;

  /** 스왑 상대 표시용 (예: 10월 19일) */
  private String swapPartnerLabel;
}
