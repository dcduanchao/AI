# Nginx 反向代理配置

本文说明如何通过域名 `ai.du-ai.top` 访问本机运行在 `1888` 端口的服务。

## 1. 域名说明

推荐使用：

```text
ai.du-ai.top
```

不要使用 `@`，例如 `ai@du-ai.top` 不是合法域名。

## 2. 访问方式

支持两种方式：

- 根路径访问：`http://ai.du-ai.top/`
- 子路径访问：`http://ai.du-ai.top/aichat/`

如果你的服务只给当前项目使用，推荐子路径方式，便于和其他站点共用同一个域名。

## 3. 基础前提

- DNS 已把 `ai.du-ai.top` 指向服务器公网 IP
- 服务器已安装 Nginx
- 本机应用已监听 `1888` 端口
- 防火墙已放行 `80` 端口

## 4. 根路径代理

如果希望直接通过 `http://ai.du-ai.top/` 访问：

```nginx
server {
    listen 80;
    server_name ai.du-ai.top;

    location / {
        proxy_pass http://127.0.0.1:1888;
        proxy_http_version 1.1;

        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;

        proxy_connect_timeout 60s;
        proxy_read_timeout 3600s;
        proxy_send_timeout 3600s;

        proxy_buffering off;
    }
}
```

## 5. 子路径代理

如果希望通过 `http://ai.du-ai.top/aichat/` 访问：

```nginx
server {
    listen 80;
    server_name ai.du-ai.top;

    location = /aichat {
        return 301 /aichat/;
    }

    location /aichat/ {
        proxy_pass http://127.0.0.1:1888/;
        proxy_http_version 1.1;

        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_set_header X-Forwarded-Prefix /aichat;

        proxy_connect_timeout 60s;
        proxy_read_timeout 3600s;
        proxy_send_timeout 3600s;

        proxy_buffering off;
    }
}
```

注意：

- `proxy_pass http://127.0.0.1:1888/;` 末尾的 `/` 很重要，会去掉 `/aichat/` 前缀
- 后端仍然按根路径运行，不需要额外改 Spring Boot 路径

## 6. 端口映射

如果容器内服务监听 `8888`，宿主机暴露 `1888`，启动时这样写：

```bash
docker run -d \
  --name ai-app \
  -p 1888:8888 \
  ai-app
```

如果应用本身就监听 `1888`，则直接映射：

```bash
docker run -d \
  --name ai-app \
  -p 1888:1888 \
  ai-app
```

## 7. 常用检查命令

检查 Nginx 配置：

```bash
nginx -t
```

重载配置：

```bash
systemctl reload nginx
```

查看监听端口：

```bash
ss -lntp | grep 1888
```

## 8. 常见问题

### 8.1 页面资源 404

如果你用子路径 `/aichat/` 访问，前端静态资源也要支持这个前缀，否则可能出现资源 404。

### 8.2 后端接口路径不对

如果后端接口出现 404，优先确认：

- `proxy_pass` 是否写成了带尾部 `/`
- 访问地址是否是 `/aichat/`
- 后端是否真的监听在 `1888`

### 8.3 连接超时

长连接或流式接口建议保留较大的超时时间，并关闭缓冲：

- `proxy_read_timeout 3600s`
- `proxy_send_timeout 3600s`
- `proxy_buffering off`

