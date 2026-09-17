# MCP 연결 (`.mcp.json`)

Claude Code가 이 저장소 루트의 `.mcp.json`으로 sj-lab 공용 MCP 서버를 불러옵니다. 처음 사용할 때 서버별로 승인 창이 뜨고, 연결 상태는 Claude Code에서 `/mcp`로 확인합니다. MCP 서버는 세션 시작 시 한 번 연결되므로 설정·값을 바꾸면 Claude Code를 재시작해야 반영됩니다.

- `github` — GitHub 원격 MCP(`api.githubcopilot.com/mcp/`). sj-lab 저장소들(프론트엔드·게이트웨이·k8s 등)의 코드·이슈·PR 조회용. 환경변수 `GITHUB_PERSONAL_ACCESS_TOKEN` 필요.
- `sjlabDevDb` — PostgreSQL MCP(`@modelcontextprotocol/server-postgres`, 읽기 전용 트랜잭션으로만 쿼리). 환경변수 `SJLAB_DEV_DATABASE_URL`(예: `postgresql://readonly_user:비밀번호@호스트:5432/DB명`) 필요. `node`/`npx` 필요.

두 환경변수의 실제 값은 `.claude/settings.local.json`의 `env`에 넣습니다(이 저장소 전용·로컬 전용, `.gitignore`로 커밋 제외).

이 파일은 MCP 전용이 아니라 **이 저장소의 로컬 비밀값 보관처**입니다. `scripts/local-stack.ps1`도 백엔드를 띄울 때 여기서 QFieldCloud 계정(`QFIELD_*`)을 읽어 넣습니다(자세한 내용은 `docs/dev-environment.md`).

```json
{
  "enabledMcpjsonServers": ["github", "sjlabDevDb"],
  "env": {
    "GITHUB_PERSONAL_ACCESS_TOKEN": "github_pat_...",
    "SJLAB_DEV_DATABASE_URL": "postgresql://readonly_user:비밀번호@호스트:5432/DB명",
    "QFIELD_USERNAME": "QFieldCloud 계정",
    "QFIELD_PASSWORD": "비밀번호",
    "QFIELD_BASE_URL": "https://qfield.sj-lab.co.kr"
  }
}
```

MCP 설정은 에이전트마다 따로입니다 — Antigravity 등 다른 에이전트는 이 `.mcp.json`을 읽지 않으므로, DB 작업을 맡길 때는 `settings.local.json`의 접속 문자열로 직접 접속(읽기 전용)하게 합니다.

## 반드시 지킬 것

- 토큰·DB 접속 문자열을 `.mcp.json`이나 문서에 직접 적지 말 것. `${변수:-}` 형태로 환경변수에서만 읽음 — `.mcp.json`은 git에 커밋되는 파일이고 이 저장소는 public임.
- `.gitignore`의 `.claude/settings.local.json` 항목을 지우지 말 것. 비밀번호에 `@`·`:`·`/`·`#` 등이 있으면 URL 인코딩(`@` → `%40`)할 것.
- `SJLAB_DEV_DATABASE_URL`에는 **개발/복제 DB의 읽기 전용 계정**(현재 `mcp_readonly`)만 넣을 것. 쓰기 권한 계정이나 `application.yml`의 datasource 계정을 연결하지 말 것.
- GitHub 토큰은 필요한 저장소만 선택한 fine-grained 토큰(Contents/Issues/Pull requests 읽기 위주)으로 발급할 것.
- `sjlabDevDb`의 `cmd /c npx` 래퍼는 네이티브 Windows용입니다. macOS/Linux/WSL에서는 `command`를 `npx`로 바꾸고 `args`의 `/c`, `npx`를 빼야 함.
- 검증용 스크립트에서 접속 문자열을 파싱할 때 오류 메시지에 원문이 섞여 출력되지 않게 할 것(예외 타입만 출력).
