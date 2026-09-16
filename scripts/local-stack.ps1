<#
  sj-lab 로컬 스택을 IntelliJ 없이 띄우고 끄는 스크립트 (Windows PowerShell 5.1)

  사용법 (mapservice-rest 루트에서):
    powershell -ExecutionPolicy Bypass -File scripts\local-stack.ps1 start            # 빌드 후 전체 기동
    powershell -ExecutionPolicy Bypass -File scripts\local-stack.ps1 start -NoBuild   # 기존 jar로 기동
    powershell -ExecutionPolicy Bypass -File scripts\local-stack.ps1 status
    powershell -ExecutionPolicy Bypass -File scripts\local-stack.ps1 stop

  기동 순서: Eureka(8761) → mapservice-rest(랜덤 포트) → API Gateway(8100) → 프론트 정적 서버(4000)
  - 이 스크립트가 띄운 프로세스만 .local-stack\pids.json 에 기록하고, stop 은 그 프로세스만 종료한다.
  - 포트가 이미 사용 중이면(예: IntelliJ로 실행 중) 그 구성요소는 건너뛴다.
  - sj-lab-discoveryServer 는 target/ 이 git에 추적되므로 원본이 아닌 .local-stack\build 복사본에서 빌드한다.
  - sj-lab-scheduler 는 기동 시 cron 배치가 실제 DB에 적재하므로 이 스크립트에 넣지 않는다.
#>
param(
  [Parameter(Position = 0)][ValidateSet('start', 'stop', 'status')][string]$action = 'status',
  [switch]$NoBuild,
  [string]$workspaceRoot = 'C:\developer\workspace',
  [string]$frontendRoot = 'C:\vscode_develop\sj-lab-mapservice'
)

$ErrorActionPreference = 'Stop'
$hubRoot = Split-Path -Parent $PSScriptRoot
$stateDir = Join-Path $hubRoot '.local-stack'
$pidFile = Join-Path $stateDir 'pids.json'
New-Item -ItemType Directory -Force $stateDir | Out-Null

function findJdk17 {
  $candidates = @($env:JDK17_HOME, 'C:\Program Files\Java\jdk-17') + @(Get-ChildItem "$env:USERPROFILE\.jdks" -Directory -Filter '*17*' -ErrorAction SilentlyContinue | ForEach-Object FullName)
  foreach ($c in $candidates) { if ($c -and (Test-Path (Join-Path $c 'bin\java.exe'))) { return $c } }
  throw 'JDK 17을 찾지 못했습니다. JDK17_HOME 환경변수를 지정하세요.'
}

function testPort([int]$port) {
  [bool](Get-NetTCPConnection -State Listen -LocalPort $port -ErrorAction SilentlyContinue)
}

function waitUntil([scriptblock]$condition, [int]$timeoutSec, [string]$label) {
  $deadline = (Get-Date).AddSeconds($timeoutSec)
  while ((Get-Date) -lt $deadline) {
    if (& $condition) { return }
    Start-Sleep -Seconds 2
  }
  throw "$label 대기 시간($timeoutSec 초) 초과. .local-stack 로그를 확인하세요."
}

function readPids {
  if (Test-Path $pidFile) { return (Get-Content $pidFile -Raw | ConvertFrom-Json) }
  return $null
}

function savePid([string]$name, [int]$processId) {
  $pids = @{}
  $existing = readPids
  if ($existing) { $existing.PSObject.Properties | ForEach-Object { $pids[$_.Name] = $_.Value } }
  $pids[$name] = $processId
  $pids | ConvertTo-Json | Set-Content $pidFile -Encoding ascii
}

function invokeMavenPackage([string]$projectDir, [string]$jdkHome) {
  Write-Host "  빌드: $projectDir"
  $env:JAVA_HOME = $jdkHome
  Push-Location $projectDir
  $previousPreference = $ErrorActionPreference
  $ErrorActionPreference = 'Continue'   # 5.1에서 네이티브 stderr 출력이 종료 오류로 바뀌지 않게
  try {
    & .\mvnw.cmd -q clean package -DskipTests
    if ($LASTEXITCODE -ne 0) { throw "빌드 실패: $projectDir" }
  } finally {
    $ErrorActionPreference = $previousPreference
    Pop-Location
  }
}

function startJava([string]$name, [string]$jar, [string]$jdkHome, [string[]]$extraArgs) {
  if (-not (Test-Path $jar)) { throw "jar 없음: $jar (-NoBuild 없이 다시 실행하세요)" }
  $log = Join-Path $stateDir "$name.log"
  $argList = @('-jar', $jar, '--spring.profiles.active=local') + $extraArgs
  $proc = Start-Process -FilePath (Join-Path $jdkHome 'bin\java.exe') -ArgumentList $argList -WorkingDirectory $stateDir `
    -RedirectStandardOutput $log -RedirectStandardError (Join-Path $stateDir "$name.err.log") -WindowStyle Hidden -PassThru
  savePid $name $proc.Id
  Write-Host "  기동: $name (pid $($proc.Id), 로그 .local-stack\$name.log)"
  return $proc
}

function startStack {
  $jdkHome = findJdk17
  $gatewayDir = Join-Path $workspaceRoot 'sj-lab-apigateway'
  $discoverySrc = Join-Path $workspaceRoot 'sj-lab-discoveryServer'
  $discoveryBuild = Join-Path $stateDir 'build\sj-lab-discoveryServer'

  $needEureka = -not (testPort 8761)
  $needGateway = -not (testPort 8100)
  $needFrontend = -not (testPort 4000)
  $running = readPids
  $backendAlive = $running -and $running.'mapservice-rest' -and (Get-Process -Id $running.'mapservice-rest' -ErrorAction SilentlyContinue)
  $needBackend = -not $backendAlive

  if (-not $NoBuild) {
    Write-Host '[1/2] 빌드 (JDK 17)'
    if ($needEureka) {
      if (Test-Path $discoveryBuild) { Remove-Item -Recurse -Force $discoveryBuild }
      robocopy $discoverySrc $discoveryBuild /E /XD target .git .idea .claude /NFL /NDL /NJH /NJS /NP | Out-Null
      invokeMavenPackage $discoveryBuild $jdkHome
    }
    if ($needBackend) { invokeMavenPackage $hubRoot $jdkHome }
    if ($needGateway) { invokeMavenPackage $gatewayDir $jdkHome }
  }

  Write-Host '[2/2] 기동'
  if ($needEureka) {
    startJava 'eureka' (Join-Path $discoveryBuild 'target\sj-lab-discoveryservice.jar') $jdkHome @() | Out-Null
    waitUntil { testPort 8761 } 120 'Eureka(8761)'
  } else { Write-Host '  건너뜀: 8761 이미 사용 중' }

  if ($needBackend) {
    startJava 'mapservice-rest' (Join-Path $hubRoot 'target\sj-lab-mapservice-rest.jar') $jdkHome @() | Out-Null
    waitUntil { Select-String -Path (Join-Path $stateDir 'mapservice-rest.log') -Pattern 'Started MapServiceRestApplication' -Quiet } 180 'mapservice-rest'
  } else { Write-Host '  건너뜀: mapservice-rest 이미 실행 중' }

  if ($needGateway) {
    startJava 'apigateway' (Join-Path $gatewayDir 'target\sj-lab-apigateway.jar') $jdkHome @() | Out-Null
    waitUntil { testPort 8100 } 180 'API Gateway(8100)'
  } else { Write-Host '  건너뜀: 8100 이미 사용 중' }

  if ($needFrontend) {
    $python = (Get-Command python -ErrorAction Stop).Source
    $proc = Start-Process -FilePath $python -ArgumentList '-m', 'http.server', '4000', '--bind', '127.0.0.1' -WorkingDirectory $frontendRoot `
      -RedirectStandardOutput (Join-Path $stateDir 'frontend.log') -RedirectStandardError (Join-Path $stateDir 'frontend.err.log') -WindowStyle Hidden -PassThru
    savePid 'frontend' $proc.Id
    Write-Host "  기동: frontend (pid $($proc.Id))"
  } else { Write-Host '  건너뜀: 4000 이미 사용 중' }

  Write-Host '게이트웨이 라우팅 대기 (Eureka 레지스트리 갱신)...'
  try {
    waitUntil {
      try { (Invoke-WebRequest -UseBasicParsing -Uri 'http://localhost:8100/map/admin-area/sido' -TimeoutSec 10).StatusCode -eq 200 } catch { $false }
    } 120 '게이트웨이 → mapservice-rest 라우팅'
    Write-Host '준비 완료: http://localhost:4000'
  } catch { Write-Warning $_.Exception.Message }
  showStatus
}

function stopStack {
  $pids = readPids
  if (-not $pids) { Write-Host '이 스크립트가 띄운 프로세스가 없습니다.'; return }
  foreach ($p in $pids.PSObject.Properties) {
    $proc = Get-Process -Id $p.Value -ErrorAction SilentlyContinue
    if ($proc -and $proc.ProcessName -in @('java', 'python')) {
      Stop-Process -Id $p.Value -Force -Confirm:$false
      Write-Host "  종료: $($p.Name) (pid $($p.Value))"
    } else { Write-Host "  이미 종료됨: $($p.Name)" }
  }
  Remove-Item $pidFile -Force
}

function showStatus {
  $pids = readPids
  foreach ($name in 'eureka', 'mapservice-rest', 'apigateway', 'frontend') {
    $processId = if ($pids) { $pids.$name } else { $null }
    $alive = $processId -and (Get-Process -Id $processId -ErrorAction SilentlyContinue)
    $state = if ($alive) { "실행 중 (pid $processId)" } elseif ($processId) { '종료됨' } else { '이 스크립트로 띄우지 않음' }
    Write-Host ("  {0,-16} {1}" -f $name, $state)
  }
  Write-Host ("  포트 리슨: 8761={0} 8100={1} 4000={2}" -f (testPort 8761), (testPort 8100), (testPort 4000))
  try {
    $apps = Invoke-RestMethod -Uri 'http://localhost:8761/eureka/apps' -Headers @{ Accept = 'application/json' } -TimeoutSec 5
    $names = @($apps.applications.application) | ForEach-Object { "$($_.name)($(@($_.instance).Count))" }
    Write-Host "  Eureka 등록: $($names -join ', ')"
  } catch { Write-Host '  Eureka 조회 불가' }
}

switch ($action) {
  'start' { startStack }
  'stop' { stopStack }
  'status' { showStatus }
}
