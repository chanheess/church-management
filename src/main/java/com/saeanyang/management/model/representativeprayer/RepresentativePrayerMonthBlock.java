package com.saeanyang.management.model.representativeprayer;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class RepresentativePrayerMonthBlock {
    private int month;
    /** 예: 1월 */
    private String monthLabel;
    /** 해당 월 주일 수 (월·헌금위원 셀 rowspan) */
    private int sundayCount;
    private List<RepresentativePrayerTableDay> days = new ArrayList<>();
    /** 해당 월 헌금위원 표기 (JSON monthlyOffering) */
    private String offeringText;
}
