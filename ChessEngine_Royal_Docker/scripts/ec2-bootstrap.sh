#!/bin/bash
# ════════════════════════════════════════════════════════
#  Chess Engine Royal — EC2 User Data / Bootstrap Script
#
#  Paste this into EC2 → Advanced → User Data when launching
#  a new instance, OR run it manually after SSH-ing in.
#
#  Tested on: Ubuntu 22.04 LTS, Amazon Linux 2023
#  Instance:  t2.micro or larger
# ════════════════════════════════════════════════════════
set -e

LOG="/var/log/chess-bootstrap.log"
exec > >(tee -a "$LOG") 2>&1

echo "======================================"
echo "  Chess Engine — EC2 Bootstrap"
echo "  $(date)"
echo "======================================"

# ── Detect OS ─────────────────────────────────────────────
if command -v apt-get &>/dev/null; then
    PKG_MANAGER="apt"
elif command -v yum &>/dev/null; then
    PKG_MANAGER="yum"
else
    echo "Unsupported OS"; exit 1
fi

# ── 1. Update system ──────────────────────────────────────
echo "[1/6] Updating system..."
if [ "$PKG_MANAGER" = "apt" ]; then
    apt-get update -y && apt-get upgrade -y
else
    yum update -y
fi

# ── 2. Install Docker ─────────────────────────────────────
echo "[2/6] Installing Docker..."
if ! command -v docker &>/dev/null; then
    if [ "$PKG_MANAGER" = "apt" ]; then
        apt-get install -y ca-certificates curl gnupg lsb-release
        install -m 0755 -d /etc/apt/keyrings
        curl -fsSL https://download.docker.com/linux/ubuntu/gpg \
            | gpg --dearmor -o /etc/apt/keyrings/docker.gpg
        echo \
          "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] \
          https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable" \
          | tee /etc/apt/sources.list.d/docker.list > /dev/null
        apt-get update -y
        apt-get install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin
    else
        yum install -y docker
        yum install -y docker-compose-plugin 2>/dev/null || \
          curl -SL "https://github.com/docker/compose/releases/latest/download/docker-compose-linux-$(uname -m)" \
            -o /usr/local/bin/docker-compose && chmod +x /usr/local/bin/docker-compose
    fi
fi

systemctl enable docker
systemctl start docker

# Add ubuntu/ec2-user to docker group (so no sudo needed)
MAIN_USER=$(id -un 1000 2>/dev/null || echo ubuntu)
usermod -aG docker "$MAIN_USER" || true

echo "Docker $(docker --version) installed ✓"

# ── 3. Install Git ────────────────────────────────────────
echo "[3/6] Installing Git..."
if [ "$PKG_MANAGER" = "apt" ]; then
    apt-get install -y git unzip
else
    yum install -y git unzip
fi

# ── 4. Copy project files ─────────────────────────────────
echo "[4/6] Setting up project..."
PROJECT_DIR="/opt/chess-engine"
mkdir -p "$PROJECT_DIR"

# If you are running this as User Data, embed your project here OR
# pull from S3 / Git. Examples:

# Option A — Pull from S3 (recommended for CI/CD):
# aws s3 cp s3://YOUR-BUCKET/ChessEngine_Royal.zip /tmp/chess.zip
# unzip -q /tmp/chess.zip -d "$PROJECT_DIR"
# mv "$PROJECT_DIR"/ChessEngine_Royal/* "$PROJECT_DIR"/

# Option B — Pull from GitHub:
# git clone https://github.com/YOUR_USER/chess-engine-royal.git "$PROJECT_DIR"

# Option C — SCP files manually (done before running this script):
# scp -i KEY.pem -r ChessEngine_Royal/ ubuntu@EC2_IP:/opt/chess-engine/

echo "  → Project dir: $PROJECT_DIR"
echo "  → Populate it via S3, Git, or SCP (see comments in script)"

# ── 5. Build & Start container ────────────────────────────
echo "[5/6] Building Docker image and starting container..."
cd "$PROJECT_DIR"

if [ -f "docker-compose.yml" ]; then
    docker compose up -d --build
    echo "Container started via Docker Compose ✓"
else
    echo "  ⚠ docker-compose.yml not found — build and run manually:"
    echo "    docker build -t chess-engine-royal ."
    echo "    docker run -d -p 80:80 --name chess chess-engine-royal"
fi

# ── 6. Configure firewall ─────────────────────────────────
echo "[6/6] Checking firewall..."
if command -v ufw &>/dev/null; then
    ufw allow 80/tcp
    ufw allow 22/tcp
    ufw --force enable
    echo "UFW configured ✓"
fi

PUBLIC_IP=$(curl -s http://169.254.169.254/latest/meta-data/public-ipv4 2>/dev/null || echo "YOUR_EC2_IP")

echo ""
echo "======================================"
echo "  ✓ Chess Engine Royal is live!"
echo "  Open: http://$PUBLIC_IP"
echo "======================================"
