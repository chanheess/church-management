# church-management
교회 업무 관리 프로그램

모든 코드는 Claude, Cursor AI로 작성되었습니다.

## Packaging

- macOS 기본 앱 번들: `./gradlew packageApp`
- Windows 기본 배포본(설치형 `.exe`): `gradlew.bat packageApp`
- Windows 폴더형 배포본(app-image): `gradlew.bat packagePortableApp`
- Windows MSI 설치파일: `gradlew.bat packageWindowsMsi`

Windows에서는 폴더형 `app-image`의 `exe`만 따로 옮기면 동작하지 않습니다.
배포는 기본적으로 설치형 `.exe`를 사용하세요.
