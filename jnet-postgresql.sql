-- JNet 权限框架 PostgreSQL 数据库初始化脚本
-- 创建数据库（需要以超级用户身份执行）
-- CREATE DATABASE jnet WITH OWNER = postgres ENCODING = 'UTF8' LC_COLLATE = 'en_US.UTF-8' LC_CTYPE = 'en_US.UTF-8' TEMPLATE = template0;

-- 连接到 jnet 数据库
-- \c jnet

-- 1. 用户表（sys_user）
DROP TABLE IF EXISTS sys_user;
CREATE TABLE sys_user
(
    user_id      BIGSERIAL PRIMARY KEY,
    user_name    VARCHAR(50)  NOT NULL UNIQUE,
    password     VARCHAR(255) NOT NULL,
    nick_name    VARCHAR(50),
    head_img_url VARCHAR(500),
    mobile       VARCHAR(20),
    sex          SMALLINT    DEFAULT 0,
    email        VARCHAR(100),
    enabled      BOOLEAN     DEFAULT TRUE,
    type         VARCHAR(20) DEFAULT 'USER',
    company      VARCHAR(100),
    open_id      VARCHAR(100),
    tenant_id    BIGINT,
    dept_id      BIGINT,
    create_by    BIGINT,
    create_time  TIMESTAMP   DEFAULT CURRENT_TIMESTAMP,
    update_by    BIGINT,
    update_time  TIMESTAMP   DEFAULT CURRENT_TIMESTAMP,
    del_flag     BOOLEAN     DEFAULT FALSE,
    remark       VARCHAR(500)
);

COMMENT ON TABLE sys_user IS '用户表';
COMMENT ON COLUMN sys_user.user_id IS '用户 ID';
COMMENT ON COLUMN sys_user.user_name IS '用户名';
COMMENT ON COLUMN sys_user.password IS '密码（加密）';
COMMENT ON COLUMN sys_user.nick_name IS '昵称';
COMMENT ON COLUMN sys_user.head_img_url IS '头像 URL';
COMMENT ON COLUMN sys_user.mobile IS '手机号';
COMMENT ON COLUMN sys_user.sex IS '性别（0 未知 1 男 2 女）';
COMMENT ON COLUMN sys_user.email IS '邮箱';
COMMENT ON COLUMN sys_user.enabled IS '启用状态';
COMMENT ON COLUMN sys_user.type IS '用户类型（USER ADMIN APP）';
COMMENT ON COLUMN sys_user.company IS '所属公司';
COMMENT ON COLUMN sys_user.open_id IS '第三方 openid';
COMMENT ON COLUMN sys_user.tenant_id IS '租户 ID';
COMMENT ON COLUMN sys_user.dept_id IS '部门 ID';
COMMENT ON COLUMN sys_user.create_by IS '创建人';
COMMENT ON COLUMN sys_user.create_time IS '创建时间';
COMMENT ON COLUMN sys_user.update_by IS '更新人';
COMMENT ON COLUMN sys_user.update_time IS '更新时间';
COMMENT ON COLUMN sys_user.del_flag IS '删除标志（0 未删除 1 已删除）';
COMMENT ON COLUMN sys_user.remark IS '备注';

CREATE INDEX idx_sys_user_username ON sys_user (user_name);
CREATE INDEX idx_sys_user_mobile ON sys_user (mobile);
CREATE INDEX idx_sys_user_tenant ON sys_user (tenant_id);
CREATE INDEX idx_sys_user_dept ON sys_user (dept_id);

-- 2. 角色表（sys_role）
DROP TABLE IF EXISTS sys_role;
CREATE TABLE sys_role
(
    role_id     BIGSERIAL PRIMARY KEY,
    role_name   VARCHAR(50) NOT NULL,
    role_key    VARCHAR(50) NOT NULL UNIQUE,
    role_sort   SMALLINT    DEFAULT 0,
    enabled     BOOLEAN     DEFAULT TRUE,
    data_scope  VARCHAR(20) DEFAULT 'ALL',
    tenant_id   BIGINT,
    create_by   BIGINT,
    create_time TIMESTAMP   DEFAULT CURRENT_TIMESTAMP,
    update_by   BIGINT,
    update_time TIMESTAMP   DEFAULT CURRENT_TIMESTAMP,
    del_flag    BOOLEAN     DEFAULT FALSE,
    remark      VARCHAR(500)
);

COMMENT ON TABLE sys_role IS '角色表';
COMMENT ON COLUMN sys_role.role_id IS '角色 ID';
COMMENT ON COLUMN sys_role.role_name IS '角色名称';
COMMENT ON COLUMN sys_role.role_key IS '角色权限字符串';
COMMENT ON COLUMN sys_role.role_sort IS '显示顺序';
COMMENT ON COLUMN sys_role.enabled IS '启用状态';
COMMENT ON COLUMN sys_role.data_scope IS '数据范围（ALL:全部数据 DEPT:本部门及以下 DEPT_ONLY:本部门 SELF:仅本人 CUSTOM:自定义）';
COMMENT ON COLUMN sys_role.tenant_id IS '租户 ID';
COMMENT ON COLUMN sys_role.create_by IS '创建人';
COMMENT ON COLUMN sys_role.create_time IS '创建时间';
COMMENT ON COLUMN sys_role.update_by IS '更新人';
COMMENT ON COLUMN sys_role.update_time IS '更新时间';
COMMENT ON COLUMN sys_role.del_flag IS '删除标志';
COMMENT ON COLUMN sys_role.remark IS '备注';

CREATE UNIQUE INDEX uk_sys_role_key ON sys_role (role_key);
CREATE INDEX idx_sys_role_tenant ON sys_role (tenant_id);

-- 3. 菜单资源表（sys_menu）
DROP TABLE IF EXISTS sys_menu;
CREATE TABLE sys_menu
(
    menu_id      BIGSERIAL PRIMARY KEY,
    menu_name    VARCHAR(50) NOT NULL,
    path         VARCHAR(200),
    component    VARCHAR(255),
    visible      BOOLEAN              DEFAULT TRUE,
    enabled      BOOLEAN              DEFAULT TRUE,
    perms        VARCHAR(100),
    icon         VARCHAR(100),
    type         SMALLINT    NOT NULL DEFAULT 0,
    parent_id    BIGINT               DEFAULT 0,
    order_num    SMALLINT             DEFAULT 0,
    query_params VARCHAR(255),
    is_cache     BOOLEAN              DEFAULT FALSE,
    is_frame     BOOLEAN              DEFAULT TRUE,
    tenant_id    BIGINT,
    create_by    BIGINT,
    create_time  TIMESTAMP            DEFAULT CURRENT_TIMESTAMP,
    update_by    BIGINT,
    update_time  TIMESTAMP            DEFAULT CURRENT_TIMESTAMP,
    del_flag     BOOLEAN              DEFAULT FALSE,
    remark       VARCHAR(500)
);

COMMENT ON TABLE sys_menu IS '菜单资源表';
COMMENT ON COLUMN sys_menu.menu_id IS '资源 ID';
COMMENT ON COLUMN sys_menu.menu_name IS '资源名称';
COMMENT ON COLUMN sys_menu.path IS '路由地址';
COMMENT ON COLUMN sys_menu.component IS '前端组件路径';
COMMENT ON COLUMN sys_menu.visible IS '是否显示在菜单栏';
COMMENT ON COLUMN sys_menu.enabled IS '是否启用';
COMMENT ON COLUMN sys_menu.perms IS '权限标识（如 user:list, user:create）';
COMMENT ON COLUMN sys_menu.icon IS '图标';
COMMENT ON COLUMN sys_menu.type IS '资源类型（0=目录 1=菜单 2=按钮 3=接口）';
COMMENT ON COLUMN sys_menu.parent_id IS '父级资源 ID';
COMMENT ON COLUMN sys_menu.order_num IS '显示顺序';
COMMENT ON COLUMN sys_menu.query_params IS '路由参数';
COMMENT ON COLUMN sys_menu.is_cache IS '是否缓存';
COMMENT ON COLUMN sys_menu.is_frame IS '是否外链';
COMMENT ON COLUMN sys_menu.tenant_id IS '租户 ID';
COMMENT ON COLUMN sys_menu.create_by IS '创建人';
COMMENT ON COLUMN sys_menu.create_time IS '创建时间';
COMMENT ON COLUMN sys_menu.update_by IS '更新人';
COMMENT ON COLUMN sys_menu.update_time IS '更新时间';
COMMENT ON COLUMN sys_menu.del_flag IS '删除标志';
COMMENT ON COLUMN sys_menu.remark IS '备注';

CREATE INDEX idx_sys_menu_parent ON sys_menu (parent_id);
CREATE INDEX idx_sys_menu_type ON sys_menu (type);
CREATE INDEX idx_sys_menu_perms ON sys_menu (perms);
CREATE INDEX idx_sys_menu_tenant ON sys_menu (tenant_id);

-- 4. 用户角色关联表（sys_user_role）
DROP TABLE IF EXISTS sys_user_role;
CREATE TABLE sys_user_role
(
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT NOT NULL,
    role_id     BIGINT NOT NULL,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE sys_user_role IS '用户角色关联表';
COMMENT ON COLUMN sys_user_role.id IS '主键';
COMMENT ON COLUMN sys_user_role.user_id IS '用户 ID';
COMMENT ON COLUMN sys_user_role.role_id IS '角色 ID';
COMMENT ON COLUMN sys_user_role.create_time IS '创建时间';

CREATE UNIQUE INDEX uk_sys_user_role ON sys_user_role (user_id, role_id);
CREATE INDEX idx_sys_user_role_user ON sys_user_role (user_id);
CREATE INDEX idx_sys_user_role_role ON sys_user_role (role_id);

-- 5. 角色菜单关联表（sys_role_menu）
DROP TABLE IF EXISTS sys_role_menu;
CREATE TABLE sys_role_menu
(
    id          BIGSERIAL PRIMARY KEY,
    role_id     BIGINT NOT NULL,
    menu_id     BIGINT NOT NULL,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE sys_role_menu IS '角色菜单关联表';
COMMENT ON COLUMN sys_role_menu.id IS '主键';
COMMENT ON COLUMN sys_role_menu.role_id IS '角色 ID';
COMMENT ON COLUMN sys_role_menu.menu_id IS '菜单 ID';
COMMENT ON COLUMN sys_role_menu.create_time IS '创建时间';

CREATE UNIQUE INDEX uk_sys_role_menu ON sys_role_menu (role_id, menu_id);
CREATE INDEX idx_sys_role_menu_role ON sys_role_menu (role_id);
CREATE INDEX idx_sys_role_menu_menu ON sys_role_menu (menu_id);

-- 6. OAuth2 客户端表（oauth2_registered_client）
DROP TABLE IF EXISTS oauth2_registered_client;
CREATE TABLE oauth2_registered_client
(
    id                            VARCHAR(100) PRIMARY KEY,
    client_id                     VARCHAR(100) NOT NULL UNIQUE,
    client_id_issued_at           TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    client_secret                 VARCHAR(200),
    client_secret_expires_at      TIMESTAMP,
    client_name                   VARCHAR(200) NOT NULL,
    client_authentication_methods JSONB,  -- 认证方式 Set ["client_secret_basic", "private_key_jwt"]
    authorization_grant_types     JSONB,  -- 授权类型 Set ["authorization_code", "refresh_token"]
    redirect_uris                 JSONB,  -- 重定向 URI Set ["http://localhost:8080/callback"]
    post_logout_redirect_uris     JSONB,  -- 登出重定向 URI Set
    scopes                        JSONB,  -- 授权范围 Set ["openid", "profile"]
    client_settings               JSONB,  -- 客户端设置 {"require-authorization-consent": true, "require-proof-key": true}
    token_settings                JSONB,  -- 令牌设置 {"access-token-time-to-live": 3600, "refresh-token-time-to-live": 604800}
    tenant_id                     BIGINT,
    create_time                   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time                   TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE oauth2_registered_client IS 'OAuth2 客户端表';
COMMENT ON COLUMN oauth2_registered_client.id IS '客户端 ID';
COMMENT ON COLUMN oauth2_registered_client.client_id IS '客户端 ID';
COMMENT ON COLUMN oauth2_registered_client.client_id_issued_at IS '客户端 ID 签发时间';
COMMENT ON COLUMN oauth2_registered_client.client_secret IS '客户端密钥';
COMMENT ON COLUMN oauth2_registered_client.client_secret_expires_at IS '客户端密钥过期时间';
COMMENT ON COLUMN oauth2_registered_client.client_name IS '客户端名称';
COMMENT ON COLUMN oauth2_registered_client.client_authentication_methods IS '客户端认证方式 (JSONB Set) ["client_secret_basic", "private_key_jwt"]';
COMMENT ON COLUMN oauth2_registered_client.authorization_grant_types IS '授权类型 (JSONB Set) ["authorization_code", "refresh_token", "client_credentials"]';
COMMENT ON COLUMN oauth2_registered_client.redirect_uris IS '重定向 URI(JSONB Set) ["http://localhost:8080/callback"]';
COMMENT ON COLUMN oauth2_registered_client.post_logout_redirect_uris IS '登出重定向 URI(JSONB Set)';
COMMENT ON COLUMN oauth2_registered_client.scopes IS '授权范围 (JSONB Set) ["openid", "profile", "email"]';
COMMENT ON COLUMN oauth2_registered_client.client_settings IS '客户端设置 (JSONB 对象)
{
  "require-authorization-consent": true,  // 是否需要授权同意
  "require-proof-key": true,              // PKCE 是否需要 proof key
  "jwk-set-url": "https://...",           // JWK Set URL
  "token-endpoint-authentication-signing-algorithm": "RS256"  // Token 端点认证签名算法
}';
COMMENT ON COLUMN oauth2_registered_client.token_settings IS '令牌设置 (JSONB 对象)
{
  "access-token-time-to-live": 3600,              // Access Token TTL (秒)
  "access-token-format": "jwt",                   // Access Token 格式
  "refresh-token-time-to-live": 604800,           // Refresh Token TTL (秒)
  "reuse-refresh-tokens": false,                  // 是否重用 Refresh Token
  "authorization-code-time-to-live": 300,         // Authorization Code TTL (秒)
  "device-code-time-to-live": 900,                // Device Code TTL (秒)
  "id-token-signature-algorithm": "RS256"         // ID Token 签名算法
}';
COMMENT ON COLUMN oauth2_registered_client.tenant_id IS '租户 ID';
COMMENT ON COLUMN oauth2_registered_client.create_time IS '创建时间';
COMMENT ON COLUMN oauth2_registered_client.update_time IS '更新时间';

CREATE INDEX idx_oauth2_client_client_id ON oauth2_registered_client (client_id);
CREATE INDEX idx_oauth2_client_tenant ON oauth2_registered_client (tenant_id);

-- 7. OAuth2 授权表（oauth2_authorization）
DROP TABLE IF EXISTS oauth2_authorization;
CREATE TABLE oauth2_authorization
(
    id                            VARCHAR(100) PRIMARY KEY,
    registered_client_id          VARCHAR(100) NOT NULL,
    principal_name                VARCHAR(200) NOT NULL,
    authorization_grant_type      VARCHAR(100) NOT NULL,
    authorized_scopes             JSONB,
    attributes                    JSONB,
    state                         VARCHAR(500),
    authorization_code_value      TEXT,
    authorization_code_issued_at  TIMESTAMP,
    authorization_code_expires_at TIMESTAMP,
    authorization_code_metadata   JSONB,
    access_token_value            TEXT,
    access_token_issued_at        TIMESTAMP,
    access_token_expires_at       TIMESTAMP,
    access_token_metadata         JSONB,
    access_token_type             VARCHAR(100),
    access_token_scopes           JSONB,
    oidc_id_token_value           TEXT,
    oidc_id_token_issued_at       TIMESTAMP,
    oidc_id_token_expires_at      TIMESTAMP,
    oidc_id_token_metadata        JSONB,
    refresh_token_value           TEXT,
    refresh_token_issued_at       TIMESTAMP,
    refresh_token_expires_at      TIMESTAMP,
    refresh_token_metadata        JSONB,
    user_code_value               TEXT,
    user_code_issued_at           TIMESTAMP,
    user_code_expires_at          TIMESTAMP,
    user_code_metadata            JSONB,
    device_code_value             TEXT,
    device_code_issued_at         TIMESTAMP,
    device_code_expires_at        TIMESTAMP,
    device_code_metadata          JSONB,
    create_time                   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time                   TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE oauth2_authorization IS 'OAuth2 授权表';
COMMENT ON COLUMN oauth2_authorization.id IS '授权 ID';
COMMENT ON COLUMN oauth2_authorization.registered_client_id IS '客户端 ID';
COMMENT ON COLUMN oauth2_authorization.principal_name IS '用户标识';
COMMENT ON COLUMN oauth2_authorization.authorization_grant_type IS '授权类型';
COMMENT ON COLUMN oauth2_authorization.authorized_scopes IS '授权范围（JSONB 数组）';
COMMENT ON COLUMN oauth2_authorization.attributes IS '属性（JSONB 对象）';
COMMENT ON COLUMN oauth2_authorization.state IS '状态';
COMMENT ON COLUMN oauth2_authorization.authorization_code_value IS '授权码';
COMMENT ON COLUMN oauth2_authorization.authorization_code_issued_at IS '授权码签发时间';
COMMENT ON COLUMN oauth2_authorization.authorization_code_expires_at IS '授权码过期时间';
COMMENT ON COLUMN oauth2_authorization.authorization_code_metadata IS '授权码元数据（JSONB 对象）';
COMMENT ON COLUMN oauth2_authorization.access_token_value IS '访问令牌';
COMMENT ON COLUMN oauth2_authorization.access_token_issued_at IS '访问令牌签发时间';
COMMENT ON COLUMN oauth2_authorization.access_token_expires_at IS '访问令牌过期时间';
COMMENT ON COLUMN oauth2_authorization.access_token_metadata IS '访问令牌元数据（JSONB 对象）';
COMMENT ON COLUMN oauth2_authorization.access_token_type IS '访问令牌类型';
COMMENT ON COLUMN oauth2_authorization.access_token_scopes IS '访问令牌范围（JSONB 数组）';
COMMENT ON COLUMN oauth2_authorization.oidc_id_token_value IS 'OIDC ID 令牌';
COMMENT ON COLUMN oauth2_authorization.oidc_id_token_issued_at IS 'OIDC ID 令牌签发时间';
COMMENT ON COLUMN oauth2_authorization.oidc_id_token_expires_at IS 'OIDC ID 令牌过期时间';
COMMENT ON COLUMN oauth2_authorization.oidc_id_token_metadata IS 'OIDC ID 令牌元数据（JSONB 对象）';
COMMENT ON COLUMN oauth2_authorization.refresh_token_value IS '刷新令牌';
COMMENT ON COLUMN oauth2_authorization.refresh_token_issued_at IS '刷新令牌签发时间';
COMMENT ON COLUMN oauth2_authorization.refresh_token_expires_at IS '刷新令牌过期时间';
COMMENT ON COLUMN oauth2_authorization.refresh_token_metadata IS '刷新令牌元数据（JSONB 对象）';
COMMENT ON COLUMN oauth2_authorization.user_code_value IS '用户代码';
COMMENT ON COLUMN oauth2_authorization.user_code_issued_at IS '用户代码签发时间';
COMMENT ON COLUMN oauth2_authorization.user_code_expires_at IS '用户代码过期时间';
COMMENT ON COLUMN oauth2_authorization.user_code_metadata IS '用户代码元数据（JSONB 对象）';
COMMENT ON COLUMN oauth2_authorization.device_code_value IS '设备代码';
COMMENT ON COLUMN oauth2_authorization.device_code_issued_at IS '设备代码签发时间';
COMMENT ON COLUMN oauth2_authorization.device_code_expires_at IS '设备代码过期时间';
COMMENT ON COLUMN oauth2_authorization.device_code_metadata IS '设备代码元数据（JSONB 对象）';
COMMENT ON COLUMN oauth2_authorization.create_time IS '创建时间';
COMMENT ON COLUMN oauth2_authorization.update_time IS '更新时间';

CREATE INDEX idx_oauth2_auth_client ON oauth2_authorization (registered_client_id);
CREATE INDEX idx_oauth2_auth_principal ON oauth2_authorization (principal_name);
CREATE INDEX idx_oauth2_auth_access_token ON oauth2_authorization (access_token_value);
CREATE INDEX idx_oauth2_auth_refresh_token ON oauth2_authorization (refresh_token_value);

-- 8. 部门表（sys_dept）
DROP TABLE IF EXISTS sys_dept;
CREATE TABLE sys_dept
(
    dept_id     BIGSERIAL PRIMARY KEY,
    parent_id   BIGINT    DEFAULT 0,
    ancestors   VARCHAR(500),
    dept_name   VARCHAR(50) NOT NULL,
    order_num   SMALLINT  DEFAULT 0,
    leader      VARCHAR(50),
    phone       VARCHAR(20),
    email       VARCHAR(100),
    status      BOOLEAN   DEFAULT TRUE,
    tenant_id   BIGINT,
    create_by   BIGINT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by   BIGINT,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    del_flag    BOOLEAN   DEFAULT FALSE
);

COMMENT ON TABLE sys_dept IS '部门表';
COMMENT ON COLUMN sys_dept.dept_id IS '部门 ID';
COMMENT ON COLUMN sys_dept.parent_id IS '父部门 ID';
COMMENT ON COLUMN sys_dept.ancestors IS '祖级列表';
COMMENT ON COLUMN sys_dept.dept_name IS '部门名称';
COMMENT ON COLUMN sys_dept.order_num IS '显示顺序';
COMMENT ON COLUMN sys_dept.leader IS '负责人';
COMMENT ON COLUMN sys_dept.phone IS '联系电话';
COMMENT ON COLUMN sys_dept.email IS '邮箱';
COMMENT ON COLUMN sys_dept.status IS '部门状态';
COMMENT ON COLUMN sys_dept.tenant_id IS '租户 ID';
COMMENT ON COLUMN sys_dept.create_by IS '创建人';
COMMENT ON COLUMN sys_dept.create_time IS '创建时间';
COMMENT ON COLUMN sys_dept.update_by IS '更新人';
COMMENT ON COLUMN sys_dept.update_time IS '更新时间';
COMMENT ON COLUMN sys_dept.del_flag IS '删除标志';

CREATE INDEX idx_sys_dept_parent ON sys_dept (parent_id);
CREATE INDEX idx_sys_dept_tenant ON sys_dept (tenant_id);

-- 9. 操作日志表（sys_oper_log）
DROP TABLE IF EXISTS sys_oper_log;
CREATE TABLE sys_oper_log
(
    oper_id        BIGSERIAL PRIMARY KEY,
    title          VARCHAR(50),
    business_type  VARCHAR(20),
    method         VARCHAR(100),
    request_method VARCHAR(10),
    operator_type  VARCHAR(20),
    oper_name      VARCHAR(50),
    dept_name      VARCHAR(50),
    oper_url       VARCHAR(255),
    oper_ip        VARCHAR(50),
    oper_location  VARCHAR(255),
    oper_param     VARCHAR(2000),
    json_result    VARCHAR(2000),
    status         BOOLEAN   DEFAULT TRUE,
    error_msg      VARCHAR(2000),
    oper_time      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    cost_time      BIGINT,
    tenant_id      BIGINT
);

COMMENT ON TABLE sys_oper_log IS '操作日志表';

-- 初始化数据
-- 插入默认部门
INSERT INTO sys_dept (dept_id, parent_id, ancestors, dept_name, order_num, leader, phone, email, status, tenant_id,
                      create_by, create_time)
VALUES (1, 0, '0', 'JNet 公司', 0, '管理员', '13800138000', 'admin@jnet.com', TRUE, 1, 1, NOW());

-- 插入默认用户（管理员）
-- 密码：admin123 (使用 BCrypt 加密)
INSERT INTO sys_user (user_id, user_name, password, nick_name, enabled, type, tenant_id, dept_id, create_by,
                      create_time)
VALUES (1, 'admin', '$2a$10$qDHxM7NdHTiI/44jN/KECOJUReWS3HnoEvo2RVZ89hUdbr66FSEZe', '超级管理员', TRUE, 'ADMIN', 1, 1,
        1, NOW());

-- 插入默认角色
INSERT INTO sys_role (role_id, role_name, role_key, role_sort, enabled, data_scope, tenant_id, create_by, create_time)
VALUES (1, '超级管理员', 'admin', 0, TRUE, 'ALL', 1, 1, NOW());

-- 插入用户角色关联
INSERT INTO sys_user_role (user_id, role_id, create_time)
VALUES (1, 1, NOW());

-- 插入默认菜单
INSERT INTO sys_menu (menu_id, menu_name, path, component, visible, enabled, perms, icon, type, parent_id, order_num,
                      tenant_id, create_by, create_time)
VALUES (1, '系统管理', '/system', 'Layout', TRUE, TRUE, NULL, 'system', 0, 0, 1, 1, 1, NOW()),
       (2, '用户管理', '/system/users', 'system/user/index', TRUE, TRUE, 'system:user:list', 'user', 1, 1, 1, 1, 1,
        NOW()),
       (3, '角色管理', '/system/roles', 'system/role/index', TRUE, TRUE, 'system:role:list', 'role', 1, 1, 2, 1, 1,
        NOW()),
       (4, '菜单管理', '/system/menus', 'system/menu/index', TRUE, TRUE, 'system:menu:list', 'menu', 1, 1, 3, 1, 1,
        NOW()),
       (5, '部门管理', '/system/depts', 'system/dept/index', TRUE, TRUE, 'system:dept:list', 'dept', 1, 1, 4, 1, 1,
        NOW()),
       (6, '操作日志', '/system/operLogs', 'system/operLog/index', TRUE, TRUE, 'system:operLog:list', 'log', 1, 1, 5, 1,
        1, NOW());

INSERT INTO sys_menu (menu_id, menu_name, path, component, visible, enabled, perms, icon, type, parent_id, order_num,
                      tenant_id, create_by, create_time)
VALUES (7, '用户分页查询', '/api/system/user/page', NULL, FALSE, TRUE, 'system:user:page', NULL, 3, 2, 0, 1, 1, NOW());

-- 插入角色菜单关联
INSERT INTO sys_role_menu (role_id, menu_id, create_time)
VALUES (1, 1, NOW()),
       (1, 2, NOW()),
       (1, 3, NOW()),
       (1, 4, NOW()),
       (1, 5, NOW()),
       (1, 6, NOW()),
       (1, 7, NOW());


-- 插入 OAuth2 客户端
-- 注意：所有数组和对象都使用 JSONB 格式
INSERT INTO oauth2_registered_client (id, client_id, client_secret, client_name, client_authentication_methods,
                                      authorization_grant_types, redirect_uris, scopes, client_settings, token_settings,
                                      tenant_id, create_time)
VALUES ('1',
        'jnet-client',
        '{noop}jnet-secret',
        'JNet 客户端',
        '["client_secret_basic"]'::jsonb,
        '["authorization_code", "refresh_token", "client_credentials", "password"]'::jsonb,
        '["http://localhost:8080/login/oauth2/code/jnet", "http://localhost:8080/authorized"]'::jsonb,
        '["openid", "profile"]'::jsonb,
        '{"require-authorization-consent": true}'::jsonb,
           -- Token Settings：access-token-time-to-live=7200 秒（2 小时）, refresh-token-time-to-live=604800 秒（7 天）
        '{"access-token-time-to-live": 7200, "refresh-token-time-to-live": 604800, "reuse-refresh-tokens": false}'::jsonb,
        1,
        NOW());

-- 新增：支持 Authorization Code + PKCE 的客户端（适合 SPA/前端应用）
INSERT INTO oauth2_registered_client (id, client_id, client_secret, client_name, client_authentication_methods,
                                      authorization_grant_types, redirect_uris, post_logout_redirect_uris, scopes,
                                      client_settings, token_settings, tenant_id, create_time)
VALUES ('2',
        'jnet-pkce-client',
        NULL,
        'JNet PKCE 客户端',
        '["none"]'::jsonb,
        '["authorization_code", "refresh_token"]'::jsonb,
        '["http://localhost:5173/callback", "http://172.31.3.199:5173/callback"]'::jsonb,
        '["http://localhost:8080/logout"]'::jsonb,
        '["openid", "profile", "email"]'::jsonb,
        '{"require-authorization-consent": false, "require-proof-key": true}'::jsonb,
           -- Token Settings：access-token-time-to-live=7200 秒（2 小时）, refresh-token-time-to-live=604800 秒（7 天）
        '{"access-token-time-to-live": 7200, "refresh-token-time-to-live": 604800, "reuse-refresh-tokens": false}'::jsonb,
        1,
        NOW());


