<#
.SYNOPSIS
  운영 정적 사이트(허브 / 지도)가 각각 제 파일로 서빙되고 있는지 확인한다.

.DESCRIPTION
  허브(sj-lab-hub)와 지도(sj-lab-mapservice)는 웹서버의 같은 디렉터리를 공유한다.
      /home/kuber-volume/sj-lab-webserver/html        ← 허브
      /home/kuber-volume/sj-lab-webserver/html/map    ← 지도
  그래서 한쪽 배포 잡이 상위 디렉터리를 비우면 다른 쪽이 통째로 지워진다.
  게다가 nginx 가 `try_files $uri $uri/ /index.html` 로 SPA 폴백을 하므로,
  지도가 지워져도 /map/ 은 404 가 아니라 **허브 첫 화면을 200 으로** 돌려준다.
  이 스크립트는 응답 본문의 표식으로 그 상태를 구분한다.

.EXAMPLE
  powershell -ExecutionPolicy Bypass -File scripts\check-prod-sites.ps1
  powershell -ExecutionPolicy Bypass -File scripts\check-prod-sites.ps1 -BaseUrl http://localhost:4000 -MapPath /
#>
[CmdletBinding()]
param(
  [string]$BaseUrl = "https://sj-lab.co.kr",
  [string]$HubPath = "/",
  [string]$MapPath = "/map/",
  [int]$TimeoutSec = 20
)

$ErrorActionPreference = "Stop"

# 각 사이트를 가려내는 표식 (index.html 에 반드시 들어 있는 문자열)
$hubMarkers = @("bundle")                                  # webpack 번들 스크립트
$mapMarkers = @("js/auth-gate.js", "openlayers/ol.js")     # 로그인 게이트 + OpenLayers

$failCount = 0

function Get-Page {
  param([string]$Url)
  try {
    $response = Invoke-WebRequest -UseBasicParsing -Uri $Url -TimeoutSec $TimeoutSec
    return @{ ok = $true; status = [int]$response.StatusCode; body = [string]$response.Content }
  } catch {
    $status = 0
    if ($_.Exception.Response) { $status = [int]$_.Exception.Response.StatusCode.value__ }
    return @{ ok = $false; status = $status; body = ""; error = $_.Exception.Message }
  }
}

function Test-Site {
  param(
    [string]$Label,
    [string]$Url,
    [string[]]$Expected,
    [string[]]$NotExpected = @()
  )

  $page = Get-Page -Url $Url
  if (-not $page.ok) {
    Write-Host ("  [실패] {0,-6} {1} → 응답 없음/오류 (status={2})" -f $Label, $Url, $page.status) -ForegroundColor Red
    return $false
  }

  $missing = @($Expected | Where-Object { $page.body -notlike "*$_*" })
  $wrongSite = @($NotExpected | Where-Object { $page.body -like "*$_*" })

  if ($missing.Count -eq 0 -and $wrongSite.Count -eq 0) {
    Write-Host ("  [정상] {0,-6} {1} → {2} ({3:N0} bytes)" -f $Label, $Url, $page.status, $page.body.Length) -ForegroundColor Green
    return $true
  }

  Write-Host ("  [실패] {0,-6} {1} → {2} ({3:N0} bytes)" -f $Label, $Url, $page.status, $page.body.Length) -ForegroundColor Red
  if ($missing.Count -gt 0) {
    Write-Host ("         표식 없음: {0}" -f ($missing -join ", ")) -ForegroundColor Red
  }
  if ($wrongSite.Count -gt 0) {
    # 지도 경로에서 허브 표식이 나오면 = 지도 파일이 지워져 SPA 폴백이 허브를 돌려준 것
    Write-Host ("         다른 사이트의 표식이 나옴: {0} — 파일이 지워졌을 수 있음" -f ($wrongSite -join ", ")) -ForegroundColor Red
  }
  return $false
}

Write-Host ""
Write-Host ("정적 사이트 점검: {0}" -f $BaseUrl)

if (-not (Test-Site -Label "허브" -Url ($BaseUrl.TrimEnd("/") + $HubPath) -Expected $hubMarkers)) { $failCount++ }
if (-not (Test-Site -Label "지도" -Url ($BaseUrl.TrimEnd("/") + $MapPath) -Expected $mapMarkers -NotExpected $hubMarkers)) { $failCount++ }

Write-Host ""
if ($failCount -eq 0) {
  Write-Host "두 사이트 모두 제 파일로 서빙 중입니다." -ForegroundColor Green
  exit 0
}

Write-Host ("실패 {0}건 — 지도가 지워졌다면 해당 Jenkins 잡을 다시 실행하면 복구됩니다." -f $failCount) -ForegroundColor Yellow
Write-Host "재발 방지는 docs/deploy-static-sites.md 참고 (배포 잡이 상위 디렉터리를 비우지 않게 할 것)." -ForegroundColor Yellow
exit 1
