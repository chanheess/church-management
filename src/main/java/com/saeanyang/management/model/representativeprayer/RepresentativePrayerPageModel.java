package com.saeanyang.management.model.representativeprayer;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RepresentativePrayerPageModel {
    private RepresentativePrayerYearView yearView;
    private List<RepresentativePrayerScheduleRow> rows;
}
