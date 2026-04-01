package com.jnet.common.util;

import cn.hutool.json.JSONUtil;
import cn.hutool.json.JSONObject;

public class JsonUtils {

    public static String toJson(Object object) {
        return JSONUtil.toJsonStr(object);
    }

    public static <T> T toBean(String json, Class<T> clazz) {
        return JSONUtil.toBean(json, clazz);
    }

    public static JSONObject parseObject(String json) {
        return JSONUtil.parseObj(json);
    }

}
