# history v2.58 — 활용 페이지에서 로그인 후 돌아오지 못하던 문제 수정

- **날짜**: 2026-09-29
- **영향 저장소**: `sj-lab-authserver`, `mapservice-rest`(문서·기록)
- **이전 버전**: [history_v2.57.md](history_v2.57.md)

## 접속 URL

| 대상 | URL | 비고 |
|---|---|---|
| 공유 웹사이트(발행, 링크 공유 가능) | https://claude.ai/artifact/HhEYu2UmxSko5h8uef7hB9 | 작업 로그 |
| 활용 페이지(로컬) | http://localhost:4100 | 로그인 후 복귀 확인 |
| 로그인 페이지(로컬) | http://localhost:8100/auth/login.html | |

## 실행한 프롬프트

```
커밋 푸시해주고 openapi 페이지 접속하면 이미지처럼만 나와
(로그인 화면에 "demo님, 로그인되었습니다."만 뜨고 멈춤)
```

## 원인 — 돌아갈 주소가 허용 목록에 없었다

로그인 페이지는 **돌아갈 주소(`redirect_uri`)를 미리 정해 둔 목록과 대조**합니다. 아무 주소로나 돌려보내면
토큰이 엉뚱한 사이트로 새기 때문입니다(오픈 리다이렉트 방지).

목록에 `localhost:3000`(허브)·`localhost:4000`(지도)·운영 주소만 있고 **새로 만든 활용 페이지(4100)가 빠져 있어서**,
로그인은 성공했지만 돌아가지 못하고 "demo님, 로그인되었습니다."에서 멈춘 것입니다.

## 수정

- `sj-lab-authserver/src/main/resources/static/login.html` — 허용 목록에 `http://localhost:4100` 추가(주석으로 어느 사이트인지 표기).
  **운영은 `sj-lab.co.kr` 하위 경로라 추가가 필요 없습니다.**
- `sj-lab-authserver/CLAUDE.md`, `mapservice-rest/docs/system-architecture.md` — "새 프론트를 추가하면 이 목록도 같이 고칠 것,
  빠뜨리면 이런 증상이 난다"를 증상과 함께 적어 뒀습니다.

## 검증 (로컬, 헤드리스 Chrome)

| 단계 | 결과 |
|---|---|
| 로그인 안 한 채 `localhost:4100` 접속 | 로그인 화면으로 이동(`redirect_uri=...4100`) |
| 체험용 계정으로 로그인 | **`http://localhost:4100/` 으로 복귀** |
| 복귀 후 화면 | 계정 `demo` 저장됨, API 목록 12개 표시 |

## 참고 — 재기동 중 잠깐 500

로그인 서버를 새로 띄우면 Eureka에 죽은 인스턴스가 남아 게이트웨이 요청의 일부가 500이 됩니다.
죽은 인스턴스를 해제하고 다시 확인해 정상으로 만들었습니다(기존에 문서화된 현상).
