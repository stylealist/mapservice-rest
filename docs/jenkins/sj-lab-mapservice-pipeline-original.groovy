// [백업] sj-lab-mapservice 배포 잡의 **수정 전** 원본 (2026-09-23 사용자 제공).
// 롤백용으로만 보관합니다.
//
// 이 잡 자체는 허브를 지우지 않습니다(대상이 html/map 과 스테이징 /respal/deploy 뿐).
// 다만 아래 두 가지 때문에 개선본을 만들었습니다.
//   1) `rm -rf $DIR_FRONT/*` 후 `cp -r` → 복사하는 동안 지도가 비어 있고(그때 접속하면 nginx 폴백으로
//      허브 화면이 뜸), 중간에 실패하면 반쪽만 남는다.
//   2) sourceFiles: '**/*' 로 저장소를 통째 전송 → /map/CLAUDE.md, /map/docs/*.md 등이 웹에 공개됨.
//
// 고친 버전: sj-lab-mapservice-pipeline.groovy   (원인·조치 설명: docs/deploy-static-sites.md)

pipeline {
    agent any

    environment {
        SSH_SERVER = 'sj-lab-master'
        BUILD_DIR = '.'
        REMOTE_DIR = '/respal/deploy'

        // 복사 후 실행할 디렉토리 설정
        DIR_JENKINS = '/respal/deploy'
        DIR_FRONT = '/home/kuber-volume/sj-lab-webserver/html/map'
    }

    triggers {
        githubPush()
    }

    stages {
        stage('Checkout') {
            steps {
                git(
                    branch: 'main',
                    credentialsId: 'GitHubAccount', // 이거 없다고 하니 Webhook만 쓰고 인증은 생략된 상태인 듯함
                    url: 'https://github.com/stylealist/sj-lab-mapservice.git'
                )
                sh 'ls -al' // 실제 어떤 파일들이 있는지 로그로 출력
            }
        }

        stage('Deploy Static Files to Kubernetes Node') {
            steps {
                sshPublisher(
                    publishers: [
                        sshPublisherDesc(
                            configName: "${SSH_SERVER}",
                            transfers: [
                                sshTransfer(
                                    sourceFiles: '**/*',
                                    removePrefix: '',
                                    remoteDirectory: "${REMOTE_DIR}",
                                    cleanRemote: true,
                                    flatten: false,

                                    // 여기 안에 쉘 명령 추가!
                                    execCommand: '''
                                        echo "[SSH] 복사 후 후처리 시작"
                                        DIR_JENKINS=/respal/deploy
                                        DIR_FRONT=/home/kuber-volume/sj-lab-webserver/html/map

                                        if [ ! -d "$DIR_FRONT" ]; then
                                            echo "📁 대상 디렉토리 없음. 생성 중: $DIR_FRONT"
                                            mkdir -p "$DIR_FRONT"
                                        fi

                                        if [ -d "$DIR_JENKINS" ]; then
                                            rm -rf $DIR_FRONT/*
                                            cp -r $DIR_JENKINS/* $DIR_FRONT/
                                            echo "✅ 정적 웹 파일 복사 완료"
                                        else
                                            echo "❌ 디렉터리 없음: $DIR_JENKINS"
                                            exit 1
                                        fi
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
}
