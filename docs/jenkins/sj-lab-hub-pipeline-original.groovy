// [백업] sj-lab-hub 배포 잡의 **수정 전** 원본 (2026-09-23 사용자 제공).
// 롤백용으로만 보관합니다 — 이 내용을 그대로 되돌리면 허브를 배포할 때마다 지도(html/map)가 다시 지워집니다.
//
// 문제 지점: sshTransfer 가 빌드 산출물을 최종 웹 디렉터리로 직접 전송하면서 cleanRemote 를 켰다.
//   remoteDirectory: "/home/kuber-volume/sj-lab-webserver/html"   ← 최종 위치
//   cleanRemote: true                                             ← 전송 전 이 디렉터리를 통째로 비움
// → 하위 폴더인 html/map(지도 사이트 전체)이 매 허브 배포마다 삭제됨.
//
// 고친 버전: sj-lab-hub-pipeline.groovy   (원인·조치 설명: docs/deploy-static-sites.md)

pipeline {
  agent any

  environment {
    BUILD_DIR = "build"
    REMOTE_DIR = "/home/kuber-volume/sj-lab-webserver/html"
    //REMOTE_DIR = "/home/test"
    SSH_SERVER = "sj-lab-master"  // Publish Over SSH에서 설정한 이름
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
                    remoteDirectory: "${env.REMOTE_DIR}",
                    removePrefix: "${env.BUILD_DIR}",
                    cleanRemote: true,
                    flatten: false
                  )
                ],
                verbose: true
              )
            ]
          )
        }
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
