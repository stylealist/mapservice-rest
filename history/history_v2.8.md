# history v2.8 — sj-lab-authserver 운영 배포 완료

- **날짜**: 2026-09-22
- **영향 저장소**: `sj-lab-authserver`(Jenkins 파이프라인, 사용자가 직접 Jenkins에 등록), `sj-lab-k8s-manifests`(Helm 차트 신설), 클러스터(ArgoCD Application 연결, Secret 생성 — 사용자가 직접 진행)
- **이전 버전**: [history_v2.7.md](history_v2.7.md)

## 접속 URL

| 대상 | URL | 비고 |
|---|---|---|
| 공유 웹사이트(발행, 링크 공유 가능) | https://claude.ai/artifact/HhEYu2UmxSko5h8uef7hB9 | 버전 전환기로 v1.0~ 전체 열람 |
| 운영 로그인 페이지 | https://api.sj-lab.co.kr/auth/login.html | 200 확인 |
| 운영 세션 확인 | https://api.sj-lab.co.kr/auth/session | 쿠키 없이 401(정상) |

## 진행 경과

v2.6~v2.7에서 로컬까지 완성한 `sj-lab-authserver`를 실제 운영 클러스터에 배포했습니다. Jenkins 파이프라인은 **사용자가 Jenkins UI에 직접 작성**(기존 mapservice-rest 파이프라인을 참고해 변환 — 저장소에는 Jenkinsfile을 커밋하지 않기로 함), Helm 차트는 이 세션에서 `sj-lab-k8s-manifests/sj-lab-authserver/`로 신설, ArgoCD Application 연결과 Secret 생성은 사용자가 클러스터에서 직접 진행했습니다.

### 겪은 문제와 해결

| 문제 | 원인 | 해결 |
|---|---|---|
| `ImagePullBackOff` (`sj-lab-authserver:1` not found) | Jenkins 파이프라인을 아직 안 돌린 상태에서 ArgoCD가 먼저 차트를 동기화해 존재하지 않는 이미지를 당겨오려 함 | Jenkins 잡 실행 순서상 자연스러운 단계 — 실제 빌드·푸시 후 해결 |
| `git commit` 이 "nothing to commit"으로 exit 1 → 파이프라인 FAILURE (그런데 이미지 푸시는 성공) | 첫 Jenkins 빌드 번호가 우연히 차트에 넣어둔 초기 `tag: 1`과 같아 `sed`가 변경을 만들지 못함 | 다음 빌드부터는 빌드 번호가 달라져 자연 해소됨. `git commit ... \|\| echo "No changes to commit"`로 방어하도록 권장 |
| `Error: secret "auth-jwt-secret" not found` | `values.yaml`/`CLAUDE.md`에 적어 둔 대로, Secret을 클러스터에 아직 안 만든 상태에서 파드가 먼저 스케줄됨(의도된 fail-fast — 시크릿 없이 조용히 뜨지 않음) | 사용자가 `kubectl create secret generic auth-jwt-secret -n sj-lab --from-literal=secret="$(openssl rand -base64 32)"` 실행 후 파드 재생성으로 해결 |

## 검증 결과

| 확인 | 결과 |
|---|---|
| `GET https://api.sj-lab.co.kr/auth/login.html` | 200 |
| `GET https://api.sj-lab.co.kr/auth/session`(쿠키 없음) | 401 (예상된 정상 응답) |

## 현재 상태

- `sj-lab-authserver`는 운영에 정상 배포되어 로그인·세션·로그아웃 API가 게이트웨이 경유로 동작합니다.
- **`sj-lab-hub`·`sj-lab-mapservice`의 로그인 게이트는 v2.7 장애 이후 되돌려진 상태 그대로**입니다(운영 접속 불가 장애가 있었던 그 코드) — authserver가 이제 실제로 운영에 떠 있으니, 다시 게이트를 붙이는 작업은 사용자 확인 후 별도로 진행합니다.
- `sj-lab-authserver`의 `AUTH_JWT_SECRET`은 클러스터 Secret으로만 존재하며 이 세션·저장소 어디에도 값이 기록되지 않았습니다.

## 남은 작업

- `sj-lab-hub`·`sj-lab-mapservice`에 로그인 게이트를 다시 적용할지 — 이번엔 authserver가 살아있는 상태에서 진행하되, 재적용 전에 운영에서 직접 한 번 더 확인 후 진행할 것.
- Jenkins 파이프라인의 `git commit` 방어 코드 추가(선택 사항, 위 표 참고).
