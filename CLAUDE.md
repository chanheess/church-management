# CLAUDE.md

이 파일은 Claude Code (claude.ai/code)가 이 저장소에서 작업할 때 참고하는 안내 문서입니다.

## 관련 문서

- **[docs/agent-teams.md](docs/agent-teams.md)** — Agent Teams(병렬 에이전트) 작업 가이드. 워크트리 위에서 여러 에이전트를 병렬로 운영하는 방법.

## 프로젝트 개요

새안양교회를 위한 주보 관리 시스템. 내장 Spring Boot 웹 서버를 가진 데스크톱 애플리케이션으로, 실행 시 브라우저를 자동으로 엽니다. 주간 예배 주보, 월간 출석, 대표기도 순번 관리 기능을 제공합니다.

## 빌드 및 실행 명령어

```bash
# 개발 서버 실행 (http://localhost:8082/bulletin)
./gradlew bootRun

# 테스트 실행
./gradlew test

# macOS 앱으로 패키징
./gradlew packageApp          # → build/dist/ChurchManagement.app

# Windows 설치 파일로 패키징
gradlew.bat packageApp        # → .exe 설치 파일

# Windows 포터블 폴더로 패키징
gradlew.bat packagePortableApp
```

## 아키텍처

**스택:** Java 17, Spring Boot 4.0.0, Thymeleaf, Apache POI, Gradle

**데이터 흐름:** Excel 파일 (`.env`에서 경로 설정) → `ExcelReaderService` → Spring MVC 컨트롤러 → Thymeleaf 템플릿 → 브라우저 UI

**영속성:** DB 없음. 설정은 `~/.church-management/` 아래 두 개의 로컬 파일에 저장:
- `text-config.properties` — 편집 가능한 텍스트 (목사 이름, 공지사항 등)
- `representative-prayer.json` — 대표기도 순번 (교체 및 이름 수정 포함)

**진입점:** `ChurchManagementApplication` — Spring Boot 시작, 시스템 트레이 아이콘 등록, `/bulletin`으로 브라우저 자동 실행

**주요 서비스:**
- `ExcelReaderService` — 연도별 명단 시트 (예: `26년명단`) 파싱. 성도 정보, 셀 그룹, 생일, 출석 데이터 처리
- `RepresentativePrayerService` — 연간 대표기도 순번 생성, 교체 요청 및 이름 수정 처리
- `TextConfigService` — 편집 가능한 텍스트 설정을 홈 디렉토리에서 로드/저장

**컨트롤러:**
- `BulletinController` — `/bulletin`, `/attendance` 서빙, 설정 API 엔드포인트 처리
- `RepresentativePrayerController` — `/representative-prayer` 및 관련 API 서빙

**템플릿** (`src/main/resources/templates/`):
- `weekly-bulletin.html` — 주간 예배 순서 메인 뷰
- `attendance.html` — 셀 그룹별 월간 출석 뷰
- `representative-prayer.html` — 연간 대표기도 순번 뷰

## 설정

환경변수는 `.env` 파일에서 로드됩니다 (git에 커밋되지 않음). 주요 변수:
- `BULLETIN_EXCEL_PATH` — 명단 Excel 파일 경로
- `LOGO_IMAGE_PATH`, `ILLUSTRATION_IMAGE_PATH` — 인쇄용 이미지 파일 경로
- `TEXT_CONFIG_PATH` — 텍스트 설정 파일 경로

서버 포트는 `8082` (`application.yml`에서 설정).

## 패키지 / 모듈 구조

```
com.saeanyang.management
├── ChurchManagementApplication.java   # 진입점, 트레이 아이콘, 브라우저 실행
├── controller/                        # Spring MVC 컨트롤러
├── service/                           # 비즈니스 로직 (Excel, 기도, 설정)
├── model/                             # POJO 모델 (BulletinData, Person, CellGroup, ...)
│   └── representativeprayer/          # 대표기도 순번 모델
└── resources/
    ├── templates/                     # Thymeleaf HTML 템플릿
    └── static/                        # CSS 스타일시트
```

## 이슈 기반 워크트리 워크플로우

> 여러 에이전트가 도메인별로 병렬 작업하려면 [docs/agent-teams.md](docs/agent-teams.md)를 함께 참고한다. 워크트리의 도메인 경계가 그대로 팀원 분담선이 된다.

### 최초 1회 설정
```bash
./scripts/setup-hooks.sh   # 훅 경로 설정 + 실행 권한 부여
```

### 이슈 제목 규칙

이슈 제목은 반드시 `[도메인] 작업 내용` 형식으로 작성합니다.

```
[bulletin] 주보 헤더 이미지 교체
[attendance] 출석 합계 계산 오류 수정
[workflow] 머지 후 워크트리 자동 닫기
```

도메인 목록: `bulletin` | `attendance` | `representative-prayer` | `config` | `workflow`

### 작업 흐름
```
1. GitHub 이슈에 요구사항 작성 (.github/ISSUE_TEMPLATE/ 양식 사용)
   → 제목 형식: [도메인] 작업 내용
2. ./scripts/worktree-new.sh <이슈번호>   # 워크트리 생성
3. docs/agent-journal/날짜-issue-N.md 작성 후 커밋
4. 해당 디렉토리에서 소단위로 커밋
   → pre-commit 훅이 자동으로 컴파일 + 테스트 검증
5. ./scripts/worktree-done.sh            # 브랜치 push + PR 준비
6. 사람이 코드 리뷰 후 머지 (Claude는 main에 직접 머지하지 않음)
   → main에서 git pull 시 post-merge 훅이 워크트리 자동 닫기
```

> 워크트리는 PR 머지 후 `git pull` 시 자동으로 닫힙니다. 자동 닫기가 실패한 경우 `./scripts/worktree-close.sh <이슈번호>`로 수동 처리하세요.
>
> **워크트리를 닫으면 해당 브랜치도 함께 닫습니다(로컬·원격 모두 삭제).** `worktree-close.sh`가 워크트리 제거 → 로컬 브랜치 삭제 → 원격 브랜치(`origin/feature/<번호>`) 삭제까지 한 번에 처리합니다. 머지된 브랜치만 정리되므로 작업 손실은 없습니다.

### 워크트리 분리 기준
다음 조건이 **모두** 충족될 때 워크트리를 분리하여 병렬 작업합니다:
- 서로 관련도가 낮은 다른 도메인의 작업
- 수정 파일이 겹치지 않는 작업
- 최대 **4개** 동시 워크트리 (`worktree-new.sh`가 자동 제한)

### 도메인 경계
| 도메인 | 핵심 파일 |
|--------|----------|
| bulletin | BulletinController, ExcelReaderService, weekly-bulletin.html |
| attendance | AttendanceMember, attendance.html |
| representative-prayer | RepresentativePrayerController/Service, representative-prayer.html |
| config | TextConfigService, EditableTextConfig |

### 커밋 단위 원칙
- 한 커밋 = 하나의 논리적 변경 (기능/수정/리팩토링 중 하나)
- feature 브랜치에만 커밋. main 직접 커밋 금지
- 모든 커밋은 pre-commit 훅(컴파일 + 테스트)을 통과해야 함
- 최종 머지는 사람이 직접 승인 후 처리

### 에이전트 결정 로그 (필수)
자동화로 코드를 작성하고 커밋하기 전에 반드시 `docs/agent-journal/YYYY-MM-DD-issue-N.md` 파일을 작성합니다.
템플릿: `docs/agent-journal/TEMPLATE.md`

기록할 내용:
1. **문제 분석** — 이슈 요구사항과 현재 코드 상태
2. **검토한 방법** — 고려한 접근법과 각각의 장단점
3. **결정 및 근거** — 최종 선택과 그 이유
4. **커밋 목록** — 각 커밋의 해시, 설명, 변경 파일
5. **리뷰 포인트** — 사람이 특히 확인해야 할 부분
6. **남은 불확실성** — 확신하지 못한 사항

저널 파일도 feature 브랜치에 함께 커밋합니다.

## 하네스 현황 및 로드맵

가이드(예방)와 센서(검사) 두 축으로 관리합니다. 싸고 빠른 것은 pre-commit, 무거운 것은 CI에서만 돌립니다.

### 현재 구성 (완료)
| 종류 | 도구 | 타이밍 |
|------|------|--------|
| 가이드 | CLAUDE.md — 스택, 계층 규칙, 워크플로우 | 항상 |
| 가이드 | 이슈 템플릿 — 요구사항 강제 | 이슈 생성 시 |
| 가이드 | 에이전트 결정 로그 — 판단 과정 기록 | 커밋 전 |
| 센서 (계산형) | pre-commit: `./gradlew compileJava` + `test` | 커밋마다 |
| 센서 (계산형) | prepare-commit-msg: 이슈 번호 `[#N]` 자동 삽입 | 커밋마다 |
| 센서 (계산형) | commit-msg: 커밋 메시지 형식 검증 | 커밋마다 |

### 다음 단계 (우선순위 순)
1. **ArchUnit** — 계층 경계 규칙 강제 (Controller → Repository 직접 호출 금지 등)
2. **스티어링 루프** — 같은 실수가 두 번 나오면 즉시 가이드/센서에 반영
3. **CI 파이프라인** — 통합 테스트·무거운 검사는 push 후 CI에서만 실행

## 주의: 자동화의 한계

- **AI가 만든 테스트를 맹신하지 않습니다.** 테스트가 통과해도 테스트 자체가 부실할 수 있습니다. 커버리지 수치보다 테스트가 실제로 의미 있는 검증을 하는지 사람이 판단해야 합니다.
- **하네스의 목표는 사람을 없애는 게 아닙니다.** 명세가 모호하면 어떤 센서도 옳고 그름을 판단할 수 없습니다. 요구사항은 반드시 이슈에 먼저 작성합니다.
- **가이드만 있고 센서가 없으면 의미 없습니다.** 규칙을 적어도 검사하지 않으면 지켜지지 않습니다.

## 향후 방향

`docs/oricle-server-plan.md`에 따르면, OCI VM + PostgreSQL + Docker Compose 기반의 서버 아키텍처로 전환 예정입니다. Spring Security 인증, TOTP 2FA, HP ePrint를 통한 PDF 인쇄, 감사 로그 기능이 추가될 계획입니다. Excel은 데이터 소스로 유지되며, PostgreSQL은 메타데이터와 설정 저장에 사용됩니다.
