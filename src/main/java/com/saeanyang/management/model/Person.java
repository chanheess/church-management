package com.saeanyang.management.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Person {
    private String name;           // 이름
    private List<String> positions; // 직분 (간사, 목자, 리더, 인턴)
    private String group;          // 목장
    private String cell;           // 셀
    private String birthday;       // 생일 (MM-dd 형식)
    private String status;         // 상태
}
