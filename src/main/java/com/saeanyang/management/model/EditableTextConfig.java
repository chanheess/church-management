package com.saeanyang.management.model;

import lombok.Data;

/**
 * 페이지 내에서 사용자가 편집 가능한 텍스트들을 서버 측에 저장/복원하기 위한 설정 객체.
 */
@Data
public class EditableTextConfig {
    private String headPastor;     // 담당목사
    private String director;       // 디렉터
    private String advisors;       // 고문

    private String newYouthLeader; // 새청년 담당
    private String worshipLeader;  // 찬양팀 리더

    // 예배 안내
    private String worshipInfo1;   // 카리스 청년 예배
    private String worshipInfo2;   // 청년 금요 회복 예배

    // 오시는 길
    private String wayToChurch1;
    private String wayToChurch2;

    // 연락처
    private String contact1;
    private String contact2;
}


