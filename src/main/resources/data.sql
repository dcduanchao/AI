INSERT INTO ai_provider (ai_code, ai_name, adapter_type, base_url, api_key, enabled)
SELECT 'openai-compatible-demo', 'OpenAI Compatible Demo', 'OPENAI_COMPATIBLE', 'https://api.openai.com', 'replace-with-api-key', FALSE
WHERE NOT EXISTS (SELECT 1 FROM ai_provider WHERE ai_code = 'openai-compatible-demo');

-- 初始管理员；password 为 BCrypt 哈希（明文 dc673836112，strength 10）
INSERT INTO sys_user (username, password, nickname, enabled)
SELECT 'admin', '$2a$10$08JG2HoA8r7N6U.fgMwIROaC5kqVCtdunspiIUy4PiIexKiKc9qhm', '管理员', TRUE
WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE username = 'admin');
