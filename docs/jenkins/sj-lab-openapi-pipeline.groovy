// sj-lab-openapi (공개 API 서비스) 빌드·배포 파이프라인 — Jenkins 잡의 [Pipeline script] 에 붙여 넣는 내용
//
// 다른 백엔드 서비스(mapservice-rest, sj-lab-authserver 등)와 같은 흐름이다.
//   jar 빌드 → 이미지 빌드·push(NCP 레지스트리) → sj-lab-k8s-manifests 의 image.tag 커밋
//   → ArgoCD 가 동기화(selfHeal·prune) → 롤아웃
//
// Jenkins Credentials (docs/k8s-secrets.md 참고, 값은 Jenkins 에만 있다)
//   ncp-api-key   : NCP 레지스트리 docker login (Access Key / Secret Key)
//   GitHub_token  : 매니페스트 저장소에 image.tag 커밋·push
//   (체크아웃은 public 저장소라 자격 증명 없이 된다. 비공개로 바꾸면 Jenkins 에 등록된
//    아이디(`github_login` 또는 `GitHubAccount` — 잡마다 다르니 Credentials 화면에서 확인)를 넣을 것)
//
// 주의
//  - 여러 서비스 잡이 동시에 매니페스트를 push 하면 한 잡이 `cannot lock ref` 로 실패할 수 있다.
//    그때는 이미지가 이미 올라가 있으므로 그 잡만 재실행하면 된다(2026-09-16 실제 발생).
//  - image.tag 는 이 잡이 관리하는 값이다. 사람이 임의로 낮추거나 되돌리지 말 것.

pipeline {
    agent any

    environment {
        REGISTRY     = 'sj-lab-registry.kr.ncr.ntruss.com'
        IMAGE_NAME   = 'sj-lab-openapi'
        CHART_DIR    = 'sj-lab-openapi'                     // sj-lab-k8s-manifests 안의 차트 디렉터리
        MANIFEST_REPO = 'github.com/stylealist/sj-lab-k8s-manifests.git'
    }

    triggers {
        githubPush()
    }

    stages {
        stage('Checkout') {
            steps {
                git branch: 'main', url: 'https://github.com/stylealist/sj-lab-openapi.git'
            }
        }

        stage('Build & Test') {
            steps {
                // Dockerfile 이 미리 빌드된 jar 를 복사하므로 여기서 package 까지 끝낸다.
                sh 'chmod +x mvnw'
                sh './mvnw -B clean package'
                sh 'test -f target/sj-lab-openapi.jar'
            }
        }

        stage('Docker Build & Push') {
            steps {
                withCredentials([usernamePassword(
                        credentialsId: 'ncp-api-key',
                        usernameVariable: 'NCP_ACCESS_KEY',
                        passwordVariable: 'NCP_SECRET_KEY')]) {
                    sh '''
set -eu
echo "$NCP_SECRET_KEY" | docker login "$REGISTRY" -u "$NCP_ACCESS_KEY" --password-stdin
docker build -t "$REGISTRY/$IMAGE_NAME:$BUILD_NUMBER" -t "$REGISTRY/$IMAGE_NAME:latest" .
docker push "$REGISTRY/$IMAGE_NAME:$BUILD_NUMBER"
docker push "$REGISTRY/$IMAGE_NAME:latest"
docker logout "$REGISTRY"
'''
                }
            }
        }

        stage('Update Manifest (image.tag)') {
            steps {
                withCredentials([string(credentialsId: 'GitHub_token', variable: 'GITHUB_TOKEN')]) {
                    sh '''
set -eu
rm -rf manifests
git clone "https://$GITHUB_TOKEN@$MANIFEST_REPO" manifests
cd manifests

# values.yaml 의 image.tag 만 바꾼다(다른 값은 건드리지 않는다)
sed -i "s/^\\( *tag: *\\).*/\\1$BUILD_NUMBER/" "$CHART_DIR/values.yaml"
grep -n "tag:" "$CHART_DIR/values.yaml"

git config user.email "jenkins@sj-lab.co.kr"
git config user.name "Jenkins"
git add "$CHART_DIR/values.yaml"
git diff --cached --quiet && { echo "변경 없음 - 커밋 생략"; exit 0; }
git commit -m "Update $IMAGE_NAME image tag to $BUILD_NUMBER from Jenkins"
git push origin main
'''
                }
            }
        }

        stage('Verify (rollout 후)') {
            steps {
                // ArgoCD 동기화 + 롤아웃에 시간이 걸린다. 옛 파드가 내려가고 새 파드가 Eureka 에
                // 등록되기 전까지는 게이트웨이가 503 을 낼 수 있으므로 잠시 기다렸다가 확인한다.
                sh '''
set -eu
sleep 90
for i in 1 2 3 4 5; do
  if curl -fsS https://api.sj-lab.co.kr/open-api/catalog | grep -q '"groups"'; then
    echo "공개 API 카탈로그 확인 완료"
    exit 0
  fi
  echo "재시도 $i..."
  sleep 30
done
echo "배포 확인 실패: /open-api/catalog 응답이 카탈로그가 아니다"
exit 1
'''
            }
        }
    }

    post {
        failure {
            echo '실패 지점 확인: 매니페스트 push 충돌(cannot lock ref)이면 이 잡만 재실행하면 된다.'
        }
    }
}
