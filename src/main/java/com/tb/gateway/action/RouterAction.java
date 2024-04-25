package com.tb.gateway.action;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ClassUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.server.HttpServerRequest;
import cn.hutool.http.server.HttpServerResponse;
import cn.hutool.log.Log;
import com.google.gson.Gson;
import com.tb.gateway.annotation.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.*;

public class RouterAction {
    private static final Log LOG = Log.get(RouterAction.class);
    private static final Map<String, Method> actionList = new HashMap<>();
    private static final Gson gson = new Gson();

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
        if (CollUtil.isNotEmpty(actionList)) {
            return;
        }
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
        String method = request.getMethod();

        if (!actionList.containsKey(path)) {
            return false;
        }

        LOG.info(path);

        Method action = actionList.get(path);

        if (method.equals("POST")) {
            PostApi postApi = action.getAnnotation(PostApi.class);
            if (Objects.isNull(postApi)) {
                return false;
            }
        }

        if (method.equals("GET")) {
            GetApi getApi = action.getAnnotation(GetApi.class);
            if (Objects.isNull(getApi)) {
                return false;
            }
        }

        if (!auth(request, action)) {
            response.write("Auth Error!").close();
            return true;
        }

        Class<?> declaringClass = action.getDeclaringClass();
        Object o = ReflectUtil.newInstance(declaringClass);
        LOG.info(declaringClass.getName());

        Object[] objects = Arrays.stream(action.getParameters())
                .map(parameter -> {
                    Class<?> type = parameter.getType();
                    String typeName = type.getName();
                    if (typeName.equals(HttpServerRequest.class.getName())) {
                        return request;
                    }

                    if (typeName.equals(HttpServerResponse.class.getName())) {
                        return response;
                    }
                    Body body = parameter.getAnnotation(Body.class);
                    if (Objects.isNull(body)) {
                        return null;
                    }

                    if (typeName.equals(String.class.getName())) {
                        return request.getBody();
                    }

                    return gson.fromJson(request.getBody(), type);
                }).toArray();

        Object invoke;
        try {
            invoke = ReflectUtil.invoke(o, action, objects);
        } catch (Exception e) {
            e.printStackTrace();
            LOG.error(e);
            response.write(e.getMessage()).close();
            return true;
        }
        if (Objects.isNull(invoke)) {
            return true;
        }
        String s = gson.toJson(invoke);
        response.write(s).close();

        return true;
    }

    public static boolean auth(HttpServerRequest request, Method method) {
        Auth auth = method.getAnnotation(Auth.class);
        if (Objects.isNull(auth)) {
            return true;
        }
        boolean value = auth.value();
        if (value) {
            String login = LoginAction.cache.get("login");
            if (StrUtil.isBlank(login)) {
                return false;
            }
            return login.equals(request.getHeader("auth"));
        }
        return true;
    }

}