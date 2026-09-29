// sj-lab-openapi-web (API 활용 페이지) 배포 파이프라인 — Jenkins 잡의 [Pipeline script] 에 붙여 넣는 내용
// 허브(sj-lab-hub)·지도(sj-lab-mapservice) 잡과 같은 구조다(도구·빌드 명령·post 블록 동일).
//
// ── 무엇을 배포하나 ─────────────────────────────────────────────────────────
// 공개 API 활용 페이지(React + Webpack)는 이미지·ArgoCD 를 거치지 않고,
// 웹서버 노드의 정적 디렉터리에 파일을 그대로 복사한다.
//
//     /home/kuber-volume/sj-lab-webserver/html/          ← 허브(sj-lab-hub)
//     /home/kuber-volume/sj-lab-webserver/html/map/      ← 지도(sj-lab-mapservice)
//     /home/kuber-volume/sj-lab-webserver/html/openapi/  ← 이 잡(sj-lab-openapi-web)
//
// ── 주의할 점 ───────────────────────────────────────────────────────────────
// 1) STAGING_DIR 는 지도(/respal/deploy)·허브(/respal/deploy-hub)와 **달라야** 한다.
//    같으면 두 잡이 동시에 돌 때 서로의 파일을 덮어쓴다.
// 2) cleanRemote 는 **스테이징에만** 켠다. 최종 위치(html)에 켜면 옆 사이트가 통째로 지워진다
//    (허브 잡에서 실제로 겪은 사고 — docs/deploy-static-sites.md 참고).
// 3) 이 잡은 자기 폴더(html/openapi)만 바꾸므로 허브·지도에는 영향이 없다.
//
// 전제: SSH 서버(sj-lab-master)의 Jenkins 전역 "Remote Directory" 가 '/' 이다.
//       (허브·지도 잡이 절대경로를 그대로 쓰고 있으므로 그렇게 보인다. 다르면 STAGING_DIR 와
//        execCommand 의 SRC 를 같은 기준으로 맞출 것.)
// 도구 이름: Jenkins 에 등록된 NodeJS 도구는 'node-18' 이다. 'node18' 로 적으면 잡이 시작도
//       못 하고 "Tool type nodejs does not have an install of ..." 로 실패한다.

pipeline {
  agent any

  environment {
    BUILD_DIR   = "build"
    STAGING_DIR = "/respal/deploy-openapi"                             // 이 잡 전용 스테이징 (신규)
    WEB_DIR     = "/home/kuber-volume/sj-lab-webserver/html/openapi"   // 최종 위치
    SSH_SERVER  = "sj-lab-master"
  }

  tools {
    nodejs 'node-18'
  }

  stages {
    stage('Clone Repository') {
      steps {
        git branch: 'main', url: 'https://github.com/stylealist/sj-lab-openapi-web.git'
      }
    }

    stage('Install Dependencies') {
      steps {
        sh 'npm install'
      }
    }

    stage('Build React App') {
      steps {
        sh 'npm run build'
        // 빈 산출물로 사이트를 덮어쓰는 사고 방지
        sh 'test -f build/index.html'
      }
    }

    stage('Deploy to Kubernetes Node') {
      steps {
        script {
          sshPublisher(
            publishers: [
              sshPublisherDesc(
                configName: "${env.SSH_SERVER}",
                transfers: [
                  sshTransfer(
                    sourceFiles: "${env.BUILD_DIR}/**",
                    removePrefix: "${env.BUILD_DIR}",
                    remoteDirectory: "${env.STAGING_DIR}",   // ← 최종 위치가 아니라 스테이징으로
                    cleanRemote: true,                        // ← 스테이징만 비운다 (여기는 비워도 안전)
                    flatten: false,

                    execCommand: '''
                        set -eu
                        SRC=/respal/deploy-openapi
                        DST=/home/kuber-volume/sj-lab-webserver/html/openapi

                        [ -f "$SRC/index.html" ] || { echo "❌ index.html 없음 - 배포 중단"; exit 1; }
                        mkdir -p "$DST"

                        if command -v rsync >/dev/null 2>&1; then
                            # 이 잡의 폴더만 정리한다(상위 html 은 건드리지 않는다)
                            rsync -a --delete "$SRC"/ "$DST"/
                        else
                            # rsync 가 없으면: 새 폴더에 펼친 뒤 바꿔치기(복사 중 빈 화면 방지)
                            NEW="${DST}.new.$$"
                            OLD="${DST}.old.$$"
                            rm -rf "$NEW"; mkdir -p "$NEW"
                            tar -C "$SRC" -cf - . | tar -C "$NEW" -xf -
                            mv "$DST" "$OLD"
                            mv "$NEW" "$DST"
                            rm -rf "$OLD"
                        fi

                        echo "✅ API 활용 페이지 배포 완료: $DST"
                        ls -al "$DST"
                    '''
                  )
                ],
                verbose: true
              )
            ]
          )
        }
      }
    }

    stage('Verify') {
      steps {
        // 파일이 없으면 nginx 의 location /openapi/ 가 404 를 돌려준다(허브 폴백 방지).
        // 그 설정이 아직 반영되지 않아 허브 화면이 200 으로 오더라도 본문 표식으로 걸러낸다.
        sh '''
            set -eu
            curl -fsS https://sj-lab.co.kr/openapi/ | grep -q 'SJ-LAB OpenAPI' \
                || { echo "❌ /openapi/ 가 활용 페이지가 아니다 - html/openapi 확인 필요"; exit 1; }
            curl -fsS https://sj-lab.co.kr/ | grep -q 'bundle' \
                || { echo "❌ 허브 확인 실패"; exit 1; }
            echo "✅ 활용 페이지·허브 모두 정상"
        '''
      }
    }
  }

  post {
    success {
      echo '✅ React 앱이 Kubernetes 웹서버에 배포되었습니다!'
    }
    failure {
      echo '❌ 빌드 또는 배포 실패!'
    }
  }
}

// ── 처음 배포할 때 ───────────────────────────────────────────────────────────
// 1) 웹서버 노드에 스테이징 경로를 만들어 둔다: mkdir -p /respal/deploy-openapi
// 2) nginx 의 location /openapi/ 설정(sj-lab-k8s-manifests/sj-lab-webserver/files/nginx-set.conf)이
//    반영돼 있어야 한다. 아직이면 Verify 단계가 허브 화면을 잡아내 실패로 떨어진다.
// 3) 허브의 OpenAPI 카드는 이미 열려 있으므로(sj-lab-hub), 배포만 되면 바로 눌러 들어갈 수 있다.
