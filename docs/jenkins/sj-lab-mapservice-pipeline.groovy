// sj-lab-mapservice (지도) 배포 파이프라인 — Jenkins 잡의 [Pipeline script] 에 붙여 넣는 내용
//
// 기존 잡 대비 바뀐 점
//  1) `rm -rf $DIR_FRONT/*` → `cp -r` (지우고 나서 복사) 방식은 복사하는 동안 지도가 **비어 있고**,
//     그 사이 접속하면 nginx 폴백 때문에 허브 화면이 뜬다. 중간에 실패하면 반쪽만 남는다.
//     → 새 폴더에 먼저 올린 뒤 `mv` 로 바꿔치기(원자적 교체)한다.
//  2) 저장소 문서(*.md)·docs/ 가 그대로 웹에 공개되고 있었다(예: /map/CLAUDE.md 가 200).
//     → sshTransfer 의 excludes 로 제외한다.
//  3) 배포 후 확인 단계 추가 — 다른 잡이 지도를 지웠다면 이 잡이 실패로 드러난다.
//
// 배포 대상은 허브 디렉터리의 하위 폴더다: /home/kuber-volume/sj-lab-webserver/html/map
// 이 잡은 자기 폴더만 비우므로 허브를 지우지 않는다(허브 잡 쪽 설정은 sj-lab-hub-pipeline.groovy 참고).

pipeline {
    agent any

    environment {
        SSH_SERVER = 'sj-lab-master'
        REMOTE_DIR = '/respal/deploy'                                   // 지도 전용 스테이징 경로
        DIR_FRONT  = '/home/kuber-volume/sj-lab-webserver/html/map'     // 지도 배포 대상
    }

    triggers {
        githubPush()
    }

    stages {
        stage('Checkout') {
            steps {
                git(
                    branch: 'main',
                    credentialsId: 'GitHubAccount',
                    url: 'https://github.com/stylealist/sj-lab-mapservice.git'
                )
                sh 'ls -al'
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
                                    // 저장소 문서·에이전트 설정은 웹에 올리지 않는다
                                    excludes: 'docs/**, **/*.md, .claude/**, .agents/**, .git/**',
                                    removePrefix: '',
                                    remoteDirectory: "${REMOTE_DIR}",
                                    cleanRemote: true,
                                    flatten: false,

                                    execCommand: '''
                                        set -eu
                                        SRC=/respal/deploy
                                        DST=/home/kuber-volume/sj-lab-webserver/html/map

                                        [ -d "$SRC" ] || { echo "❌ 소스 없음: $SRC"; exit 1; }
                                        # 빈 디렉터리로 덮어써 지도를 날리는 사고 방지
                                        [ -f "$SRC/index.html" ] || { echo "❌ index.html 없음 - 배포 중단"; exit 1; }

                                        NEW="${DST}.new.$$"
                                        OLD="${DST}.old.$$"

                                        rm -rf "$NEW"
                                        mkdir -p "$(dirname "$DST")"
                                        cp -r "$SRC" "$NEW"

                                        # 교체는 mv 두 번으로 끝난다 (빈 화면 구간 없음)
                                        if [ -d "$DST" ]; then mv "$DST" "$OLD"; fi
                                        mv "$NEW" "$DST"
                                        rm -rf "$OLD"

                                        echo "✅ 지도 배포 완료: $DST"
                                        ls -al "$DST" | head -20
                                    '''
                                )
                            ],
                            verbose: true
                        )
                    ]
                )
            }
        }

        stage('Verify') {
            steps {
                sh '''
                    set -eu
                    curl -fsS https://sj-lab.co.kr/map/ | grep -q 'js/auth-gate.js' \
                        || { echo "❌ 지도가 허브 폴백으로 응답 - html/map 확인 필요"; exit 1; }
                    curl -fsS https://sj-lab.co.kr/ | grep -q 'bundle' \
                        || { echo "❌ 허브 확인 실패"; exit 1; }
                    echo "✅ 허브·지도 모두 정상"
                '''
            }
        }
    }
}
