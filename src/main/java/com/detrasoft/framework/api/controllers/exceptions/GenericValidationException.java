package com.detrasoft.framework.api.controllers.exceptions;

public class GenericValidationException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public GenericValidationException(String msg) {
		super(msg);
	}
}
