package com.saeanyang.management.service;

import com.saeanyang.management.model.EditableTextConfig;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Properties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class TextConfigService {

  @Value("${bulletin.text.path}")
  private String textConfigPath;

  /** 서버에 저장된 설정(없으면 기본값)을 읽어온다. */
  public EditableTextConfig loadConfig() {
    EditableTextConfig config = createDefaultConfig();

    File file = new File(textConfigPath);
    if (!file.exists()) {
      return config;
    }

    Properties props = new Properties();
    try (InputStream in = new FileInputStream(file);
        Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
      props.load(reader);
    } catch (IOException e) {
      e.printStackTrace();
      return config;
    }

    config.setHeadPastor(props.getProperty("headPastor", config.getHeadPastor()));
    config.setDirector(props.getProperty("director", config.getDirector()));
    config.setAdvisors(props.getProperty("advisors", config.getAdvisors()));
    config.setNewYouthLeader(props.getProperty("newYouthLeader", config.getNewYouthLeader()));
    config.setWorshipLeader(props.getProperty("worshipLeader", config.getWorshipLeader()));

    config.setWorshipInfo1(props.getProperty("worshipInfo1", config.getWorshipInfo1()));
    config.setWorshipInfo2(props.getProperty("worshipInfo2", config.getWorshipInfo2()));

    config.setWayToChurch1(props.getProperty("wayToChurch1", config.getWayToChurch1()));
    config.setWayToChurch2(props.getProperty("wayToChurch2", config.getWayToChurch2()));

    config.setContact1(props.getProperty("contact1", config.getContact1()));
    config.setContact2(props.getProperty("contact2", config.getContact2()));

    config.setSloganLabel(props.getProperty("sloganLabel", config.getSloganLabel()));
    config.setSloganTitle(props.getProperty("sloganTitle", config.getSloganTitle()));

    // 2페이지 필드들도 additionalFields에 로드
    String[] page2Keys = {
      "worship_songs",
      "worship_team",
      "prayer_topic",
      "prayer_leader",
      "scripture_reading",
      "scripture_reader",
      "sermon_title",
      "sermon_pastor",
      "offering_song",
      "offering_team",
      "announcement_content",
      "announcement_leader",
      "closing_prayer_content",
      "closing_prayer_leader",
      "cell_meeting_content",
      "cell_meeting_participants",
      "offering_prayer_1",
      "offering_prayer_2",
      "offering_prayer_3",
      "offering_prayer_4",
      "offering_member_all",
      "notice_1",
      "notice_2",
      "notice_3",
      "notice_4",
      "offering_account",
      "cell_meeting_schedule",
      "blessing_1"
    };
    for (String key : page2Keys) {
      String value = props.getProperty(key);
      if (value != null) {
        config.getAdditionalFields().put(key, value);
      }
    }

    // Properties에 있는 모든 키를 확인해서 추가 필드로 저장 (동적 키 지원)
    for (String propKey : props.stringPropertyNames()) {
      if (!isPage1Field(propKey) && !config.getAdditionalFields().containsKey(propKey)) {
        config.getAdditionalFields().put(propKey, props.getProperty(propKey));
      }
    }

    return config;
  }

  /** 단일 key/value 업데이트 후 파일에 저장 */
  public void updateField(String key, String value) {
    EditableTextConfig config = loadConfig();

    switch (key) {
      case "headPastor":
        config.setHeadPastor(value);
        break;
      case "director":
        config.setDirector(value);
        break;
      case "advisors":
        config.setAdvisors(value);
        break;
      case "newYouthLeader":
        config.setNewYouthLeader(value);
        break;
      case "worshipLeader":
        config.setWorshipLeader(value);
        break;
      case "worshipInfo1":
        config.setWorshipInfo1(value);
        break;
      case "worshipInfo2":
        config.setWorshipInfo2(value);
        break;
      case "wayToChurch1":
        config.setWayToChurch1(value);
        break;
      case "wayToChurch2":
        config.setWayToChurch2(value);
        break;
      case "contact1":
        config.setContact1(value);
        break;
      case "contact2":
        config.setContact2(value);
        break;
      case "sloganLabel":
        config.setSloganLabel(value);
        break;
      case "sloganTitle":
        config.setSloganTitle(value);
        break;
      default:
        // 2페이지 필드 또는 동적 필드는 additionalFields에 저장
        config.getAdditionalFields().put(key, value);
        break;
    }

    saveConfig(config);
  }

  private void saveConfig(EditableTextConfig config) {
    Properties props = new Properties();
    props.setProperty("headPastor", nullToEmpty(config.getHeadPastor()));
    props.setProperty("director", nullToEmpty(config.getDirector()));
    props.setProperty("advisors", nullToEmpty(config.getAdvisors()));
    props.setProperty("newYouthLeader", nullToEmpty(config.getNewYouthLeader()));
    props.setProperty("worshipLeader", nullToEmpty(config.getWorshipLeader()));
    props.setProperty("worshipInfo1", nullToEmpty(config.getWorshipInfo1()));
    props.setProperty("worshipInfo2", nullToEmpty(config.getWorshipInfo2()));
    props.setProperty("wayToChurch1", nullToEmpty(config.getWayToChurch1()));
    props.setProperty("wayToChurch2", nullToEmpty(config.getWayToChurch2()));
    props.setProperty("contact1", nullToEmpty(config.getContact1()));
    props.setProperty("contact2", nullToEmpty(config.getContact2()));
    props.setProperty("sloganLabel", nullToEmpty(config.getSloganLabel()));
    props.setProperty("sloganTitle", nullToEmpty(config.getSloganTitle()));

    // 2페이지 필드들도 저장
    for (Map.Entry<String, String> entry : config.getAdditionalFields().entrySet()) {
      props.setProperty(entry.getKey(), nullToEmpty(entry.getValue()));
    }

    File file = new File(textConfigPath);
    File parent = file.getParentFile();
    if (parent != null && !parent.exists()) {
      //noinspection ResultOfMethodCallIgnored
      parent.mkdirs();
    }

    try (OutputStream out = new FileOutputStream(file);
        Writer writer = new OutputStreamWriter(out, StandardCharsets.UTF_8)) {
      props.store(writer, "Bulletin editable text configuration");
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private EditableTextConfig createDefaultConfig() {
    EditableTextConfig config = new EditableTextConfig();
    config.setHeadPastor("김한욱 목사");
    config.setDirector("고은옥 사모");
    config.setAdvisors("김기현 장로, 황윤식 장로");
    config.setNewYouthLeader("임유빈, 신영찬");
    config.setWorshipLeader("임유빈 간사");

    config.setWorshipInfo1("카리스 청년 예배 : 주일 낮 12:30 3층 본당");
    config.setWorshipInfo2("청년 금요 회복 예배 : 금요일 저녁 8:30 3층 본당");

    config.setWayToChurch1("1호선 관악역 하차 후 예술공원 방면 도보 10분");
    config.setWayToChurch2("버스 : 1, 20, 51, 5530, 5713, 5625, 5626");

    config.setContact1("교회 : 031-472-5670");
    config.setContact2("임세일 간사 : 010-3091-5659");
    config.setSloganLabel("새안양 교회 표어");
    config.setSloganTitle("우리가 넉넉히 이기리라");

    // 축복 기본값 설정
    config.getAdditionalFields().put("blessing_1", "새청년 축복");

    return config;
  }

  private String nullToEmpty(String v) {
    return v == null ? "" : v;
  }

  /** 1페이지 필드인지 확인 */
  private boolean isPage1Field(String key) {
    return key.equals("headPastor")
        || key.equals("director")
        || key.equals("advisors")
        || key.equals("newYouthLeader")
        || key.equals("worshipLeader")
        || key.equals("worshipInfo1")
        || key.equals("worshipInfo2")
        || key.equals("wayToChurch1")
        || key.equals("wayToChurch2")
        || key.equals("contact1")
        || key.equals("contact2")
        || key.equals("sloganLabel")
        || key.equals("sloganTitle");
  }

  /** 추가 필드 값 가져오기 (템플릿에서 사용) */
  public String getAdditionalField(String key, String defaultValue) {
    EditableTextConfig config = loadConfig();
    return config.getAdditionalFields().getOrDefault(key, defaultValue);
  }

  /** 파일 경로 설정 조회 (없으면 defaultValue 반환) */
  public String getPathConfig(String key, String defaultValue) {
    String value = loadConfig().getAdditionalFields().get("__path_" + key);
    return (value != null && !value.isBlank()) ? value : defaultValue;
  }

  /** 파일 경로 설정 일괄 저장 */
  public void updatePathConfigs(String excelPath, String logoPath, String illustrationFolder) {
    if (excelPath != null) updateField("__path_excelPath", excelPath);
    if (logoPath != null) updateField("__path_logoPath", logoPath);
    if (illustrationFolder != null) updateField("__path_illustrationFolder", illustrationFolder);
  }
}
