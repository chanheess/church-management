#!/bin/bash
# Usage: ./scripts/worktree-close.sh <이슈번호> [--auto]
# PR 머지 완료 후 워크트리와 로컬·원격 브랜치를 정리합니다.
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

# 커밋 안 한 변경 보호: 미커밋 변경이 있으면 닫지 않는다 (작업 손실 방지).
if [ -n "$(git -C "$WORKTREE_PATH" status --porcelain 2>/dev/null)" ]; then
  echo "⚠️  $WORKTREE_PATH 에 커밋 안 된 변경이 있어 워크트리를 닫지 않습니다:"
  git -C "$WORKTREE_PATH" status --short
  echo "   → 커밋 또는 stash 후 다시 실행하세요: ./scripts/worktree-close.sh $ISSUE_NUM"
  echo "   (정말 버리려면 수동으로: git worktree remove \"$WORKTREE_PATH\" --force)"
  [ "$AUTO" = true ] && exit 0   # 자동(post-merge): 이 워크트리만 건너뛰고 정상 종료
  exit 1                          # 수동: 중단 (사용자가 직접 처리)
fi

# 여기 도달 = 워크트리가 깨끗함 → --force 불필요
echo "▶ 워크트리 제거: $WORKTREE_PATH"
git worktree remove "$WORKTREE_PATH"

# 로컬 브랜치 삭제
if git branch --list "$BRANCH" | grep -q "$BRANCH"; then
  echo "▶ 로컬 브랜치 삭제: $BRANCH"
  git branch -d "$BRANCH" 2>/dev/null || git branch -D "$BRANCH"
fi

# 원격 브랜치 삭제 (머지 완료 후 정리 — GitHub 자동 삭제가 꺼져 있어도 깔끔하게 닫는다)
if git ls-remote --heads origin "$BRANCH" 2>/dev/null | grep -q "$BRANCH"; then
  echo "▶ 원격 브랜치 삭제: origin/$BRANCH"
  git push origin --delete "$BRANCH" 2>/dev/null || echo "  ⚠️ 원격 브랜치 삭제 실패 — 권한/네트워크 확인 후 수동 삭제하세요."
fi

echo ""
echo "✓ 워크트리 정리 완료 (#$ISSUE_NUM)"
echo ""
git worktree list
