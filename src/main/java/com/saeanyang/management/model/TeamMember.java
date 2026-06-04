package com.saeanyang.management.model;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TeamMember {
  private String division; // 목장 (1목장, 2목장, 3목장)
  private List<String> secretaries; // 간사
  private List<String> pastors; // 목자
  private List<String> leaders; // 셀리더
  private List<String> interns; // 인턴
}
