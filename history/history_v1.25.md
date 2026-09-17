# history v1.25 — 실서버 첨부 재생 503 해결(QFieldCloud 계정 Secret 주입)

- **날짜**: 2026-09-17
- **영향 저장소**: `sj-lab-k8s-manifests`(Helm 차트)
- **이전 버전**: [history_v1.24.md](history_v1.24.md)

## 접속 URL

| 대상 | URL | 비고 |
|---|---|---|
| 공유 웹사이트(발행, 링크 공유 가능) | https://claude.ai/artifact/HhEYu2UmxSko5h8uef7hB9 | 버전 전환기로 v1.0~ 전체 열람 |
| 공유 웹사이트(로컬 파일) | [file:///C:/developer/workspace/mapservice-rest/history/web/index.html](file:///C:/developer/workspace/mapservice-rest/history/web/index.html) | 오프라인 요약 페이지 |
| 실서버 미디어 API | https://api.sj-lab.co.kr/map/qfield/facilities/{totalId}/media?path=... | 현재 503 → Secret 생성 후 정상화 |

## 실행한 프롬프트

```
로컬에서는 사진, 오디오, 비디오가 잘나오는데 실서버에서는
GET https://api.sj-lab.co.kr/map/qfield/facilities/FACIL_T18_10/media?path=... 503 (Service Unavailable)
```

## 원인

503은 백엔드가 **QFieldCloud 계정이 설정되지 않았을 때 의도적으로 반환하는 코드**입니다(v1.20에서 그렇게 설계). 로컬에서는 환경변수를 넣고 띄워 재생이 됐지만, 운영 차트에는 주입 설정이 없었습니다.

```java
if (username.isBlank() || password.isBlank()) {
    throw new MediaException(MediaError.NOT_CONFIGURED, ...);  // → 503
}
```

## 변경된 결과물

| 파일 | 변경 |
|---|---|
| `mapservice-rest/values.yaml` | `qfield` 블록 추가(baseUrl, Secret 이름·키), 생성 명령을 주석으로 안내 |
| `mapservice-rest/templates/deployment.yaml` | `QFIELD_BASE_URL` 환경변수와 `QFIELD_USERNAME`/`QFIELD_PASSWORD` Secret 참조 추가 |

- **계정 값은 차트에 넣지 않습니다.** Secret 참조만 두고 실제 값은 클러스터의 Secret에 있습니다.
- `optional: true`로 두어 **Secret이 없어도 파드는 정상 기동**하고, 미디어 엔드포인트만 503이 됩니다(다른 API는 영향 없음).

## 남은 작업 — Secret 생성 (클러스터 권한 필요)

로컬에 쿠버네티스 접근 권한이 없어 제가 실행하지 못했습니다. 아래를 실행해 주세요.

```bash
kubectl create secret generic qfield-credentials -n sj-lab \
  --from-literal=username='<QFieldCloud 계정>' \
  --from-literal=password='<비밀번호>'

# 이미 있으면 교체
kubectl create secret generic qfield-credentials -n sj-lab \
  --from-literal=username='<계정>' --from-literal=password='<비밀번호>' \
  --dry-run=client -o yaml | kubectl apply -f -

# 환경변수를 새로 읽도록 재시작
kubectl rollout restart deployment/mapservice-rest -n sj-lab
```

계정 값은 `sj-qfieldsync`의 `qfield_data_sync.py`에 있는 QFieldCloud 계정과 같은 것을 쓰면 됩니다.

## 검증 결과

| 확인 | 결과 |
|---|---|
| `helm lint mapservice-rest` | 통과 (0 failed) |
| `helm template` 렌더링 | `QFIELD_BASE_URL` 값 주입, `QFIELD_USERNAME`/`QFIELD_PASSWORD`가 `qfield-credentials` Secret을 `optional: true`로 참조 |

배포 후 확인:

```
https://api.sj-lab.co.kr/map/qfield/facilities/FACIL_T18_10/media?path=DCIM%2FJPEG_20260916071830596.jpg
→ 200 image/jpeg 면 정상
```

## 참고

- 콘솔의 `캐시된 데이터가 없습니다: convenience_store` 등은 WFS 레이어를 켜지 않은 상태에서 지도를 움직일 때 나오는 **정상 안내 로그**로, 이번 문제와 무관합니다.
