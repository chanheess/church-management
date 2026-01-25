package com.saeanyang.management.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceMember {
    private String name;        // 이름
    private String birthday;    // 생일 (MM-dd)
    private String phone;       // 연락처
    private String action;      // 행삶 (행함이 있는 삶)
    private String training;    // 양육
    private String attendance;  // 출석현황
    private String position;    // 직분
}
