# history v1.11 — 시·도 기본값을 서울특별시로 설정

- **날짜**: 2026-09-16
- **영향 저장소**: `sj-lab-mapservice`(프론트엔드)
- **이전 버전**: [history_v1.10.md](history_v1.10.md)

## 접속 URL

| 대상 | URL | 비고 |
|---|---|---|
| 공유 웹사이트(발행, 링크 공유 가능) | https://claude.ai/artifact/HhEYu2UmxSko5h8uef7hB9 | 버전 전환기로 v1.0~ 전체 열람 |
| 공유 웹사이트(로컬 파일) | [file:///C:/developer/workspace/mapservice-rest/history/web/index.html](file:///C:/developer/workspace/mapservice-rest/history/web/index.html) | 오프라인 요약 페이지 |
| 지도 서비스(로컬) | http://localhost:4000 | 첫 화면이 서울로 시작 |

## 실행한 프롬프트

```
시,도 주소의 기본값을 서울특별시로 해줄 수 있어?
```

## 변경된 결과물

| 파일 | 변경 |
|---|---|
| `js/modules/map/map-facility.js` | `DEFAULT_SIDO_CD` 상수 추가, `applyDefaultSido()` 신설, 초기화에서 전체 조회 제거 |
| `docs/ui-conventions.md` | 기본 시·도 규칙과 초기화 시 주의사항 문서화 |

### 동작

- 시도 목록을 받은 뒤 `applyDefaultSido()`가 select 값을 `11`(서울특별시)로 바꾸고 **`change` 이벤트를 직접 발생**시킵니다. 사용자가 직접 고른 것과 같은 경로(구역 extent로 지도 이동 → 시군구 목록 로드 → 시설물 재조회)를 그대로 타므로 로직이 한 벌로 유지됩니다.
- 초기화에서 부르던 `loadFacilities({})`를 **제거**했습니다. 그대로 두면 전체 2,474건을 받은 직후 서울만 다시 받아 **요청이 두 번** 나갑니다.
- 시도 목록 조회가 실패하거나 목록에 `11`이 없으면 기존처럼 전체 조회로 넘어갑니다.

## 검증 결과

Chrome에서 확인했습니다.

| 확인 | 결과 |
|---|---|
| 시·도 select | `서울특별시`(값 `11`) 선택됨 |
| 시·군·구 select | 활성화, 26개 옵션(전체 + 25개 구) |
| 시설물 건수 | **247건** (전체 2,474건 중 서울) |
| 지도 위치 | 서울 영역으로 이동(zoom 11.6) |
| 네트워크 요청 | `admin-area/sido` → `admin-area/sgg?sidoCd=11` → `qfield/facilities?sidoCd=11` **3건만** (전체 조회 없음) |
| 콘솔 오류 | **0건** |

## 참고

- 사용자가 시·도에서 "전체"를 고르면 기존처럼 전국이 조회됩니다.
- 기본 시·도를 바꾸려면 `DEFAULT_SIDO_CD` 상수만 수정하면 됩니다.
- 커밋·push는 하지 않았습니다. **지도 프론트엔드는 현재 실서버에서 서비스되지 않는 상태**(v1.10 확인)라, 배포 복구 후에 반영 여부를 확인해야 합니다.
