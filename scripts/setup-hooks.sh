#!/bin/bash
# 최초 1회 실행: git 훅 경로를 .githooks 디렉토리로 설정합니다.

git config core.hooksPath .githooks
chmod +x .githooks/pre-commit
chmod +x .githooks/prepare-commit-msg
chmod +x .githooks/commit-msg
chmod +x .githooks/post-merge
chmod +x scripts/worktree-new.sh
chmod +x scripts/worktree-done.sh
chmod +x scripts/worktree-close.sh

echo "✓ 훅 설정 완료"
echo "  - .githooks/pre-commit        (컴파일 + 테스트)"
echo "  - .githooks/prepare-commit-msg (이슈 번호 [#N] 자동 삽입)"
echo "  - .githooks/commit-msg         (커밋 메시지 형식 검증)"
echo "  - .githooks/post-merge         (머지된 워크트리 자동 닫기)"
echo "✓ 스크립트 실행 권한 설정 완료"
