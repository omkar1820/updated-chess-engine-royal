#!/bin/bash
# ════════════════════════════════════════════════════════
#  Chess Engine Royal — Deploy Helper
#  Run from your LOCAL machine (not the EC2 instance)
#
#  Commands:
#    ./scripts/deploy.sh build          — Build Docker image locally
#    ./scripts/deploy.sh push           — Push to AWS ECR
#    ./scripts/deploy.sh ssh-deploy     — SCP + run on EC2
#    ./scripts/deploy.sh ecr-deploy     — Pull from ECR on EC2
# ════════════════════════════════════════════════════════
set -e

# ── CONFIG — edit these ───────────────────────────────────
AWS_REGION="${AWS_REGION:-us-east-1}"
AWS_ACCOUNT_ID="${AWS_ACCOUNT_ID:-123456789012}"  # replace with yours
ECR_REPO="chess-engine-royal"
IMAGE_TAG="${IMAGE_TAG:-latest}"
EC2_IP="${EC2_IP:-}"           # set via env: EC2_IP=1.2.3.4 ./deploy.sh ssh-deploy
EC2_KEY="${EC2_KEY:-~/.ssh/your-key.pem}"
EC2_USER="${EC2_USER:-ubuntu}"

ECR_URI="$AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com/$ECR_REPO"

# ─────────────────────────────────────────────────────────
usage() {
    echo "Usage: $0 [build|push|ssh-deploy|ecr-deploy|logs|stop]"
    exit 1
}

build() {
    echo "🔨 Building Docker image..."
    docker build -t "$ECR_REPO:$IMAGE_TAG" .
    echo "✓ Built $ECR_REPO:$IMAGE_TAG"
}

push_ecr() {
    echo "📦 Pushing to ECR: $ECR_URI"
    aws ecr get-login-password --region "$AWS_REGION" \
        | docker login --username AWS --password-stdin "$AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com"

    # Create repo if it doesn't exist
    aws ecr describe-repositories --repository-names "$ECR_REPO" --region "$AWS_REGION" 2>/dev/null || \
        aws ecr create-repository --repository-name "$ECR_REPO" --region "$AWS_REGION"

    docker tag "$ECR_REPO:$IMAGE_TAG" "$ECR_URI:$IMAGE_TAG"
    docker push "$ECR_URI:$IMAGE_TAG"
    echo "✓ Pushed to $ECR_URI:$IMAGE_TAG"
}

ssh_deploy() {
    [ -z "$EC2_IP" ] && { echo "Set EC2_IP env var first"; exit 1; }
    echo "🚀 Deploying to EC2: $EC2_IP"

    # Upload project files
    scp -i "$EC2_KEY" -r \
        Dockerfile docker-compose.yml docker/ chess.html src/ compile.sh \
        "$EC2_USER@$EC2_IP:/opt/chess-engine/"

    # Build and start on EC2
    ssh -i "$EC2_KEY" "$EC2_USER@$EC2_IP" "
        set -e
        cd /opt/chess-engine
        docker compose down --remove-orphans 2>/dev/null || true
        docker compose up -d --build
        echo '✓ Chess Engine running!'
        docker ps
    "
    echo "✓ Live at http://$EC2_IP"
}

ecr_deploy() {
    [ -z "$EC2_IP" ] && { echo "Set EC2_IP env var first"; exit 1; }
    echo "🚀 Deploying ECR image to EC2: $EC2_IP"

    ssh -i "$EC2_KEY" "$EC2_USER@$EC2_IP" "
        set -e
        aws ecr get-login-password --region $AWS_REGION \
            | docker login --username AWS --password-stdin $AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com

        docker pull $ECR_URI:$IMAGE_TAG
        docker stop chess-engine-royal 2>/dev/null || true
        docker rm chess-engine-royal 2>/dev/null || true
        docker run -d \
            --name chess-engine-royal \
            --restart unless-stopped \
            -p 80:80 \
            -p 9999:9999 \
            $ECR_URI:$IMAGE_TAG
        echo '✓ Container running'
        docker ps
    "
    echo "✓ Live at http://$EC2_IP"
}

show_logs() {
    [ -z "$EC2_IP" ] && { echo "Set EC2_IP env var first"; exit 1; }
    ssh -i "$EC2_KEY" "$EC2_USER@$EC2_IP" "docker logs -f chess-engine-royal"
}

stop_container() {
    [ -z "$EC2_IP" ] && { echo "Set EC2_IP env var first"; exit 1; }
    ssh -i "$EC2_KEY" "$EC2_USER@$EC2_IP" "
        docker compose -f /opt/chess-engine/docker-compose.yml down
        echo '✓ Stopped'
    "
}

# ── Dispatch ──────────────────────────────────────────────
case "${1:-}" in
    build)       build ;;
    push)        build && push_ecr ;;
    ssh-deploy)  ssh_deploy ;;
    ecr-deploy)  ecr_deploy ;;
    logs)        show_logs ;;
    stop)        stop_container ;;
    *)           usage ;;
esac
