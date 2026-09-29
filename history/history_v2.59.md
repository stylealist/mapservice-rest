# history v2.59 — 공개 API 서비스 Jenkins 파이프라인 정리

- **날짜**: 2026-09-30
- **영향 저장소**: `mapservice-rest`(문서)
- **이전 버전**: [history_v2.58.md](history_v2.58.md)

## 접속 URL

| 대상 | URL | 비고 |
|---|---|---|
| 공유 웹사이트(발행, 링크 공유 가능) | https://claude.ai/artifact/HhEYu2UmxSko5h8uef7hB9 | 작업 로그 |
| Jenkins | https://jenkins.sj-lab.co.kr | 잡 등록은 사람이 |
| 운영 확인용 | https://api.sj-lab.co.kr/open-api/catalog | 배포 후 |

## 실행한 프롬프트

```
커밋 푸시하고 jenkins pipeline 알려줘
```

## 추가한 것

공개 API를 배포하려면 잡이 **두 개** 필요합니다. 하나는 서버(이미지), 하나는 화면(정적 파일)입니다.

| 잡 | 파일 | 흐름 |
|---|---|---|
| `sj-lab-openapi` (서버) | `docs/jenkins/sj-lab-openapi-pipeline.groovy` (신규) | jar 빌드 → 이미지 push → 매니페스트 `image.tag` 커밋 → ArgoCD 동기화 |
| `sj-lab-openapi-web` (화면) | `docs/jenkins/sj-lab-openapi-web-pipeline.groovy` (v2.57에서 추가) | 빌드 → 웹서버 노드 스테이징 → 폴더 바꿔치기 → 확인 |

서버 잡은 다른 백엔드(지도·로그인 서버)와 같은 방식이라, 쓰던 자격 증명(`github_login`, `ncp-api-key`, `GitHub_token`)을 그대로 씁니다.

두 잡 모두 **마지막에 확인 단계**를 넣었습니다. 서버는 카탈로그 응답을, 화면은 페이지 내용을 확인하고
아니면 잡을 실패로 떨어뜨립니다 — 다른 잡이 파일을 지웠거나 배포가 반쪽만 됐을 때 조용히 지나가지 않게 하려는 것입니다.

## 참고

- 여러 서비스가 동시에 매니페스트를 push 하면 한 잡이 `cannot lock ref`로 실패할 수 있습니다. 이미지는 이미
  올라가 있으니 그 잡만 재실행하면 됩니다(2026-09-16 실제 겪음).
- `image.tag`는 Jenkins가 관리하는 값이라 사람이 임의로 되돌리지 않습니다.

## 남은 일 (사람이 해야 하는 것)

1. Jenkins에 잡 2개 등록(위 두 파일을 Pipeline script에 붙여넣기)
2. ArgoCD에 `sj-lab-openapi` Application 등록
3. `api` 스키마 표 2개 생성 + `openapi-db-credentials` Secret → 차트 `apiKey.enabled: true`
