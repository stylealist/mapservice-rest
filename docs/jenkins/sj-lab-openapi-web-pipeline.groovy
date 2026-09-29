// sj-lab-openapi-web (API 활용 페이지) 배포 파이프라인 — Jenkins 잡의 [Pipeline script] 에 붙여 넣는 내용
//
// 지도(sj-lab-mapservice)·허브(sj-lab-hub) 잡과 같은 방식이다 — 이미지·ArgoCD 를 거치지 않고
// 웹서버 노드의 html/openapi 폴더에 정적 파일을 복사한다.
//
// 이 저장소의 다른 잡과 맞춰 둔 것(틀리면 잡이 시작조차 못 한다)
//   tools     : nodejs 'node-18'        ← Jenkins 에 등록된 도구 이름 그대로
//   SSH 서버   : sj-lab-master           ← Jenkins 전역 SSH 서버 설정 이름
//   스테이징   : /respal/deploy-openapi  ← 지도(/respal/deploy)·허브(/respal/deploy-hub)와 달라야 한다
//               같은 경로를 쓰면 두 잡이 동시에 돌 때 서로의 파일을 덮어쓴다.
//
// 주의: cleanRemote 는 **스테이징에만** 켠다. 최종 위치(html)에 켜면 옆 사이트(map/openapi)가 지워진다.

pipeline {
  agent any

  environment {
    BUILD_DIR   = "build"
    STAGING_DIR = "/respal/deploy-openapi"                             // 이 잡 전용 스테이징
    WEB_DIR     = "/home/kuber-volume/sj-lab-webserver/html/openapi"   // 최종 위치
    SSH_SERVER  = "sj-lab-master"
  }

  tools {
    // Jenkins 에 등록된 NodeJS 도구 이름 그대로 써야 한다(허브 잡도 같은 이름).
    // 이름이 틀리면 "Tool type nodejs does not have an install of ..." 로 시작 단계에서 실패한다.
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
                    remoteDirectory: "${env.STAGING_DIR}",   // 최종 위치가 아니라 스테이징으로
                    cleanRemote: true,                        // 스테이징만 비운다 (여기는 비워도 안전)
                    flatten: false,

                    execCommand: '''
                        set -eu
                        SRC=/respal/deploy-openapi
                        DST=/home/kuber-volume/sj-lab-webserver/html/openapi

                        [ -f "$SRC/index.html" ] || { echo "❌ index.html 없음 - 배포 중단"; exit 1; }

                        # 새 폴더에 먼저 펼친 뒤 바꿔치기한다 — 복사하는 동안 사이트가 비어 보이지 않게
                        NEW="${DST}.new.$$"
                        OLD="${DST}.old.$$"
                        rm -rf "$NEW"; mkdir -p "$NEW"
                        tar -C "$SRC" -cf - . | tar -C "$NEW" -xf -

                        [ -d "$DST" ] && mv "$DST" "$OLD" || true
                        mv "$NEW" "$DST"
                        rm -rf "$OLD"

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
        // 파일이 없으면 nginx 의 /openapi/ 설정이 404 를 돌려준다(허브 폴백 방지).
        // 혹시 설정이 빠져 허브 화면이 200 으로 오더라도 본문 표식으로 걸러낸다.
        sh '''
            set -eu
            curl -fsS https://sj-lab.co.kr/openapi/ | grep -q 'SJ-LAB OpenAPI' \
                || { echo "❌ /openapi/ 가 활용 페이지가 아니다 - html/openapi 확인 필요"; exit 1; }
            echo "✅ API 활용 페이지 정상"
        '''
      }
    }
  }

  post {
    success {
      echo '✅ API 활용 페이지가 웹서버에 배포되었습니다!'
    }
    failure {
      echo '❌ 빌드 또는 배포 실패!'
    }
  }
}
