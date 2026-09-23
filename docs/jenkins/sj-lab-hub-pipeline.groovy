// sj-lab-hub (허브 첫 화면) 배포 파이프라인 — Jenkins 잡의 [Pipeline script] 에 붙여 넣는 내용
// 기존 잡을 그대로 두고 **배포 단계만** 바꾼 버전이다(도구·빌드 명령·post 블록은 원본 유지).
//
// ── 왜 바꾸나 ────────────────────────────────────────────────────────────────
// 기존 잡은 빌드 산출물을 최종 위치로 바로 전송하면서 cleanRemote 를 켰다.
//
//     remoteDirectory: "/home/kuber-volume/sj-lab-webserver/html"
//     cleanRemote: true      ← 전송 전에 이 디렉터리를 통째로 비운다
//
// 그런데 지도 사이트(sj-lab-mapservice)가 그 **하위 폴더** html/map 에 들어 있어서,
// 허브를 배포할 때마다 지도가 통째로 지워졌다(지도 잡이 다시 돌 때까지 복구 안 됨).
// nginx 가 `try_files $uri $uri/ /index.html` 로 폴백하므로 그때 /map/ 은 404 가 아니라
// **허브 첫 화면을 200 으로** 돌려줘서 원인이 잘 드러나지 않았다.
//
// ── 어떻게 바꾸나 ────────────────────────────────────────────────────────────
// 전송은 허브 전용 **스테이징 경로**로 받고(거기서는 cleanRemote 를 켜도 안전),
// 최종 반영은 execCommand 에서 `map/` 을 제외한 채 동기화한다.
//
// 전제: SSH 서버(sj-lab-master)의 Jenkins 전역 "Remote Directory" 가 '/' 이다.
//       (기존 두 잡이 절대경로를 그대로 쓰고 있으므로 그렇게 보인다. 다르면 STAGING_DIR 와
//        execCommand 의 SRC 를 같은 기준으로 맞출 것.)
// 주의: STAGING_DIR 는 지도 잡의 스테이징(/respal/deploy)과 **달라야** 한다. 같으면 두 잡이
//       동시에 돌 때 서로의 파일을 덮어쓴다.

pipeline {
  agent any

  environment {
    BUILD_DIR   = "build"
    STAGING_DIR = "/respal/deploy-hub"                        // 허브 전용 스테이징 (신규)
    WEB_DIR     = "/home/kuber-volume/sj-lab-webserver/html"  // 최종 위치 (하위 map/ 은 지도 사이트)
    SSH_SERVER  = "sj-lab-master"
  }

  tools {
    nodejs 'node-18'
  }

  stages {
    stage('Clone Repository') {
      steps {
        git branch: 'main', url: 'https://github.com/stylealist/sj-lab-hub.git'
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
                        SRC=/respal/deploy-hub
                        DST=/home/kuber-volume/sj-lab-webserver/html

                        [ -f "$SRC/index.html" ] || { echo "❌ index.html 없음 - 배포 중단"; exit 1; }
                        mkdir -p "$DST"

                        if command -v rsync >/dev/null 2>&1; then
                            # 옛 파일은 정리하되 지도(map/)와 인증서 challenge 는 건드리지 않는다
                            rsync -a --delete --exclude 'map/' --exclude '.well-known/' "$SRC"/ "$DST"/
                        else
                            # rsync 가 없으면: map 과 .well-known 만 남기고 지운다
                            find "$DST" -mindepth 1 -maxdepth 1 ! -name 'map' ! -name '.well-known' -exec rm -rf {} +
                            cp -r "$SRC"/. "$DST"/
                        fi

                        echo "✅ 허브 배포 완료: $DST"
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
        // 지도가 지워졌다면 /map/ 이 404 가 아니라 허브 화면(200)을 돌려주므로 본문 표식으로 판별한다.
        sh '''
            set -eu
            curl -fsS https://sj-lab.co.kr/ | grep -q 'bundle' \
                || { echo "❌ 허브 확인 실패"; exit 1; }
            curl -fsS https://sj-lab.co.kr/map/ | grep -q 'js/auth-gate.js' \
                || { echo "❌ 지도가 허브 폴백으로 응답 - html/map 확인 필요"; exit 1; }
            echo "✅ 허브·지도 모두 정상"
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

// ── 급할 때의 1줄 대안 ───────────────────────────────────────────────────────
// 위 구조 변경이 부담스러우면, 기존 잡에서 `cleanRemote: true` 를 `false` 로만 바꿔도
// 지도는 더 이상 지워지지 않는다. 다만 최종 위치를 정리하지 않으므로, 산출물 이름이
// 바뀌면 옛 파일이 남는다(현재 webpack 산출물은 index.html + bundle.js 고정이라 영향은 작다).
