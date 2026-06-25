#!/bin/bash

set -e

APP_NAME="ai-app"
IMAGE_NAME="ai-app"

echo "========== 1. 停止容器 =========="
docker stop $APP_NAME || true

echo "========== 2. 删除容器 =========="
docker rm $APP_NAME || true

echo "========== 3. 构建镜像 =========="
docker build -t $IMAGE_NAME .

echo "========== 4. 启动容器 =========="
docker run -d \
  --name $APP_NAME \
  --restart unless-stopped \
  -p 18888:8888 \
  -v /root/aidata/ai/config/application.properties:/app/config/application.properties \
  -v /root/aidata/ai/logs:/app/logs \
  $IMAGE_NAME

echo "========== 部署完成 =========="
docker ps | grep $APP_NAME || true