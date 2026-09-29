// sj-lab-openapi (공개 API 서비스) 배포 파이프라인 — Jenkins 잡의 [Pipeline script] 에 붙여 넣는 내용
// mapservice-rest 잡과 같은 구조다(도구·스테이지 구성·자격 증명 동일).
//
// ── 흐름 ────────────────────────────────────────────────────────────────────
//     jar 빌드 → 이미지 빌드·push(NCP 레지스트리)
//   → sj-lab-k8s-manifests 의 sj-lab-openapi/values.yaml 에서 image.tag 를 빌드번호로 커밋
//   → ArgoCD 가 동기화(selfHeal·prune) → 롤아웃
//
// ── 자격 증명 (docs/k8s-secrets.md, 값은 Jenkins 에만 있다) ──────────────────
//     github_login : 저장소 checkout
//     ncp-api-key  : NCP 레지스트리 docker login (Access Key / Secret Key)
//     GitHub_token : 매니페스트 저장소에 image.tag 커밋·push
//
// ── 주의할 점 ───────────────────────────────────────────────────────────────
// 1) Dockerfile 이 **미리 빌드된 jar 를 복사**하므로 Build 단계가 먼저 끝나야 한다.
// 2) values.yaml 의 tag 는 따옴표 없이 적혀 있다(`tag: 1`). mapservice-rest 잡이 쓰는
//    `sed "s|tag: \".*\"|...|"` 는 **따옴표가 있을 때만** 바뀌므로, 여기서는 따옴표가 있든
//    없든 바뀌는 패턴을 쓴다. 바뀐 게 없으면 커밋을 건너뛴다(빈 커밋으로 잡이 실패하지 않게).
// 3) 여러 서비스 잡이 동시에 매니페스트를 push 하면 한 잡이 `cannot lock ref` 로 실패할 수 있다.
//    이미지는 이미 올라가 있으므로 그 잡만 재실행하면 된다(2026-09-16 실제 발생).
// 4) image.tag 는 이 잡이 관리하는 값이다. 사람이 임의로 낮추거나 되돌리지 말 것.

pipeline {
    agent any

    environment {
        IMAGE_TAG  = "${env.BUILD_NUMBER}"
        IMAGE_NAME = "sj-lab-openapi"
        REGISTRY   = "sj-lab-registry.kr.ncr.ntruss.com"
        CHART_DIR  = "sj-lab-openapi"          // sj-lab-k8s-manifests 안의 차트 디렉터리
    }

    tools {
        maven 'MAVEN_3.9.6'
    }

    triggers {
        githubPush() // GitHub Push 이벤트 트리거 설정
    }

    stages {
        stage('Workspace Cleanup') {
            steps {
                deleteDir() // Jenkins workspace 전체 삭제
            }
        }

        stage('Checkout') {
            steps {
                git branch: 'main',
                    credentialsId: 'github_login',
                    url: 'https://github.com/stylealist/sj-lab-openapi.git'
            }
        }

        stage('Build') {
            steps {
                // Maven 빌드를 통해 Spring Boot jar 파일 생성 (Dockerfile 이 이 jar 를 복사한다)
                sh 'mvn clean package -DskipTests'
                sh 'test -f target/sj-lab-openapi.jar'
            }
        }

        stage('Docker Build') {
            steps {
                withCredentials([usernamePassword(credentialsId: 'ncp-api-key', usernameVariable: 'NCP_ACCESS_KEY', passwordVariable: 'NCP_SECRET_KEY')]) {
                    sh '''
                        echo "$NCP_SECRET_KEY" | docker login ${REGISTRY} -u "$NCP_ACCESS_KEY" --password-stdin
                        docker build -t ${IMAGE_NAME}:${IMAGE_TAG} -t ${REGISTRY}/${IMAGE_NAME}:${IMAGE_TAG} .
                        docker push ${REGISTRY}/${IMAGE_NAME}:${IMAGE_TAG}
                    '''
                }
            }
        }

        stage('Update Kubernetes Manifest') {
            steps {
                withCredentials([string(credentialsId: 'GitHub_token', variable: 'GIT_TOKEN')]) {
                    sh '''
                        rm -rf sj-lab-k8s-manifests
                        git config --global user.email "stylealist@gmail.com"
                        git config --global user.name "stylealist"

                        git clone https://stylealist:${GIT_TOKEN}@github.com/stylealist/sj-lab-k8s-manifests.git
                        cd sj-lab-k8s-manifests

                        # image.tag 만 바꾼다. 따옴표가 있든(tag: "1") 없든(tag: 1) 같은 결과가 되게 한다.
                        sed -i "s|^\\( *tag: *\\).*|\\1${IMAGE_TAG}|" ${CHART_DIR}/values.yaml
                        grep -n "tag:" ${CHART_DIR}/values.yaml

                        git add ${CHART_DIR}/values.yaml
                        # 바뀐 게 없으면 커밋을 건너뛴다(빈 커밋으로 잡이 실패하지 않게)
                        if git diff --cached --quiet; then
                            echo "변경 없음 - 커밋 생략"
                        else
                            git commit -m "Update ${IMAGE_NAME} image tag to ${IMAGE_TAG} from Jenkins"
                            git push origin main
                        fi
                    '''
                }
            }
        }

        stage('Verify') {
            steps {
                // ArgoCD 동기화 + 롤아웃에는 시간이 걸리지만 얼마나 걸릴지는 그때그때 다르다.
                // 고정 대기(sleep 90)를 두면 10초 만에 끝나도 90초를 버리므로, 짧게 자주 확인하고
                // 되는 즉시 빠져나간다(최대 5분). 옛 파드가 내려가는 사이에는 503 이 날 수 있다.
                //
                // 한계: 이 확인은 "서비스가 응답한다"까지만 본다. 새 이미지가 떴는지까지 보려면
                //       빌드 번호를 응답에 실어야 한다(아래 주석 참고).
                sh '''
                    set -eu
                    DEADLINE=$(( $(date +%s) + 300 ))
                    ATTEMPT=0
                    until curl -fsS --max-time 10 https://api.sj-lab.co.kr/open-api/catalog | grep -q '"groups"'; do
                        ATTEMPT=$((ATTEMPT + 1))
                        if [ "$(date +%s)" -ge "$DEADLINE" ]; then
                            echo "❌ 배포 확인 실패(5분 초과): /open-api/catalog 응답이 카탈로그가 아니다"
                            exit 1
                        fi
                        echo "대기 중... (${ATTEMPT}회)"
                        sleep 10
                    done
                    echo "✅ 공개 API 카탈로그 확인 완료"
                '''
            }
        }
    }

    post {
        success {
            echo '✅ 공개 API 서비스가 배포되었습니다!'
        }
        failure {
            echo '❌ 빌드 또는 배포 실패! (매니페스트 push 충돌 cannot lock ref 이면 이 잡만 재실행)'
        }
    }
}

// ── 처음 배포할 때 ───────────────────────────────────────────────────────────
// 1) ArgoCD 에 sj-lab-openapi Application 을 등록해야 한다(차트 경로 sj-lab-openapi/).
//    등록 전에는 image.tag 만 올라가고 클러스터에는 아무 일도 일어나지 않는다.
// 2) 키 기능을 켜려면 api 스키마 표 2개(db/*.sql)와 openapi-db-credentials Secret 이 먼저 필요하다.
//    켜지 않으면(기본값) 키 API 만 503 이고 공개 API 조회는 정상 동작한다.
// 3) Verify 단계는 게이트웨이에 /open-api 라우트가 배포돼 있어야 통과한다
//    (sj-lab-apigateway 잡을 먼저 한 번 돌릴 것).
