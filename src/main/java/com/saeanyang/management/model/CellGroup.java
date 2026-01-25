package com.saeanyang.management.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CellGroup {
    private String cellName;          // 셀 이름 (예: "김정훈셀")
    private String attendanceMonth;   // 출석 월 (예: "12월 출석현황")
    private List<AttendanceMember> members; // 셀 멤버 리스트
    private List<String> sundayDates; // 해당 월의 일요일 날짜들 (예: ["7일", "14일", "21일", "28일"])
}
