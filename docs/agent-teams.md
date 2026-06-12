# Agent Teams — 병렬 에이전트 작업 가이드

> 참고: [Claude Code Agent Teams (daleseo.com)](https://daleseo.com/claude-code-agent-teams/)
>
> 이 문서는 `CLAUDE.md`의 [이슈 기반 워크트리 워크플로우](../CLAUDE.md)를 보완한다.
> 워크트리가 **파일 단위 격리**를 제공한다면, Agent Teams는 그 위에서 **여러 에이전트가
> 병렬로 협업**하게 하는 상위 계층이다.

---

## 1. Agent Teams란?

여러 Claude Code 인스턴스가 하나의 팀으로 협력하는 기능. **팀 리드(메인 세션)**가 전체를
조율하고, 각 팀원은 **독립적인 컨텍스트 윈도우**를 가지며 **공유 태스크 리스트**와
**메일박스(mailbox)** 로 서로 소통한다.

### Sub-agents 와의 차이

| 구분 | Sub-agents | Agent Teams |
|------|-----------|------------|
| 소통 | 결과만 메인에 보고 | 팀원끼리 직접 대화 |
| 조율 | 메인이 전부 관리 | 공유 태스크로 자율 조율 |
| 용도 | 단순·단발 작업 | 협업·토론이 필요한 작업 |
| 비용 | 낮음 | 높음 (독립 인스턴스마다 토큰 소모) |

> 단순 검색·단발 작업이면 sub-agent로 충분하다. **여러 관점의 토론·교차 검증이 필요할
> 때만** 팀을 만든다.

---

## 2. 활성화

```jsonc
// .claude/settings.json
{
  "env": {
    "CLAUDE_CODE_EXPERIMENTAL_AGENT_TEAMS": "1"
  }
}
```

> 이 저장소는 이미 `.claude/settings.json`을 관리하므로, 위 `env` 항목을 추가하면 된다.

### 디스플레이 모드

- **In-process** — 모든 팀원이 메인 터미널에서 실행. `Shift+Up/Down`으로 팀원 선택.
- **Split panes** — 팀원마다 별도 패널 (tmux / iTerm2 필요). Windows Terminal·VS Code 미지원.

---

## 3. 이 프로젝트에서의 활용

이 저장소는 **이슈 기반 워크트리**(`scripts/worktree-new.sh`)로 도메인을 격리한다.
Agent Teams는 그 경계를 그대로 팀원 분담선으로 쓴다.

### 도메인 ↔ 팀원 매핑

`CLAUDE.md`의 도메인 경계를 팀원 단위로 나누면 파일 충돌 없이 병렬 작업이 가능하다.

| 팀원 | 담당 도메인 | 핵심 파일 |
|------|------------|----------|
| 팀원 A | bulletin | `BulletinController`, `ExcelReaderService`, `weekly-bulletin.html` |
| 팀원 B | attendance | `AttendanceMember`, `attendance.html` |
| 팀원 C | representative-prayer | `RepresentativePrayer*`, `representative-prayer.html` |
| 팀원 D | config | `TextConfigService`, `EditableTextConfig` |

> **원칙: 한 팀원 = 한 워크트리 = 한 도메인.** 서로 다른 파일을 만지므로 충돌이 없다.
> 워크트리는 최대 4개이므로 팀원도 동시에 최대 4명까지가 자연스럽다.

### 팀 구성 예시 (프롬프트)

```
"여러 도메인을 병렬로 다듬을 에이전트 팀을 만들어줘:
- 팀원 A: bulletin 도메인 — 엑셀 파싱 엣지 케이스 보강
- 팀원 B: attendance 도메인 — 월간 출석 집계 버그 수정
각자 별도 워크트리에서 작업하고, 커밋 전 docs/agent-journal 저널을 먼저 써라."
```

### 병렬 코드 리뷰 / 경쟁 가설 조사

- **병렬 리뷰** — 보안 · 성능 · 테스트 커버리지를 세 팀원이 동시에 검토.
- **경쟁 가설** — "이 버그 원인을 서로 다른 가설로 조사하고, 서로의 가설을 반증해봐."

---

## 4. 모범 사례

- **충분한 컨텍스트 전달** — 팀원은 독립 컨텍스트라 시작 시 거의 백지다. 이슈 번호,
  대상 파일, 기대 결과물을 프롬프트에 명시한다.
- **결과물이 명확한 태스크** — 함수 / 테스트 파일 단위처럼 "끝났는지" 판단 가능한 크기로.
- **파일 충돌 방지** — 팀원끼리 같은 파일을 만지지 않게 도메인 경계로 나눈다.
- **계획 승인** — 구현 전 각 팀원의 계획을 리드가 검토·승인한다.
- **완료까지 대기** — 팀원이 끝나기 전에 머지·정리하지 않는다.
- **저널 우선** — 이 저장소 규칙대로, 팀원도 커밋 전
  `docs/agent-journal/YYYY-MM-DD-issue-N.md`를 먼저 작성한다.

---

## 5. 제한사항

- In-process 팀원은 **세션 재개 불가**.
- 한 세션당 **하나의 팀**만 운영 가능.
- **중첩 불가** — 팀원이 또 다른 팀을 만들 수 없다.
- Split panes는 Windows Terminal · VS Code에서 미지원.

---

## 6. 이 프로젝트의 불변 규칙 (재확인)

Agent Teams를 써도 다음은 그대로 적용된다:

- **main 직접 커밋·머지 금지.** 모든 작업은 feature 브랜치 / 워크트리에서.
- 최종 머지는 **사람이 직접 승인**한다 (Claude는 main에 머지하지 않는다).
- 모든 커밋은 **pre-commit 훅(컴파일 + 테스트)** 을 통과해야 한다.
- 작업이 끝난 워크트리는 `scripts/worktree-close.sh`로 **반드시 닫는다** (4개 제한).
