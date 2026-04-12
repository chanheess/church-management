package com.saeanyang.management.model.representativeprayer;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RepresentativePrayerTableDay {
    /** 예: 2026-01-05 (드래그앤드롭 data-date 용) */
    private String isoDate;
    /** 예: 5일 */
    private String dayLabel;
    /** 예: 1월 5일 (드래그 확인 다이얼로그 용) */
    private String fullLabel;
    private String prayer;
    /** 스왑 전 기본 담당자 (변경 전 컬럼 표시 용) */
    private String defaultLeader;
    /** 스왑 등으로 기본 순서와 다를 때 강조 */
    private boolean highlightSwapped;
    /** 스왑 상대 날짜 표시용 (예: 3월 19일) — 취소 버튼 확인 다이얼로그용 */
    private String swapPartnerLabel;
}
