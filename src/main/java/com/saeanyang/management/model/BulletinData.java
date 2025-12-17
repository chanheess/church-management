package com.saeanyang.management.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BulletinData {
    private String headPastor;      // 담당목사
    private String director;        // 디렉터
    private String advisors;        // 고문
    private List<TeamMember> teams; // 목장별 팀원
    private String newYouthLeader;  // 새청년 담당
    private String worshipLeader;   // 찬양팀 리더
    private String date;            // 날짜 (해당 주 주일)
    private String year;            // 연도 (표어에 사용)
}
