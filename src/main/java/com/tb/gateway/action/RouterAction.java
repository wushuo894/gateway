package com.tb.gateway.action;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ClassUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.server.HttpServerRequest;
import cn.hutool.http.server.HttpServerResponse;
import cn.hutool.log.Log;
import com.tb.gateway.annotation.ActionApi;
import com.tb.gateway.annotation.GetApi;
import com.tb.gateway.annotation.PostApi;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.*;

public class RouterAction {
    private static final Log LOG = Log.get(RouterAction.class);
    public static Map<String, Method> actionList = new HashMap<>();

    public static String getUrl(ActionApi actionApi, Annotation... annotations) {
        String baseUrl = actionApi.value();

        for (Annotation annotation : annotations) {
            if (Objects.isNull(annotation)) {
                continue;
            }
            Map memberValues = (Map) ReflectUtil.getFieldValue(Proxy.getInvocationHandler(annotation), "memberValues");
            String url = (String) memberValues.get("value");
            return (baseUrl + url).replace("//", "/");
        }
        return "";
    }

    public static synchronized void loadAction() {
        Set<Class<?>> classes = ClassUtil.scanPackage("com.tb.gateway.action");

        for (Class<?> aClass : classes) {
            ActionApi actionApi = aClass.getAnnotation(ActionApi.class);
            if (Objects.isNull(actionApi)) {
                continue;
            }

            List<Method> methods = List.of(ReflectUtil.getMethods(aClass));
            for (Method method : methods) {
                PostApi postApi = method.getAnnotation(PostApi.class);
                GetApi getApi = method.getAnnotation(GetApi.class);
                String url = getUrl(actionApi, postApi, getApi);
                if (StrUtil.isBlank(url)) {
                    continue;
                }
                actionList.put(url, method);
            }
        }
    }

    public static boolean doAction(HttpServerRequest request, HttpServerResponse response) {
        if (CollUtil.isEmpty(actionList)) {
            RouterAction.loadAction();
        }
        String path = request.getPath();
        if (!actionList.containsKey(path)) {
            return false;
        }

        LOG.info(path);

        Method method = actionList.get(path);

        Class<?> declaringClass = method.getDeclaringClass();
        System.out.println(declaringClass);


        return true;
    }
}
