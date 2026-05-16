# ♟ Chess Engine Royal — Docker + EC2 Deployment

## Project Structure

```
ChessEngine_Royal/
├── Dockerfile              ← Multi-stage build (JDK builder → Nginx+JRE runtime)
├── docker-compose.yml      ← Local dev & EC2 deployment
├── .dockerignore
├── chess.html              ← Single-file chess app (zero dependencies)
├── compile.sh              ← Java build script (used inside Docker)
├── src/                    ← Java source code
│   └── chess/
│       ├── engine/         ← AI, move generator, evaluator
│       ├── model/          ← Board, pieces, moves
│       ├── network/        ← TCP server/client (port 9999)
│       ├── ui/             ← Console UI, main entry point
│       └── util/           ← FEN, PGN, opening book
├── docker/
│   ├── nginx.conf          ← Nginx main config
│   ├── chess-site.conf     ← Virtual host (port 80 → chess.html)
│   ├── supervisord.conf    ← Runs Nginx + Java server together
│   └── entrypoint.sh      ← Container startup script
└── scripts/
    ├── ec2-bootstrap.sh    ← Paste into EC2 User Data (installs Docker, runs app)
    └── deploy.sh           ← Local helper: build / push ECR / deploy to EC2
```

---

## What's Inside the Container

| Service        | Port | Description                              |
|----------------|------|------------------------------------------|
| Nginx          | 80   | Serves `chess.html` — the full chess UI  |
| Java ChessServer | 9999 | Optional TCP server for network play   |

Both are managed by **supervisord** so a single container runs everything.

---

## Option 1 — Local Development (Docker Compose)

```bash
# Build and start
docker compose up --build

# Open browser
open http://localhost

# Stop
docker compose down
```

To also start the Java TCP server:
```bash
START_JAVA_SERVER=true docker compose up --build
```

---

## Option 2 — Deploy to AWS EC2 (SCP method)

### Step 1 — Launch EC2 instance

| Setting         | Value                        |
|-----------------|------------------------------|
| AMI             | Ubuntu 22.04 LTS             |
| Instance type   | `t2.micro` (free tier) or `t3.small` |
| Security Group  | Port 22 (SSH), 80 (HTTP), 9999 (optional, TCP chess) |
| Key pair        | Create or select your `.pem` |

### Step 2 — Use EC2 User Data (fully automated)

In **Advanced Details → User Data**, paste the contents of `scripts/ec2-bootstrap.sh`.  
It will: install Docker, set up the project directory, and start the container.

Then SCP your files:
```bash
scp -i YOUR_KEY.pem -r . ubuntu@EC2_IP:/opt/chess-engine/
```

### Step 3 — Or SSH in and run manually

```bash
# Install Docker on EC2
ssh -i YOUR_KEY.pem ubuntu@EC2_IP
curl -fsSL https://get.docker.com | sudo sh
sudo usermod -aG docker ubuntu
newgrp docker

# Copy project then build
cd /opt/chess-engine
docker compose up -d --build
```

### Step 4 — Access

```
http://EC2_PUBLIC_IP
```

---

## Option 3 — Deploy via AWS ECR (recommended for teams / CI/CD)

```bash
# Set your values
export AWS_ACCOUNT_ID=123456789012
export AWS_REGION=us-east-1
export EC2_IP=YOUR_EC2_IP
export EC2_KEY=~/.ssh/your-key.pem

# Build and push image to ECR
./scripts/deploy.sh push

# Pull and run on EC2 (EC2 needs ECR IAM role or credentials)
./scripts/deploy.sh ecr-deploy
```

The deploy script also supports:
```bash
./scripts/deploy.sh build       # Build only (local)
./scripts/deploy.sh ssh-deploy  # SCP files + build on EC2
./scripts/deploy.sh logs        # Tail container logs from EC2
./scripts/deploy.sh stop        # Stop container on EC2
```

---

## EC2 IAM Role (for ECR pull)

Attach this policy to your EC2 instance role so it can pull from ECR without storing credentials:

```json
{
  "Version": "2012-10-17",
  "Statement": [{
    "Effect": "Allow",
    "Action": [
      "ecr:GetDownloadUrlForLayer",
      "ecr:BatchGetImage",
      "ecr:GetAuthorizationToken"
    ],
    "Resource": "*"
  }]
}
```

---

## Useful Docker Commands (on EC2)

```bash
# View running container
docker ps

# View logs
docker logs -f chess-engine-royal

# Restart
docker compose restart

# Rebuild after code change
docker compose up -d --build

# Shell into container
docker exec -it chess-engine-royal sh

# Stop everything
docker compose down
```

---

## Architecture Diagram

```
Internet
   │
   ▼
EC2 (Ubuntu 22.04)
   │
   ▼
Docker Container: chess-engine-royal
   ├── supervisord (process manager)
   │   ├── nginx       :80  ──► /var/www/chess/chess.html
   │   └── ChessServer :9999 (optional, enable via START_JAVA_SERVER=true)
   └── Volumes: chess-logs
```

---

## Environment Variables

| Variable           | Default  | Description                          |
|--------------------|----------|--------------------------------------|
| `START_JAVA_SERVER`| `false`  | Set `true` to start Java TCP server  |

---

## Security Notes

- The chess HTML app runs entirely in the browser (no backend needed for single-player/local games).
- Port 9999 (Java TCP server) is for **network multiplayer** only — keep it closed in the Security Group if not using it.
- For HTTPS, place an **AWS ALB** or **CloudFront** in front with an ACM certificate.
