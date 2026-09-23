# history v2.14 — 수정 전 Jenkins 파이프라인 원본 백업

- **날짜**: 2026-09-23
- **영향 저장소**: `mapservice-rest`(문서)
- **이전 버전**: [history_v2.13.md](history_v2.13.md)

## 접속 URL

| 대상 | URL | 비고 |
|---|---|---|
| 공유 웹사이트(발행, 링크 공유 가능) | https://claude.ai/artifact/HhEYu2UmxSko5h8uef7hB9 | 버전 전환기로 v1.0~ 전체 열람 |
| 운영 허브 | https://sj-lab.co.kr/ | 배포 대상 `html/` |
| 운영 지도 | https://sj-lab.co.kr/map/ | 배포 대상 `html/map/` |

## 실행한 프롬프트

```
docs/jenkins/*-original.groovy에 남겨놔줘
```

## 작업 내용

Jenkins 잡에 교체본을 넣기 전, **수정 전 원본**을 저장소에 백업했습니다. Jenkins에 Job Config History 플러그인이 없으면 잡 스크립트를 덮어쓴 뒤 되돌릴 방법이 없기 때문입니다.

| 파일 | 내용 |
|---|---|
| `docs/jenkins/sj-lab-hub-pipeline-original.groovy` | 허브 잡 수정 전 원본. 머리말에 **문제 지점**(`remoteDirectory`가 최종 웹 디렉터리인데 `cleanRemote: true` → 하위 `html/map` 삭제)과 "되돌리면 삭제 문제도 함께 돌아온다"는 경고 명시 |
| `docs/jenkins/sj-lab-mapservice-pipeline-original.groovy` | 지도 잡 수정 전 원본. 이 잡 자체는 허브를 지우지 않으며, 개선 이유(복사 중 빈 화면 구간·반쪽 배포, 저장소 문서 웹 노출)를 머리말에 정리 |
| `docs/deploy-static-sites.md` | 교체본 표에 백업 파일 2개 추가, 허브 잡 요점을 실제 조치(스테이징 경유 + `map/` 제외 동기화)로 갱신 |

원본 본문은 사용자가 제공한 내용을 **그대로** 보존했습니다(들여쓰기·주석 포함). 머리말 주석만 덧붙였습니다.

## 검증

| 확인 | 결과 |
|---|---|
| 교체본 2개 문법 점검 | 중괄호 균형 26/26·18/18, `pipeline {` 블록 정상 |
| 백업본 2개 | 제공된 원문과 동일(주석 헤더만 추가) |
| 운영 현재 상태 | 허브·지도 모두 정상(직전 점검 기준) |

## 남은 작업

- 허브 잡에 교체본(또는 최소 `cleanRemote: false`) 반영 → 1회 배포 → `scripts\check-prod-sites.ps1`로 확인.
- `Verify` 스테이지는 에이전트에서 외부 URL을 호출하므로, 외부 접근이 막혀 있으면 `catchError`로 감싸거나 스테이지를 빼고 로컬 스크립트로 확인.
- 지도 잡 교체본 반영 후 `/map/` 아래 저장소 문서가 404가 되는지 확인.
