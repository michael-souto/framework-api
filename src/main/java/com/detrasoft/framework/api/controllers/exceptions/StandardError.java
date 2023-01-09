package com.detrasoft.framework.api.controllers.exceptions;

import lombok.Builder;
import lombok.Getter;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
/*
* Implementation of pattern RFC 7807 for results
* */
@Getter
@Builder
public class StandardError implements Serializable {
	@Serial
	private static final long serialVersionUID = 1L;

	private Instant timestamp;
	private Integer status;
	private String title;
	private String detail;
	private String path;
}
