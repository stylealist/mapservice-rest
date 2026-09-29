// sj-lab-openapi-web (API 활용 페이지) 배포 파이프라인 예시
//
// 지도(sj-lab-mapservice)와 같은 방식이다 — 이미지·ArgoCD 를 거치지 않고, 웹서버 노드의
// html/openapi 폴더에 정적 파일을 복사한다. 허브 배포가 상위 디렉터리를 비우면 이 폴더도
// 함께 지워지므로, 허브 잡의 안전 규칙(docs/deploy-static-sites.md)을 반드시 지킬 것.
//
// 이 파일은 "이렇게 만들면 된다"는 예시이며, 실제 잡 등록은 Jenkins 에서 사람이 한다.

pipeline {
    agent any

    tools {
        nodejs 'node18'          // Jenkins 전역 도구 이름에 맞출 것
    }

    environment {
        DEPLOY_STAGE = '/respal/deploy-openapi'                              // 지도 잡(/respal/deploy)과 달라야 한다
        DEPLOY_DST   = '/home/kuber-volume/sj-lab-webserver/html/openapi'
    }

    stages {
        stage('Checkout') {
            steps {
                git branch: 'main', url: 'https://github.com/stylealist/sj-lab-openapi-web.git'
            }
        }

        stage('Build') {
            steps {
                sh 'npm ci || npm install'
                sh 'npm run build'
                sh 'test -f build/index.html'      // 빈 산출물로 덮어쓰는 사고 방지
            }
        }

        stage('Publish') {
            steps {
                sshPublisher(publishers: [
                    sshPublisherDesc(
                        configName: 'sj-lab-webserver',      // Jenkins 에 등록된 SSH 서버 이름
                        transfers: [
                            sshTransfer(
                                sourceFiles: 'build/**',
                                removePrefix: 'build',
                                remoteDirectory: 'deploy-openapi',   // 스테이징 경로. 최종 웹 디렉터리를 직접 쓰지 말 것
                                cleanRemote: true,                   // 스테이징이라 안전하다
                                execCommand: '''
set -eu
SRC=/respal/deploy-openapi
DST=/home/kuber-volume/sj-lab-webserver/html/openapi

[ -f "$SRC/index.html" ] || { echo "index.html 없음 - 배포 중단"; exit 1; }

# 새 폴더에 먼저 펼친 뒤 바꿔치기한다 — 복사하는 동안 사이트가 비어 보이지 않게
NEW="${DST}.new.$$"
OLD="${DST}.old.$$"
rm -rf "$NEW"; mkdir -p "$NEW"
tar -C "$SRC" -cf - . | tar -C "$NEW" -xf -

[ -d "$DST" ] && mv "$DST" "$OLD" || true
mv "$NEW" "$DST"
rm -rf "$OLD"
echo "API 활용 페이지 배포 완료: $DST"
'''
                            )
                        ]
                    )
                ])
            }
        }

        stage('Verify') {
            steps {
                // 다른 잡이 지웠거나 허브 화면이 대신 뜨면 여기서 실패한다
                sh '''
curl -fsS https://sj-lab.co.kr/openapi/ | grep -q 'SJ-LAB OpenAPI' \
  || { echo "배포 확인 실패: /openapi/ 가 활용 페이지가 아니다"; exit 1; }
'''
            }
        }
    }
}
