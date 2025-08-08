package com.detrasoft.framework.api.config;

import com.detrasoft.framework.core.context.GenericContext;
import com.detrasoft.framework.core.library.GeneralFunctionsCore;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
@SuppressWarnings("null")
public class DeviceIdInterceptor implements HandlerInterceptor {

    static final String DEVICE_ID = "Device-Id";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        String deviceId = ((HttpServletRequest) request).getHeader(DEVICE_ID);
        if (deviceId != null && GeneralFunctionsCore.isValidUUID(deviceId)) {
            GenericContext.setContexts("device_id", deviceId);
        }
        return true;
    }
}
