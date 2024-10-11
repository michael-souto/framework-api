package com.detrasoft.framework.api.utils;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;

public class CustomMessageInterpolator {

    private final MessageSource messageSource;

    public CustomMessageInterpolator(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    /**
     * Interpola a mensagem substituindo '${field}' pelo nome traduzido do campo.
     * @param message A mensagem contendo '${field}'
     * @param fieldName O nome do campo a ser traduzido
     * @return A mensagem interpolada
     */
    public String interpolate(String message, String fieldName) {
        if (message.contains("{field}")) {
            String translatedFieldName = messageSource.getMessage(
                fieldName,
                null,
                fieldName,
                LocaleContextHolder.getLocale()
            );
            message = message.replace("{field}", translatedFieldName);
        }
        return message;
    }
}