package com.saeanyang.management.model.representativeprayer;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class RepresentativePrayerYearView {
    /** 예: (*수정날짜: 2025.08.31.) */
    private String lastModifiedCaption;
    private List<RepresentativePrayerMonthBlock> leftHalf = new ArrayList<>();
    private List<RepresentativePrayerMonthBlock> rightHalf = new ArrayList<>();
}
