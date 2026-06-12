#!/bin/bash
# Usage: ./scripts/worktree-close.sh <이슈번호> [--auto]
# PR 머지 완료 후 워크트리와 로컬 브랜치를 정리합니다.
# --auto: post-merge 훅 등 자동 실행 시 대화형 프롬프트를 건너뜁니다.

set -e

ISSUE_NUM=$1
AUTO=false
[ "${2:-}" = "--auto" ] && AUTO=true

if [ -z "$ISSUE_NUM" ]; then
  echo "사용법: ./scripts/worktree-close.sh <이슈번호> [--auto]"
  echo "  예: ./scripts/worktree-close.sh 12"
  exit 1
fi

ROOT=$(git rev-parse --show-toplevel)
PARENT=$(dirname "$ROOT")
BRANCH="feature/$ISSUE_NUM"
WORKTREE_PATH="$PARENT/$ISSUE_NUM"

# 워크트리 존재 확인
if ! git worktree list | grep -q "$WORKTREE_PATH"; then
  echo "✗ 워크트리를 찾을 수 없습니다: $WORKTREE_PATH"
  git worktree list
  exit 1
fi

# PR이 머지됐는지 확인 (gh CLI 있고, 자동 실행이 아닌 경우)
if [ "$AUTO" = false ] && command -v gh &>/dev/null; then
  PR_STATE=$(gh pr view "$BRANCH" --json state -q '.state' 2>/dev/null || echo "UNKNOWN")
  if [ "$PR_STATE" = "OPEN" ]; then
    echo "⚠️  PR이 아직 열려 있습니다 (OPEN). 머지 후 닫으세요."
    echo "    계속하려면 Enter, 취소하려면 Ctrl+C"
    read -r
  fi
fi

# 워크트리 제거
echo "▶ 워크트리 제거: $WORKTREE_PATH"
git worktree remove "$WORKTREE_PATH" --force

# 로컬 브랜치 삭제
if git branch --list "$BRANCH" | grep -q "$BRANCH"; then
  echo "▶ 로컬 브랜치 삭제: $BRANCH"
  git branch -d "$BRANCH" 2>/dev/null || git branch -D "$BRANCH"
fi

echo ""
echo "✓ 워크트리 정리 완료 (#$ISSUE_NUM)"
echo ""
git worktree list
