package com.saeanyang.management.model.representativeprayer;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 두 주일의 대표기도 담당을 일시적으로 맞바꾼다.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PrayerSwap {
    private String dateA;
    private String dateB;
}
