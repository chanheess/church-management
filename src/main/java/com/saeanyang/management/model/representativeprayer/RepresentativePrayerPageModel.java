package com.saeanyang.management.model.representativeprayer;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RepresentativePrayerPageModel {
  private RepresentativePrayerYearView yearView;
  private List<RepresentativePrayerScheduleRow> rows;
}
