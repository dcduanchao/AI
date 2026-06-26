# 部署启动文档

本文说明如何把项目拉到 Linux 服务器上，并通过 Docker 启动。

## 1. 环境要求

- Linux 服务器
- Docker
- 可访问 Git 仓库
- Java 21 只用于本地开发，容器运行时不需要单独安装

## 2. 拉取代码

```bash
git clone <你的仓库地址>
cd ai
```

## 3. 外部配置文件

项目支持把 `application.properties` 挂载到容器外部配置目录。

容器内读取路径：

```bash
/app/config/application.properties
```

日志目录：

```bash
/app/logs
```

宿主机建议目录：

```bash
/root/aidata/ai/config/application.properties
```

把原来的配置文件内容复制到这个位置，并按实际环境修改：

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `AI_API_KEY`
- `GROK_API_KEY`
- `JWT_SECRET`

### 3.1 跨域配置

允许所有域访问：

```properties
app.cors.allow-all=true
```

只允许指定域访问：

```properties
app.cors.allow-all=false
app.cors.allowed-origins=http://localhost:5173,http://ai.du-ai.top
```

常用完整配置：

```properties
app.cors.allow-all=false
app.cors.allowed-origins=http://localhost:5173,http://ai.du-ai.top
app.cors.allowed-methods=GET,POST,PUT,DELETE,OPTIONS
app.cors.allowed-headers=*
app.cors.exposed-headers=
app.cors.allow-credentials=true
app.cors.max-age=3600
```

## 4. 构建镜像

在项目根目录执行：

```bash
docker build -t ai-app .
```

## 5. 启动容器

 ```bash
docker run -d \
  --name ai-app \
  --restart unless-stopped \
  -p 18888:8888 \
  -v /root/aidata/ai/config/application.properties:/app/config/application.properties \
  -v /root/aidata/ai/logs:/app/logs \
  ai-app
```

如果还想通过环境变量覆盖部分配置，也可以继续加 `-e` 参数，例如：

```bash
docker run -d \
  --name ai-app \
  --restart unless-stopped \
  -p 18888:8888 \
  -v /root/aidata/ai/config/application.properties:/app/config/application.properties \
  -v /root/aidata/ai/logs:/app/logs \
  -e DB_URL='jdbc:mysql://127.0.0.1:3306/ai?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true' \
  -e DB_USERNAME='root' \
  -e DB_PASSWORD='123456' \
  -e AI_API_KEY='your-ai-key' \
  -e GROK_API_KEY='your-grok-key' \
  -e JWT_SECRET='please-use-a-long-random-secret' \
  ai-app
```

## 6. 验证启动

查看日志：

```bash
docker logs -f ai-app
```

访问接口：

```bash
curl http://127.0.0.1:8888
```

如果是前后端分离或网关接入，请把端口按实际暴露方式调整。

## 7. 更新代码

当仓库有新代码时，按下面步骤更新：

```bash
cd 
docker stop ai-app
docker rm ai-app
docker build -t ai-app .
docker run -d \
  --name ai-app \
  --restart unless-stopped \
  -p 18888:8888 \
  -v /root/aidata/ai/config/application.properties:/app/config/application.properties \
  -v /root/aidata/ai/logs:/app/logs \
  ai-app
```

如果只是改了 `application.properties`，不用重新构建镜像，直接修改宿主机上的配置文件后重启容器即可：

```bash
docker restart ai-app
```

日志会保存在宿主机目录 `/root/aidata/ai/logs`，按以下规则滚动：

- 保留最近 7 天
- 单个日志文件最大 100MB
- 当前日志文件名：`ai.log`

## 8. 常见问题

### 7.1 端口被占用

如果 8888 被占用，可以改成其它宿主机端口：

```bash
-p 18088:8888
```

### 7.2 配置未生效

确认宿主机文件路径正确，并且文件名必须是：

```bash
application.properties
```

同时检查容器内是否能看到这个文件：

```bash
docker exec -it ai-app ls -l /app/config
```

### 7.3 数据库连不上

检查：

- `DB_URL` 是否可达
- MySQL 是否允许远程连接
- 防火墙是否放通端口
- 用户名和密码是否正确
