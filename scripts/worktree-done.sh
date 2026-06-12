#!/bin/bash
# Usage: ./scripts/worktree-done.sh
# 현재 워크트리의 작업을 마무리하고 사람 리뷰를 위한 PR을 준비합니다.

set -e

BRANCH=$(git rev-parse --abbrev-ref HEAD)
ROOT=$(git rev-parse --show-toplevel)

if [ "$BRANCH" = "main" ]; then
  echo "✗ main 브랜치에서는 실행할 수 없습니다."
  exit 1
fi

# 커밋되지 않은 변경사항 확인
if ! git diff --quiet || ! git diff --cached --quiet; then
  echo "✗ 커밋되지 않은 변경사항이 있습니다. 먼저 커밋하세요."
  git status --short
  exit 1
fi

# 브랜치 push
echo "▶ 브랜치를 원격에 push합니다... ($BRANCH)"
git push origin "$BRANCH" 2>&1

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "  변경 내역 요약 (main 대비)"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
git log origin/main..HEAD --oneline
echo ""
echo "변경 파일:"
git diff origin/main...HEAD --stat
echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "⚠️  머지 전 사람이 코드를 반드시 확인하세요."
echo ""

# gh CLI가 있으면 PR 자동 생성 안내
if command -v gh &>/dev/null; then
  ISSUE_NUM=$(echo "$BRANCH" | grep -o '[0-9]*$')
  echo "PR 생성 명령어:"
  echo "  gh pr create --title \"fix: #$ISSUE_NUM\" --body \"closes #$ISSUE_NUM\" --base main"
else
  echo "GitHub에서 브랜치 '$BRANCH' 로 PR을 직접 생성하세요."
fi

echo ""
echo "머지 완료 후 워크트리를 반드시 닫아주세요:"
MAIN_ROOT=$(git rev-parse --show-toplevel)
echo "  cd $MAIN_ROOT && ./scripts/worktree-close.sh $ISSUE_NUM"
echo ""
echo "✓ 리뷰 준비 완료 — 승인 후 머지 → 워크트리 닫기 순서로 진행하세요."
