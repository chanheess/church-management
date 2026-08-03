# 서버 보안 작업 계획

## Summary

GitHub issue #3 `[보안] 서버 보안 작업`을 기준으로 서버 전환 전에 우선 처리할 보안 계획을 정리한다.
서비스는 관리자만 접근 가능한 구조로 전환하고, 로그인 보호, 이메일 인증 기반 신뢰기기, 개인정보 AES 암호화, 접근 로그, 다운로드/출력 로그를 1차 보안 범위로 둔다.

기존 `docs/oricle-server-plan.md`의 OCI 서버 전환 방향은 유지하되, 이 문서는 보안 구현을 세부적으로 나누기 위한 기준 문서로 사용한다.

## Goals

- 서버에 배포된 모든 화면과 API는 관리자 로그인 후에만 접근할 수 있어야 한다.
- 계정/권한 모델은 `admin` 단일 권한으로 시작한다.
- 새 기기 로그인은 이메일 인증을 통과해야 한다.
- 이메일 인증을 통과한 기기는 30일 동안 신뢰기기로 저장한다.
- 전화번호와 생년월일은 DB에 평문으로 저장하지 않는다.
- 개인정보 접근, 다운로드, 출력 행위는 추적 가능한 로그로 남긴다.
- 로그에는 전화번호와 생년월일 원문이 남지 않아야 한다.

## Security Scope

### 1. 관리자 전용 접근

- Spring Security를 도입한다.
- 모든 화면과 API의 기본 정책은 인증 필요로 둔다.
- `/login`, 정적 리소스, 헬스체크처럼 명확히 필요한 엔드포인트만 예외로 둔다.
- 기존 공개 화면인 `/bulletin`, `/attendance`, `/representative-prayer`도 관리자 로그인 후 접근하도록 전환한다.
- API 요청은 인증되지 않은 경우 401 또는 로그인 페이지 이동으로 처리한다.
- 인가 모델은 `admin` 하나로 통일한다.
- `ADMIN`, `MEMBER_MANAGER`, `MEMBER_VIEWER` 같은 세분화 권한은 1차 범위에서 제외한다.

### 2. 관리자 로그인 보호

- 관리자 계정은 이메일과 비밀번호로 로그인한다.
- 비밀번호는 BCrypt 또는 Spring Security가 권장하는 password encoder로 해시 저장한다.
- 강한 비밀번호 정책을 적용한다.
- 로그인 실패 횟수를 기록하고, 일정 횟수 이상 실패하면 임시 잠금 또는 지연을 적용한다.
- 세션 만료 시간을 설정한다.
- 세션 쿠키는 `Secure`, `HttpOnly`, `SameSite=Lax` 또는 `SameSite=Strict`로 설정한다.
- HTTPS 운영 환경에서만 운영 로그인을 허용한다.

### 3. 이메일 인증과 신뢰기기

- 관리자 계정에는 이메일 주소를 필수 등록한다.
- 이메일/비밀번호 로그인 후 새 기기 또는 신뢰 기간이 만료된 기기에서는 이메일 인증 코드를 요구한다.
- 이메일 인증 코드는 6자리 숫자로 발급하고 10분 뒤 만료한다.
- 이메일 인증 실패는 최대 5회로 제한한다.
- 이메일 인증 성공 후 해당 기기를 30일 신뢰기기로 등록한다.
- 신뢰기기 토큰은 브라우저 쿠키에 저장한다.
- 신뢰기기 쿠키는 `Secure`, `HttpOnly`, `SameSite` 속성을 적용한다.
- 서버 DB에는 신뢰기기 토큰 원문을 저장하지 않고 해시값만 저장한다.
- 신뢰기기에는 만료일, 마지막 사용 시각, IP, User-Agent 요약값을 저장한다.
- 관리자는 본인의 신뢰기기 목록을 조회하고 해제할 수 있어야 한다.
- 회원가입은 지정된 IP 또는 CIDR에서만 허용한다.

### 4. 전화번호/생년월일 AES 암호화

- 전화번호와 생년월일은 DB 저장 시 AES-GCM으로 암호화한다.
- 암호화 키는 코드, Git, DB에 저장하지 않는다.
- 운영 환경에서는 환경변수 또는 secret 파일로 암호화 키를 주입한다.
- 암호문에는 nonce/iv, ciphertext, auth tag를 함께 저장할 수 있는 포맷을 사용한다.
- 전화번호 검색이 필요하면 정규화된 전화번호에 대한 HMAC 인덱스 컬럼을 별도로 둔다.
- 생년월일은 복호화 조회 기준으로 처리하고, DB 평문 검색은 지원하지 않는다.
- 이름은 화면 표시와 식별에 필요하므로 1차 범위에서는 평문 저장을 허용한다.
- 이름, 전화번호, 생년월일 원문은 애플리케이션 로그와 감사 로그에 기록하지 않는다.

### 5. 접근 로그

- 접근 로그는 관리자 행위를 추적하기 위해 별도 테이블 또는 별도 로깅 채널에 저장한다.
- 로그인 성공/실패를 기록한다.
- 로그아웃을 기록한다.
- 세션 만료 또는 인증 실패를 기록한다.
- 이메일 인증 코드 발송, 성공, 실패, 만료를 기록한다.
- 신뢰기기 등록, 사용, 해제를 기록한다.
- 개인정보가 포함된 화면/API 조회를 기록한다.
- 각 로그에는 관리자 ID, 요청 경로, HTTP 메서드, IP, User-Agent, 결과, 발생 시각을 저장한다.
- 전화번호와 생년월일 원문은 접근 로그에 저장하지 않는다.

### 6. 다운로드/출력 로그

- 다운로드와 출력은 일반 접근 로그와 분리해 감사 로그 성격으로 저장한다.
- 엑셀 import 시 import 로그를 남긴다.
- 파일 다운로드 시 다운로드 로그를 남긴다.
- PDF 생성 시 생성 로그를 남긴다.
- 출력 요청, 출력 성공, 출력 실패를 출력 로그로 남긴다.
- 다운로드 로그에는 관리자 ID, 대상 종류, 건수, IP, User-Agent, 시각을 저장한다.
- 출력 로그에는 관리자 ID, 출력 종류, 페이지 수, 대상 프린터 식별자, 성공 여부, 실패 사유, 시각을 저장한다.
- 다운로드 파일 내용, 출력물 내용, 전화번호/생년월일 원문은 로그에 저장하지 않는다.

## Data Model Draft

### `users`

- `id`
- `password_hash`
- `email`
- `role`: 고정값 `admin`
- `enabled`
- `failed_login_count`
- `locked_until`
- `last_login_at`
- `created_at`
- `updated_at`

### `email_verification_codes`

- `id`
- `user_id`
- `code_hash`
- `attempts`
- `expires_at`
- `consumed_at`
- `created_at`

### `trusted_devices`

- `id`
- `user_id`
- `token_hash`
- `user_agent_hash`
- `last_ip`
- `trusted_until`
- `last_used_at`
- `created_at`
- `revoked_at`

### `member_private_data`

- `id`
- `name`
- `phone_encrypted`
- `phone_hmac`
- `birth_date_encrypted`
- `cell_name`
- `group_name`
- `status`
- `source_import_batch_id`
- `created_at`
- `updated_at`

### `member_import_batches`

- `id`
- `source_file_name`
- `source_file_hash`
- `imported_by_admin_id`
- `total_rows`
- `success_rows`
- `failed_rows`
- `status`
- `created_at`

### `access_logs`

- `id`
- `user_id`
- `event_type`
- `path`
- `http_method`
- `ip_address`
- `user_agent`
- `result`
- `created_at`

### `download_logs`

- `id`
- `user_id`
- `download_type`
- `target_label`
- `row_count`
- `ip_address`
- `user_agent`
- `created_at`

### `print_logs`

- `id`
- `user_id`
- `print_type`
- `page_count`
- `printer_identifier`
- `result`
- `failure_reason`
- `created_at`

### DB Index Notes

- PostgreSQL을 사용할 경우 부분 인덱스를 적극 활용한다.
- 해제되지 않은 신뢰기기 토큰만 중복 방지한다.
  - 예: `UNIQUE INDEX ON trusted_devices(token_hash) WHERE revoked_at IS NULL`
- 실패 로그인 로그만 빠르게 조회할 수 있게 한다.
  - 예: `INDEX ON access_logs(created_at DESC) WHERE event_type = 'LOGIN_FAILED'`
- 최근 다운로드/출력 이력 조회를 위해 관리자 ID와 생성 시각 기준 인덱스를 둔다.
  - 예: `INDEX ON download_logs(user_id, created_at DESC)`
  - 예: `INDEX ON print_logs(user_id, created_at DESC)`
- 전화번호 검색을 지원하는 경우, 암호문이 아니라 `phone_hmac`에 인덱스를 둔다.
  - 예: `INDEX ON member_private_data(phone_hmac) WHERE phone_hmac IS NOT NULL`
- 부분 인덱스는 모든 행이 아니라 실제 조회 대상 행만 인덱싱하므로 로그/신뢰기기 테이블이 커져도 조회 비용을 줄일 수 있다.

## Public Interfaces

- `/login`: 관리자 로그인
- `/logout`: 관리자 로그아웃
- `/admin`: 관리자 홈
- `/admin/security/trusted-devices`: 신뢰기기 조회/해제
- `/admin/security/access-logs`: 접근 로그 조회
- `/admin/members/import`: 엑셀 import
- `/admin/downloads`: 다운로드 실행 및 이력 조회
- `/admin/print-jobs`: 출력 실행 및 이력 조회
- `/bulletin`: 관리자 인증 후 주보 화면
- `/attendance`: 관리자 인증 후 출석 화면
- `/representative-prayer`: 관리자 인증 후 대표기도 화면

## Implementation Phases

### Phase 1. 인증 기본 보호

- Spring Security 의존성을 추가한다.
- 모든 화면/API에 인증 필요 정책을 적용한다.
- 로그인/로그아웃 화면과 기본 세션 정책을 추가한다.
- 최초 관리자 계정 생성 방식을 정한다.
- 기존 화면이 로그인 후 정상 접근되는지 확인한다.

### Phase 2. 이메일 인증과 신뢰기기

- 이메일 인증 코드 발송/검증 흐름을 추가한다.
- 신뢰기기 쿠키와 DB 저장 구조를 추가한다.
- 신뢰기기 등록, 사용, 만료, 해제를 구현한다.
- 관리자 신뢰기기 관리 화면을 추가한다.
- 지정 IP/CIDR에서만 회원가입할 수 있게 한다.

### Phase 3. 개인정보 DB 저장과 AES 암호화

- 개인정보 저장 테이블을 추가한다.
- AES-GCM 암호화 유틸리티를 추가한다.
- 전화번호와 생년월일 저장 시 암호화한다.
- 전화번호 검색이 필요하면 HMAC 인덱스를 추가한다.
- 엑셀 import 시 DB에 암호화 저장하도록 전환한다.
- 기존 화면은 엑셀 직접 조회 대신 DB 조회 결과를 사용하도록 전환한다.

### Phase 4. 접근/감사 로그

- 로그인, 로그아웃, 인증 실패, 이메일 인증, 신뢰기기 이벤트 로그를 추가한다.
- 개인정보 조회 요청에 접근 로그를 추가한다.
- 다운로드 로그를 추가한다.
- 출력 로그를 추가한다.
- 로그 조회 화면에서 개인정보 원문이 노출되지 않도록 한다.

### Phase 5. 운영 HTTPS 보안

- OCI reverse proxy에서 HTTPS를 강제한다.
- 애플리케이션 서버 프로필에 forwarded header 처리를 설정한다.
- 운영 환경에서 Secure 쿠키가 적용되는지 확인한다.
- 실제 클라이언트 IP가 접근 로그에 남는지 확인한다.

## Test Plan

- 비로그인 상태에서 `/bulletin`, `/attendance`, `/representative-prayer` 접근이 차단되는지 확인한다.
- 비로그인 API 요청이 401 또는 로그인 이동으로 처리되는지 확인한다.
- 관리자 로그인 성공/실패가 정상 기록되는지 확인한다.
- 로그인 실패 제한이 동작하는지 확인한다.
- 지정 IP/CIDR 밖에서는 회원가입이 차단되는지 확인한다.
- 새 기기 로그인 시 이메일 인증 코드가 요구되는지 확인한다.
- 이메일 인증 성공 후 30일 동안 신뢰기기로 처리되는지 확인한다.
- 신뢰기기 만료 또는 해제 후 다시 이메일 인증이 요구되는지 확인한다.
- 신뢰기기 쿠키에 `Secure`, `HttpOnly`, `SameSite` 속성이 적용되는지 확인한다.
- DB에 전화번호와 생년월일이 평문으로 저장되지 않는지 확인한다.
- 전화번호 검색용 HMAC 컬럼이 원문 전화번호를 노출하지 않는지 확인한다.
- 애플리케이션 로그와 접근 로그에 전화번호/생년월일 원문이 없는지 확인한다.
- 엑셀 import 후 개인정보가 암호화 저장되고 기존 화면이 정상 표시되는지 확인한다.
- 다운로드 실행 시 다운로드 로그가 남는지 확인한다.
- 출력 실행 시 출력 로그에 출력 종류, 페이지 수, 결과, 실패 사유가 남는지 확인한다.
- HTTPS reverse proxy 뒤에서 실제 클라이언트 IP가 접근 로그에 남는지 확인한다.

## Open Decisions

- 최초 관리자 계정 생성 방식: 환경변수 bootstrap 또는 1회용 setup token 중 선택 필요.
- 운영 SMTP 제공자와 발신 메일 주소 선택 필요.
- 전화번호 검색 필요 여부: 필요하면 HMAC 인덱스 필수, 필요 없으면 암호문만 저장 가능.
- 다운로드 허용 범위: 전체 명단 다운로드를 허용할지, 출력/PDF만 허용할지 결정 필요.

## Assumptions

- 서비스 사용자는 관리자뿐이다.
- 권한 모델은 `admin` 단일 권한으로 시작한다.
- 전화번호와 생년월일만 1차 AES 암호화 대상으로 고정한다.
- 이름은 1차 범위에서 평문 저장을 허용하지만 로그에는 남기지 않는다.
- 엑셀은 초기/관리 import 원본으로 유지한다.
- 서버 화면의 개인정보 원본은 최종적으로 DB가 된다.
- 운영 서버는 HTTPS reverse proxy 뒤에서 실행된다.
