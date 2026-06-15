#!/bin/bash
# 스테이지된 "추가 라인"에서 고신뢰 시크릿 패턴을 탐지해 커밋을 차단한다.
# - 추가 라인만 검사 → 기존 코드 오탐 방지
# - ${...} 환경변수·placeholder 값은 제외
# 의도적 우회(예외): git commit --no-verify
set -uo pipefail

ROOT=$(git rev-parse --show-toplevel)
cd "$ROOT"

# 스캐너 자신은 제외(패턴 정의가 들어있음)
SELF=".githooks/secret-scan.sh"

# 고신뢰 패턴 ("이름|정규식") — 거의 오탐 없음
HIGH=(
  "PEM 개인키|-----BEGIN [A-Z0-9 ]*PRIVATE KEY-----"
  "AWS Access Key|AKIA[0-9A-Z]{16}"
  "GitHub 토큰|gh[pousr]_[A-Za-z0-9]{20,}"
  "Slack 토큰|xox[baprs]-[A-Za-z0-9-]{10,}"
  "Google API 키|AIza[0-9A-Za-z_-]{35}"
  "서비스계정 private_key|\"private_key\"[[:space:]]*:"
)

# 따옴표 문자 클래스 ["']
Q="[\"']"
# 일반 시크릿 할당(보수적): 시크릿명 = "12자+ 리터럴"
GENERIC="(password|passwd|secret|secret_key|api[_-]?key|access[_-]?key|token|private[_-]?key|client[_-]?secret)${Q}?[[:space:]]*[:=][[:space:]]*${Q}[^\"'[:space:]]{12,}${Q}"
# 일반 패턴 오탐 제외(환경변수/플레이스홀더)
PLACEHOLDER="(\\\$\\{|your[-_]|example|changeme|placeholder|dummy|sample|xxxx|\\*\\*\\*|<[^>]*>)"

findings=0

while IFS=$'\t' read -r file lineno content; do
  [ "$file" = "$SELF" ] && continue

  for entry in "${HIGH[@]}"; do
    name="${entry%%|*}"
    re="${entry#*|}"
    if printf '%s' "$content" | grep -Eq -- "$re"; then
      echo "  ✗ [$name] $file:$lineno"
      findings=$((findings + 1))
    fi
  done

  if printf '%s' "$content" | grep -Eiq -- "$GENERIC"; then
    if ! printf '%s' "$content" | grep -Eiq -- "$PLACEHOLDER"; then
      echo "  ✗ [의심 시크릿 할당] $file:$lineno"
      findings=$((findings + 1))
    fi
  fi
done < <(git diff --cached --no-color -U0 --diff-filter=ACM | awk '
  /^\+\+\+ b\// { f = substr($0, 7); next }
  /^@@ / { match($0, /\+[0-9]+/); ln = substr($0, RSTART + 1, RLENGTH - 1) + 0; next }
  /^\+/ {
    if ($0 !~ /^\+\+\+/) { print f "\t" ln "\t" substr($0, 2); ln++ }
    next
  }
')

if [ "$findings" -gt 0 ]; then
  echo ""
  echo "✗ [pre-commit] 시크릿 의심 ${findings}건 발견 → 커밋 중단."
  echo "  비밀값은 .env / 환경변수 / Compose secret 으로 옮기세요."
  echo "  오탐이라면: git commit --no-verify (의도적 예외만)"
  exit 1
fi

exit 0
