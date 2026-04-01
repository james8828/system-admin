package com.jnet.common.util;

import cn.hutool.core.util.StrUtil;

public class MessageUtils {

    private static final String BUNDLE_NAME = "messages";

    public static String message(String key, Object... args) {
        return StrUtil.format(key, args);
    }

}
