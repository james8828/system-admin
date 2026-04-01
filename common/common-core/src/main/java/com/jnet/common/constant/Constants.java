package com.jnet.common.constant;

/**
 * 常量枚举类
 * <p>
 * 将原有的字符串和整数常量重构为类型安全的枚举，提供更好的代码可读性和可维护性
 * </p>
 *
 * @author mu
 * @version 1.0
 * @since 2026/4/1
 */
public enum Constants {
    ;

    // ==================== 链路追踪相关常量 ====================

    /**
     * 链路追踪请求头
     * <p>
     * 用于在微服务间传递 Trace ID，实现分布式链路追踪
     * </p>
     */
    public static final String TRACE_ID_HEADER = "X-Trace-Id";

    // ==================== 内部服务认证相关常量 ====================

    /**
     * 内部服务认证请求头 - 服务名
     * <p>
     * 用于标识请求来源的服务名称
     * </p>
     */
    public static final String INTERNAL_SERVICE_AUTH_SERVICE_NAME = "X-Service-Name";

    /**
     * 内部服务认证请求头 - Token
     * <p>
     * 用于内部服务间认证的 Token
     * </p>
     */
    public static final String INTERNAL_SERVICE_AUTH_SERVICE_TOKEN = "X-Service-Token";

    /**
     * 认证成功标识
     * <p>
     * 表示内部服务认证成功
     * </p>
     */
    public static final String INTERNAL_SERVICE_AUTHENTICATED = "internal_service_authenticated";

    /**
     * 内部服务认证请求头 - 标识
     * <p>
     * 标记该请求为内部服务调用
     * </p>
     */
    public static final String INTERNAL_SERVICE_FLAG = "X-Internal-Service";

    // ==================== 租户相关常量 ====================

    /**
     * 默认租户 ID
     * <p>
     * 当请求未指定租户时使用的默认值
     * </p>
     */
    public static final String DEFAULT_TENANT_ID = "1";

    // ==================== 用户类型常量 ====================

    /**
     * 普通用户类型
     */
    public static final String USER_TYPE_USER = "USER";

    /**
     * 管理员用户类型
     */
    public static final String USER_TYPE_ADMIN = "ADMIN";

    /**
     * APP 用户类型
     */
    public static final String USER_TYPE_APP = "APP";

    // ==================== 数据范围常量 ====================

    /**
     * 全部数据权限
     * <p>
     * 用户可以访问所有数据
     * </p>
     */
    public static final String DATA_SCOPE_ALL = "ALL";

    /**
     * 部门数据权限
     * <p>
     * 用户可以访问本部门及下级部门的数据
     * </p>
     */
    public static final String DATA_SCOPE_DEPT = "DEPT";

    /**
     * 仅本部门数据权限
     * <p>
     * 用户只能访问本部门的数据
     * </p>
     */
    public static final String DATA_SCOPE_DEPT_ONLY = "DEPT_ONLY";

    /**
     * 仅本人数据权限
     * <p>
     * 用户只能访问自己创建的数据
     * </p>
     */
    public static final String DATA_SCOPE_SELF = "SELF";

    /**
     * 自定义数据权限
     * <p>
     * 根据具体业务需求定制的数据权限
     * </p>
     */
    public static final String DATA_SCOPE_CUSTOM = "CUSTOM";

    // ==================== 菜单类型常量 ====================

    /**
     * 目录类型菜单
     * <p>
     * 表示一级菜单或导航分组
     * </p>
     */
    public static final int MENU_TYPE_DIRECTORY = 0;

    /**
     * 菜单类型
     * <p>
     * 表示具体的功能菜单项
     * </p>
     */
    public static final int MENU_TYPE_MENU = 1;

    /**
     * 按钮类型
     * <p>
     * 表示页面中的操作按钮权限
     * </p>
     */
    public static final int MENU_TYPE_BUTTON = 2;

    /**
     * API 类型
     * <p>
     * 表示后端接口权限
     * </p>
     */
    public static final int MENU_TYPE_API = 3;

    // ==================== 性别常量 ====================

    /**
     * 未知性别
     */
    public static final int SEX_UNKNOWN = 0;

    /**
     * 男性
     */
    public static final int SEX_MALE = 1;

    /**
     * 女性
     */
    public static final int SEX_FEMALE = 2;

    // ==================== 状态常量 ====================

    /**
     * 启用状态
     * <p>
     * 表示记录、功能或账号处于可用状态
     * </p>
     */
    public static final int STATUS_ENABLED = 1;

    /**
     * 禁用状态
     * <p>
     * 表示记录、功能或账号处于不可用状态
     * </p>
     */
    public static final int STATUS_DISABLED = 0;
}
