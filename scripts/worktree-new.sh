#!/bin/bash
# Usage: ./scripts/worktree-new.sh <issue-number>
# 이슈 번호로 새 git worktree를 생성합니다. 최대 4개 제한.

set -e

ISSUE_NUM=$1
if [ -z "$ISSUE_NUM" ]; then
  echo "사용법: ./scripts/worktree-new.sh <이슈번호>"
  exit 1
fi

ROOT=$(git rev-parse --show-toplevel)
PARENT=$(dirname "$ROOT")
BRANCH="feature/$ISSUE_NUM"
WORKTREE_PATH="$PARENT/$ISSUE_NUM"

# 최대 4개 병렬 워크트리 제한 (메인 제외)
ACTIVE=$(git worktree list | tail -n +2 | wc -l | tr -d ' ')
if [ "$ACTIVE" -ge 4 ]; then
  echo "✗ 최대 4개 워크트리 제한에 도달했습니다."
  echo ""
  git worktree list
  exit 1
fi

# 이미 존재하는 경우
if [ -d "$WORKTREE_PATH" ]; then
  echo "✗ 워크트리가 이미 존재합니다: $WORKTREE_PATH"
  exit 1
fi

git fetch origin --quiet

# 브랜치가 이미 존재하면 체크아웃, 없으면 새로 생성
if git ls-remote --heads origin "$BRANCH" | grep -q "$BRANCH"; then
  git worktree add "$WORKTREE_PATH" "$BRANCH"
else
  git worktree add "$WORKTREE_PATH" -b "$BRANCH" origin/main
fi

echo ""
echo "✓ 워크트리 생성 완료"
echo "  경로  : $WORKTREE_PATH"
echo "  브랜치: $BRANCH"
echo "  이슈  : #$ISSUE_NUM"
echo ""
echo "이동: cd $WORKTREE_PATH"
