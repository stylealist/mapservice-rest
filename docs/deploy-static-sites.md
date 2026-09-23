# 정적 프론트(허브 · 지도) 배포와 "지도가 지워지는" 문제

`sj-lab-hub`(첫 화면)와 `sj-lab-mapservice`(지도)는 컨테이너가 아니라 **웹서버 노드의 한 디렉터리에 파일로 복사**되어 서빙됩니다. 두 사이트가 같은 트리를 공유하기 때문에 **한쪽 배포가 다른 쪽을 통째로 지울 수 있고**, 실제로 그런 일이 반복됐습니다. 이 문서는 구조·원인·안전한 배포 스테이지·확인 방법을 정리합니다.

## 디렉터리 구조

```
/home/kuber-volume/sj-lab-webserver/html/          ← 허브(sj-lab-hub)   → https://sj-lab.co.kr/
/home/kuber-volume/sj-lab-webserver/html/map/      ← 지도(sj-lab-mapservice) → https://sj-lab.co.kr/map/
```

- 지도는 **허브 디렉터리의 하위 폴더**입니다. 별도 볼륨이 아닙니다.
- nginx 설정(`sj-lab-k8s-manifests/sj-lab-webserver/files/nginx-set.conf`)은 `root /home/www`(= 위 html)에 `try_files $uri $uri/ /index.html`을 씁니다.

## 왜 지도가 "지워진" 것처럼 보이나

1. **지도 배포 잡**은 자기 폴더만 비웁니다 — `rm -rf /home/kuber-volume/sj-lab-webserver/html/map/*` 후 복사. 허브에는 영향이 없습니다.
2. **허브 배포 잡이 원인입니다.** 빌드 산출물을 최종 위치로 바로 전송하면서 `cleanRemote`를 켜 두었습니다(2026-09-23 실제 잡 확인).

   ```groovy
   remoteDirectory: "/home/kuber-volume/sj-lab-webserver/html"
   cleanRemote: true      // 전송 전에 이 디렉터리의 파일·하위 디렉터리를 모두 삭제
   ```

   Publish over SSH의 `cleanRemote`는 **대상 디렉터리를 통째로 비운 뒤** 전송합니다. 대상이 `html` 자체이므로 그 하위의 `map/`(지도 사이트 전체)이 매 허브 배포마다 삭제되고, 지도 잡이 다시 돌 때까지 복구되지 않습니다. 지도 잡에도 `cleanRemote: true`가 있지만 대상이 스테이징(`/respal/deploy`)이라 무해합니다.
3. 게다가 nginx의 SPA 폴백 때문에 **증상이 404로 드러나지 않습니다**. `map/`이 사라지면 `/map/` 요청이 폴백으로 넘어가 **허브 첫 화면이 HTTP 200으로** 표시됩니다. "지도가 안 뜨고 허브가 뜬다"가 곧 "파일이 지워졌다"는 신호입니다.

> 확인된 사실: 2026-09-23 기준 `/` 와 `/map/` 은 정상(각자 제 파일). `/map/` 의 파일들은 Last-Modified 가 전부 동일 — 배포가 트리 전체를 한 번에 덮어쓰는 방식임을 보여줍니다.

## 잡 전체 교체본

바로 붙여 넣을 수 있는 파이프라인 전문을 저장소에 두었습니다(Jenkins 잡의 `[Pipeline script]` 내용).

| 잡 | 파일 | 요점 |
|---|---|---|
| 허브 | `docs/jenkins/sj-lab-hub-pipeline.groovy` | **전송 대상을 스테이징으로 돌리고 `map/` 제외하고 동기화**(필수), 원본의 `tools`·`npm install`·`post`는 그대로 |
| 지도 | `docs/jenkins/sj-lab-mapservice-pipeline.groovy` | 원자적 교체(새 폴더 → `mv`), 저장소 문서 제외, 배포 후 확인 |
| (백업) 허브 원본 | `docs/jenkins/sj-lab-hub-pipeline-original.groovy` | 수정 전 원본 — **롤백용**. 되돌리면 삭제 문제도 함께 돌아옴 |
| (백업) 지도 원본 | `docs/jenkins/sj-lab-mapservice-pipeline-original.groovy` | 수정 전 원본 — 롤백용 |

두 잡 모두 마지막에 `Verify` 단계가 있어, 다른 잡이 상대 사이트를 지웠다면 **그 잡이 실패로 드러납니다**. 아래는 그 안에서 핵심이 되는 부분만 따로 설명한 것입니다.

## 조치 1 — 허브 배포 잡이 `map/`을 지우지 않게 (필수)

**가장 빠른 방법**: 허브 잡의 `cleanRemote: true` → `false`. 이것만으로 지도는 더 이상 지워지지 않습니다. 다만 최종 위치를 정리하지 않으므로 산출물 이름이 바뀌면 옛 파일이 남습니다(현재 webpack 산출물은 `index.html` + `bundle.js` 고정이라 영향은 작습니다).

**권장 방법**: 전송은 허브 전용 **스테이징 경로**로 받고(거기서는 `cleanRemote`를 켜도 안전), 최종 반영은 `execCommand`에서 `map/`을 제외한 채 동기화합니다. 아래에서 `$SRC`는 스테이징 경로, `$DST`는 `/home/kuber-volume/sj-lab-webserver/html` 입니다. 스테이징 경로는 지도 잡의 `/respal/deploy`와 **달라야** 합니다(동시 실행 시 충돌).

**rsync 가 있는 경우(권장)**

```sh
set -eu
SRC=/respal/deploy-hub                              # 허브 잡이 올린 경로
DST=/home/kuber-volume/sj-lab-webserver/html

[ -f "$SRC/index.html" ] || { echo "index.html 없음 - 배포 중단"; exit 1; }

# --delete 로 옛 파일은 정리하되, 지도 폴더와 인증서 challenge 는 건드리지 않는다
rsync -a --delete --exclude 'map/' --exclude '.well-known/' "$SRC"/ "$DST"/
echo "허브 배포 완료"
```

**rsync 가 없는 경우**

```sh
set -eu
SRC=/respal/deploy-hub
DST=/home/kuber-volume/sj-lab-webserver/html

[ -f "$SRC/index.html" ] || { echo "index.html 없음 - 배포 중단"; exit 1; }

# map 과 .well-known 만 남기고 지운다 (rm -rf $DST/* 를 쓰지 말 것)
find "$DST" -mindepth 1 -maxdepth 1 ! -name 'map' ! -name '.well-known' -exec rm -rf {} +
cp -r "$SRC"/. "$DST"/
echo "허브 배포 완료"
```

**금지 패턴 두 가지**

- `cleanRemote: true` + `remoteDirectory`가 **최종 웹 디렉터리(`.../html`)** — 하위 `map/`이 통째로 삭제됩니다(이번 원인). 스테이징 경로에만 쓰세요.
- `rm -rf $DST/*` — 마찬가지로 하위 `map/`까지 지우고, 변수가 비면 `rm -rf /*`가 되는 위험도 있습니다.

## 조치 2 — 지도 배포 잡을 원자적 교체로 (권장)

현재 지도 잡은 `rm -rf $DIR_FRONT/*` → `cp -r` 순서라, **복사가 끝날 때까지 지도가 비어 있고**(그 사이 접속하면 허브 화면), 복사가 중간에 실패하면 반쪽만 남습니다. 새 폴더에 먼저 올린 뒤 바꿔치기하면 이 구간이 사라집니다.

```sh
set -eu
SRC=/respal/deploy                                   # 지도 잡이 올린 경로 (현행 유지)
DST=/home/kuber-volume/sj-lab-webserver/html/map

[ -d "$SRC" ] || { echo "소스 없음: $SRC"; exit 1; }
[ -f "$SRC/index.html" ] || { echo "index.html 없음 - 배포 중단"; exit 1; }   # 빈 디렉터리로 덮어쓰는 사고 방지

NEW="${DST}.new.$$"
OLD="${DST}.old.$$"
rm -rf "$NEW"; mkdir -p "$NEW"

# 저장소 문서·에이전트 설정은 웹에 올리지 않는다 (아래 "부가 발견" 참고)
tar -C "$SRC" \
    --exclude='./.git' --exclude='./.claude' --exclude='./.agents' --exclude='./docs' \
    --exclude='./CLAUDE.md' --exclude='./AGENTS.md' --exclude='./README.md' \
    -cf - . | tar -C "$NEW" -xf -

[ -d "$DST" ] && mv "$DST" "$OLD" || true
mv "$NEW" "$DST"
rm -rf "$OLD"
echo "지도 배포 완료: $DST"
```

## 조치 3 — 배포 후 자동 확인

잡 마지막에 한 줄 넣어 두면, 다른 잡이 지웠을 때 그 잡이 **빨간색으로 실패**해 바로 드러납니다.

```sh
curl -fsS https://sj-lab.co.kr/map/ | grep -q 'js/auth-gate.js' \
  && echo "지도 정상" || { echo "지도가 허브 폴백으로 응답 - 파일 확인 필요"; exit 1; }
```

로컬에서는 이 저장소의 스크립트로 두 사이트를 한 번에 확인합니다.

```
powershell -ExecutionPolicy Bypass -File scripts\check-prod-sites.ps1
```

허브 표식(webpack `bundle`)과 지도 표식(`js/auth-gate.js`, `openlayers/ol.js`)으로 판별하며, **지도 경로에서 허브 표식이 나오면 "지워졌을 수 있음"으로 실패** 처리합니다.

## 지워졌을 때 복구

해당 사이트의 Jenkins 잡을 다시 실행하면 파일이 다시 복사되어 복구됩니다(빌드 산출물이 아니라 저장소 파일을 그대로 올리는 구조라 재실행만으로 충분). 복구 후 위 확인 명령으로 검증합니다.

## 근본 분리(선택) — 디렉터리를 아예 나누기

배포 잡을 고치는 것으로 충분하지만, 실수 여지를 구조적으로 없애려면 지도를 허브 하위가 아닌 **별도 디렉터리**로 옮기고 nginx가 `alias`로 서빙하게 합니다.

```nginx
location = /map { return 301 /map/; }

location /map/ {
    alias /home/kuber-volume/sj-lab-webserver/map/;
    index index.html;
    try_files $uri $uri/ /map/index.html;   # 지도 안에서만 폴백 (허브로 새지 않음)
}
```

적용 순서(순서를 지키지 않으면 지도가 잠시 내려갑니다):
1. 노드에서 `mkdir -p /home/kuber-volume/sj-lab-webserver/map && cp -a /home/kuber-volume/sj-lab-webserver/html/map/. /home/kuber-volume/sj-lab-webserver/map/`
2. nginx 설정 반영 → `/map/` 200 확인
3. 지도 Jenkins 잡의 `DST`를 새 경로로 변경
4. 옛 `html/map`은 며칠 지켜본 뒤 제거

**주의**: 운영 응답 헤더가 `Server: nginx/1.24.0 (Ubuntu)`인데 차트(`sj-lab-webserver`)의 이미지는 `nginx:latest`입니다. 즉 **공개 트래픽을 처리하는 nginx는 차트의 파드가 아니라 노드에 설치된 nginx일 가능성이 큽니다**(TLS 종료도 차트 설정에는 없음 — `listen 80`만 있음). 위 nginx 설정을 적용할 때는 **실제로 서빙 중인 쪽**(노드의 `/etc/nginx/...` 또는 차트의 `files/nginx-set.conf`)을 `nginx -T`로 먼저 확인하고 고쳐야 합니다. 차트만 고치면 반영되지 않을 수 있습니다.

## 부가 발견 — 저장소 문서가 웹에 공개됨

지도 잡이 저장소를 통째로(`sourceFiles: '**/*'`) 올려서 아래가 공개 URL로 열립니다.

```
https://sj-lab.co.kr/map/CLAUDE.md        200
https://sj-lab.co.kr/map/docs/*.md        200
https://sj-lab.co.kr/map/README.md        200
https://sj-lab.co.kr/map/AGENTS.md        200
```

`.git/`·`.claude/`는 점(.)으로 시작해 복사되지 않아 노출되지 않습니다(확인함). 저장소 자체가 public이라 비밀 유출은 아니지만, 내부 규칙 문서가 사이트에 붙어 있을 이유가 없으므로 **조치 2의 `--exclude`로 함께 정리**합니다.
