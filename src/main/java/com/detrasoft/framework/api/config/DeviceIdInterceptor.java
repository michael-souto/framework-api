package com.detrasoft.framework.api.config;

import com.detrasoft.framework.core.context.GenericContext;
import com.detrasoft.framework.core.library.GeneralFunctionsCore;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Locale;

@Component
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
