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
    private String offeringMonth;   // 헌금위원 월
    private List<String> offeringDates; // 헌금위원 날짜 리스트 (해당 월의 모든 주일)
    private List<String> birthdayMembers; // 이번 주 생일자 리스트 (이름(일))
    /** 엑셀·스왑 기준 자동 산출 대표기도 담당 (주보 예배 순서에 표시) */
    private String representativePrayerLeader;
    /** 해당 월 주일별 대표기도 (헌금위원 표의 '대표기도' 열, offeringDates와 동일 순서) */
    private List<String> monthlyRepresentativePrayers;
    /** 대표기도 탭에서 설정한 해당 월 헌금위원 문구 */
    private String monthlyOfferingText;
}
