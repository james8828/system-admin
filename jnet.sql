-- JNet 权限框架数据库初始化脚本
-- 创建数据库
CREATE DATABASE IF NOT EXISTS jnet DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE jnet;

-- 1. 用户表（sys_user）
DROP TABLE IF EXISTS sys_user;
CREATE TABLE sys_user (
    user_id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户 ID',
    user_name VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    password VARCHAR(255) NOT NULL COMMENT '密码（加密）',
    nick_name VARCHAR(50) COMMENT '昵称',
    head_img_url VARCHAR(500) COMMENT '头像 URL',
    mobile VARCHAR(20) COMMENT '手机号',
    sex TINYINT DEFAULT 0 COMMENT '性别（0 未知 1 男 2 女）',
    email VARCHAR(100) COMMENT '邮箱',
    enabled BOOLEAN DEFAULT TRUE COMMENT '启用状态',
    type VARCHAR(20) DEFAULT 'USER' COMMENT '用户类型（USER ADMIN APP）',
    company VARCHAR(100) COMMENT '所属公司',
    open_id VARCHAR(100) COMMENT '第三方 openid',
    tenant_id BIGINT COMMENT '租户 ID',
    dept_id BIGINT COMMENT '部门 ID',
    create_by BIGINT COMMENT '创建人',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by BIGINT COMMENT '更新人',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    del_flag BOOLEAN DEFAULT FALSE COMMENT '删除标志（0 未删除 1 已删除）',
    remark VARCHAR(500) COMMENT '备注',
    INDEX idx_username (user_name),
    INDEX idx_mobile (mobile),
    INDEX idx_tenant (tenant_id),
    INDEX idx_dept (dept_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 2. 角色表（sys_role）
DROP TABLE IF EXISTS sys_role;
CREATE TABLE sys_role (
    role_id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '角色 ID',
    role_name VARCHAR(50) NOT NULL COMMENT '角色名称',
    role_key VARCHAR(50) NOT NULL UNIQUE COMMENT '角色权限字符串',
    role_sort INT DEFAULT 0 COMMENT '显示顺序',
    enabled BOOLEAN DEFAULT TRUE COMMENT '启用状态',
    data_scope VARCHAR(20) DEFAULT 'ALL' COMMENT '数据范围（ALL:全部数据 DEPT:本部门及以下 DEPT_ONLY:本部门 SELF:仅本人 CUSTOM:自定义）',
    tenant_id BIGINT COMMENT '租户 ID',
    create_by BIGINT COMMENT '创建人',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by BIGINT COMMENT '更新人',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    del_flag BOOLEAN DEFAULT FALSE COMMENT '删除标志',
    remark VARCHAR(500) COMMENT '备注',
    UNIQUE KEY uk_role_key (role_key),
    INDEX idx_tenant (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

-- 3. 菜单资源表（sys_menu）
DROP TABLE IF EXISTS sys_menu;
CREATE TABLE sys_menu (
    menu_id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '资源 ID',
    menu_name VARCHAR(50) NOT NULL COMMENT '资源名称',
    path VARCHAR(200) COMMENT '路由地址',
    component VARCHAR(255) COMMENT '前端组件路径',
    visible BOOLEAN DEFAULT TRUE COMMENT '是否显示在菜单栏',
    enabled BOOLEAN DEFAULT TRUE COMMENT '是否启用',
    perms VARCHAR(100) COMMENT '权限标识（如 user:list, user:create）',
    icon VARCHAR(100) COMMENT '图标',
    type TINYINT NOT NULL DEFAULT 0 COMMENT '资源类型（0=目录 1=菜单 2=按钮 3=接口）',
    parent_id BIGINT DEFAULT 0 COMMENT '父级资源 ID',
    order_num INT DEFAULT 0 COMMENT '显示顺序',
    query_params VARCHAR(255) COMMENT '路由参数',
    is_cache BOOLEAN DEFAULT FALSE COMMENT '是否缓存',
    is_frame BOOLEAN DEFAULT TRUE COMMENT '是否外链',
    tenant_id BIGINT COMMENT '租户 ID',
    create_by BIGINT COMMENT '创建人',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by BIGINT COMMENT '更新人',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    del_flag BOOLEAN DEFAULT FALSE COMMENT '删除标志',
    remark VARCHAR(500) COMMENT '备注',
    INDEX idx_parent (parent_id),
    INDEX idx_type (type),
    INDEX idx_perms (perms),
    INDEX idx_tenant (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜单资源表';

-- 4. 用户角色关联表（sys_user_role）
DROP TABLE IF EXISTS sys_user_role;
CREATE TABLE sys_user_role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    user_id BIGINT NOT NULL COMMENT '用户 ID',
    role_id BIGINT NOT NULL COMMENT '角色 ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_user_role (user_id, role_id),
    INDEX idx_user (user_id),
    INDEX idx_role (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联表';

-- 5. 角色菜单关联表（sys_role_menu）
DROP TABLE IF EXISTS sys_role_menu;
CREATE TABLE sys_role_menu (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    role_id BIGINT NOT NULL COMMENT '角色 ID',
    menu_id BIGINT NOT NULL COMMENT '菜单 ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_role_menu (role_id, menu_id),
    INDEX idx_role (role_id),
    INDEX idx_menu (menu_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色菜单关联表';

-- 6. OAuth2 客户端表（oauth2_registered_client）
DROP TABLE IF EXISTS oauth2_registered_client;
CREATE TABLE oauth2_registered_client (
    id VARCHAR(100) PRIMARY KEY COMMENT '客户端 ID',
    client_id VARCHAR(100) NOT NULL UNIQUE COMMENT '客户端 ID',
    client_id_issued_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '客户端 ID 签发时间',
    client_secret VARCHAR(200) COMMENT '客户端密钥',
    client_secret_expires_at DATETIME COMMENT '客户端密钥过期时间',
    client_name VARCHAR(200) NOT NULL COMMENT '客户端名称',
    client_authentication_methods VARCHAR(1000) COMMENT '客户端认证方式',
    authorization_grant_types VARCHAR(1000) COMMENT '授权类型',
    redirect_uris VARCHAR(1000) COMMENT '重定向 URI',
    post_logout_redirect_uris VARCHAR(1000) COMMENT '登出重定向 URI',
    scopes VARCHAR(1000) COMMENT '授权范围',
    client_settings VARCHAR(2000) COMMENT '客户端设置',
    token_settings VARCHAR(2000) COMMENT '令牌设置',
    tenant_id BIGINT COMMENT '租户 ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_client_id (client_id),
    INDEX idx_tenant (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='OAuth2 客户端表';

-- 7. OAuth2 授权表（oauth2_authorization）
DROP TABLE IF EXISTS oauth2_authorization;
CREATE TABLE oauth2_authorization (
    id VARCHAR(100) PRIMARY KEY COMMENT '授权 ID',
    registered_client_id VARCHAR(100) NOT NULL COMMENT '客户端 ID',
    principal_name VARCHAR(200) NOT NULL COMMENT '用户标识',
    authorization_grant_type VARCHAR(100) NOT NULL COMMENT '授权类型',
    authorized_scopes VARCHAR(1000) COMMENT '授权范围',
    attributes TEXT COMMENT '属性',
    state VARCHAR(500) COMMENT '状态',
    authorization_code_value TEXT COMMENT '授权码',
    authorization_code_issued_at DATETIME COMMENT '授权码签发时间',
    authorization_code_expires_at DATETIME COMMENT '授权码过期时间',
    authorization_code_metadata TEXT COMMENT '授权码元数据',
    access_token_value TEXT COMMENT '访问令牌',
    access_token_issued_at DATETIME COMMENT '访问令牌签发时间',
    access_token_expires_at DATETIME COMMENT '访问令牌过期时间',
    access_token_metadata TEXT COMMENT '访问令牌元数据',
    access_token_type VARCHAR(100) COMMENT '访问令牌类型',
    access_token_scopes VARCHAR(1000) COMMENT '访问令牌范围',
    oidc_id_token_value TEXT COMMENT 'OIDC ID 令牌',
    oidc_id_token_issued_at DATETIME COMMENT 'OIDC ID 令牌签发时间',
    oidc_id_token_expires_at DATETIME COMMENT 'OIDC ID 令牌过期时间',
    oidc_id_token_metadata TEXT COMMENT 'OIDC ID 令牌元数据',
    refresh_token_value TEXT COMMENT '刷新令牌',
    refresh_token_issued_at DATETIME COMMENT '刷新令牌签发时间',
    refresh_token_expires_at DATETIME COMMENT '刷新令牌过期时间',
    refresh_token_metadata TEXT COMMENT '刷新令牌元数据',
    user_code_value TEXT COMMENT '用户代码',
    user_code_issued_at DATETIME COMMENT '用户代码签发时间',
    user_code_expires_at DATETIME COMMENT '用户代码过期时间',
    user_code_metadata TEXT COMMENT '用户代码元数据',
    device_code_value TEXT COMMENT '设备代码',
    device_code_issued_at DATETIME COMMENT '设备代码签发时间',
    device_code_expires_at DATETIME COMMENT '设备代码过期时间',
    device_code_metadata TEXT COMMENT '设备代码元数据',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_client (registered_client_id),
    INDEX idx_principal (principal_name),
    INDEX idx_access_token (access_token_value(255)),
    INDEX idx_refresh_token (refresh_token_value(255))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='OAuth2 授权表';

-- 8. 部门表（sys_dept）
DROP TABLE IF EXISTS sys_dept;
CREATE TABLE sys_dept (
    dept_id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '部门 ID',
    parent_id BIGINT DEFAULT 0 COMMENT '父部门 ID',
    ancestors VARCHAR(500) COMMENT '祖级列表',
    dept_name VARCHAR(50) NOT NULL COMMENT '部门名称',
    order_num INT DEFAULT 0 COMMENT '显示顺序',
    leader VARCHAR(50) COMMENT '负责人',
    phone VARCHAR(20) COMMENT '联系电话',
    email VARCHAR(100) COMMENT '邮箱',
    status BOOLEAN DEFAULT TRUE COMMENT '部门状态',
    tenant_id BIGINT COMMENT '租户 ID',
    create_by BIGINT COMMENT '创建人',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by BIGINT COMMENT '更新人',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    del_flag BOOLEAN DEFAULT FALSE COMMENT '删除标志',
    INDEX idx_parent (parent_id),
    INDEX idx_tenant (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='部门表';

-- 9. 操作日志表（sys_oper_log）
DROP TABLE IF EXISTS sys_oper_log;
CREATE TABLE sys_oper_log (
    oper_id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '日志 ID',
    title VARCHAR(50) COMMENT '模块标题',
    business_type VARCHAR(20) COMMENT '业务类型',
    method VARCHAR(100) COMMENT '方法名称',
    request_method VARCHAR(10) COMMENT '请求方式',
    operator_type VARCHAR(20) COMMENT '操作人类别',
    oper_name VARCHAR(50) COMMENT '操作人员',
    dept_name VARCHAR(50) COMMENT '部门名称',
    oper_url VARCHAR(255) COMMENT '请求 URL',
    oper_ip VARCHAR(50) COMMENT '主机地址',
    oper_location VARCHAR(255) COMMENT '操作地点',
    oper_param VARCHAR(2000) COMMENT '请求参数',
    json_result VARCHAR(2000) COMMENT '返回参数',
    status BOOLEAN DEFAULT TRUE COMMENT '操作状态',
    error_msg VARCHAR(2000) COMMENT '错误消息',
    oper_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    cost_time BIGINT COMMENT '消耗时间',
    tenant_id BIGINT COMMENT '租户 ID',
    INDEX idx_oper_time (oper_time),
    INDEX idx_tenant (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志表';

-- 初始化数据
-- 插入默认租户
INSERT INTO sys_dept (dept_id, parent_id, ancestors, dept_name, order_num, leader, phone, email, status, tenant_id, create_by, create_time) 
VALUES (1, 0, '0', 'JNet 公司', 0, '管理员', '13800138000', 'admin@jnet.com', TRUE, 1, 1, NOW());

-- 插入默认用户（管理员）
INSERT INTO sys_user (user_id, user_name, password, nick_name, enabled, type, tenant_id, dept_id, create_by, create_time) 
VALUES (1, 'admin', '$2a$10$N.zPbJqz8k8k8k8k8k8k8uOZ0z8k8k8k8k8k8k8k8k8k8k8k8k8k8', '超级管理员', TRUE, 'ADMIN', 1, 1, 1, NOW());

-- 插入默认角色
INSERT INTO sys_role (role_id, role_name, role_key, role_sort, enabled, data_scope, tenant_id, create_by, create_time) 
VALUES (1, '超级管理员', 'admin', 0, TRUE, 'ALL', 1, 1, NOW());

-- 插入用户角色关联
INSERT INTO sys_user_role (user_id, role_id, create_time) VALUES (1, 1, NOW());

-- 插入默认菜单
INSERT INTO sys_menu (menu_id, menu_name, path, component, visible, enabled, perms, icon, type, parent_id, order_num, tenant_id, create_by, create_time) 
VALUES 
(1, '系统管理', '/system', 'Layout', TRUE, TRUE, NULL, 'system', 0, 0, 1, 1, 1, NOW()),
(2, '用户管理', '/system/users', 'system/user/index', TRUE, TRUE, 'system:user:list', 'user', 1, 1, 1, 1, 1, NOW()),
(3, '角色管理', '/system/roles', 'system/role/index', TRUE, TRUE, 'system:role:list', 'role', 1, 1, 2, 1, 1, NOW()),
(4, '菜单管理', '/system/menus', 'system/menu/index', TRUE, TRUE, 'system:menu:list', 'menu', 1, 1, 3, 1, 1, NOW()),
(5, '部门管理', '/system/depts', 'system/dept/index', TRUE, TRUE, 'system:dept:list', 'dept', 1, 1, 4, 1, 1, NOW()),
(6, '操作日志', '/system/operLogs', 'system/operLog/index', TRUE, TRUE, 'system:operLog:list', 'log', 1, 1, 5, 1, 1, NOW());

-- 插入角色菜单关联
INSERT INTO sys_role_menu (role_id, menu_id, create_time) VALUES (1, 1, NOW()), (1, 2, NOW()), (1, 3, NOW()), (1, 4, NOW()), (1, 5, NOW()), (1, 6, NOW());

-- 插入 OAuth2 客户端
INSERT INTO oauth2_registered_client (id, client_id, client_secret, client_name, client_authentication_methods, authorization_grant_types, redirect_uris, scopes, client_settings, token_settings, tenant_id, create_time) 
VALUES (
    '1',
    'jnet-client',
    '{noop}jnet-secret',
    'JNet 客户端',
    'client_secret_basic',
    'authorization_code,refresh_token,client_credentials',
    'http://localhost:8080/login/oauth2/code/jnet,http://localhost:8080/authorized',
    'openid,profile',
    '{"requireAuthorizationConsent":true}',
    '{"accessTokenTimeToLive":"3600","refreshTokenTimeToLive":"604800"}',
    1,
    NOW()
);
