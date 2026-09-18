# 보고서 PDF 한글 폰트

`NanumGothic.ttf` — 내업 처리 보고서 PDF(`QfieldReportServiceImpl`)에 **임베드**하는 한글 폰트입니다.

## 왜 저장소에 포함하나

운영 이미지는 `eclipse-temurin:17-jdk-alpine` 기반이라 한글 폰트가 없습니다. 처음에는 Dockerfile 에서
`apk add font-nanum` 으로 설치하려 했지만 **Alpine 저장소에 그 패키지가 없어 빌드가 실패**했습니다
(2026-09-18 Jenkins: `font-nanum (no such package)`). 폰트를 JAR 안에 넣으면 이미지·배포 환경과
무관하게 한글이 항상 정상 출력됩니다.

## 로드 순서 (`QfieldReportServiceImpl#getBaseFont`)

1. `REPORT_FONT_PATH` 환경변수로 지정한 파일
2. **이 클래스패스 리소스(`fonts/NanumGothic.ttf`)** — 기본 경로
3. OS 에 설치된 폰트 경로(로컬 개발용: 윈도우 맑은 고딕 등)
4. 모두 없으면 임베드 없이 CJK 폰트로 생성하고 경고 로그 (글자가 깨질 수 있음)

폰트를 바꾸려면 이 파일을 교체하고 라이선스 파일도 함께 갱신하세요.

## 라이선스

나눔고딕(NanumGothic)은 네이버가 배포하는 글꼴로 **SIL Open Font License 1.1** 을 따릅니다.
같은 폴더의 `OFL.txt` 를 참고하세요. OFL 은 재배포를 허용하며, 글꼴 파일 자체를 판매하지 않는 한
애플리케이션에 포함해 배포할 수 있습니다.
