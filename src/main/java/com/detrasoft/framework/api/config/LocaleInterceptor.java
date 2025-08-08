package com.detrasoft.framework.api.config;

import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Locale;

@Component
@SuppressWarnings("null")
public class LocaleInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        String locale = ((HttpServletRequest) request).getHeader(HttpHeaders.ACCEPT_LANGUAGE);
        if (locale != null && locale.length() >= 5) {
            var localeSplitVirgule = locale.split(",");
            var localeFormatted = localeSplitVirgule[0].replace('_', '-').split("-");
            if (localeFormatted.length > 1) {
                LocaleContextHolder.setLocale(new Locale(localeFormatted[0], localeFormatted[1]));
            } else {
                LocaleContextHolder.setLocale(new Locale(localeFormatted[0]));
            }
        }

        return true;
    }
}
