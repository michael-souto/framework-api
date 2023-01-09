package com.detrasoft.framework.api.controllers.exceptions;

import lombok.Builder;
import lombok.Getter;

import java.io.Serial;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Getter
public class ValidationError extends StandardError {
	@Serial
	private static final long serialVersionUID = 1L;
	private List<FieldMessage> errors = new ArrayList<>();

	@Builder(builderMethodName = "standardBuilder")
	ValidationError(Instant timestamp, Integer status, String title, String detail, String path) {
		super(timestamp, status, title, detail, path);
	}

	public void addError(String fieldName, String message) {
		errors.add(new FieldMessage(fieldName, message));
	}
}
