package com.detrasoft.framework.api.controllers.exceptions;

import com.detrasoft.framework.core.notification.Message;
import com.detrasoft.framework.crud.services.exceptions.DatabaseException;
import com.detrasoft.framework.crud.services.exceptions.EntityValidationException;
import com.detrasoft.framework.crud.services.exceptions.ResourceNotFoundException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.time.Instant;


@ControllerAdvice
public class ResourceExceptionHandler {

	@Autowired
	private MessageSource messageSource;

	@ExceptionHandler(Exception.class)
	public ResponseEntity<Object> handleUncaught(Exception ex, HttpServletRequest request) {
		HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
		ex.printStackTrace();
		return ResponseEntity.status(status).body(createStandardError(
				status,
				"Resource not found",
				"An unexpected internal system error has occurred. Please try again and if the problem persists, contact your system administrator.",
				request.getRequestURI()));
	}
	
	@ExceptionHandler(NoSuchMessageException.class)
	public ResponseEntity<Object> handleUncaught(NoSuchMessageException ex, HttpServletRequest request) {
		HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
		ex.printStackTrace();
		return ResponseEntity.status(status).body(createStandardError(
				status,
				"Error of development of API.",
				ex.getMessage(),
				request.getRequestURI()));
	}
	
	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<StandardError> entityNotFound(IllegalArgumentException e, HttpServletRequest request) {
		HttpStatus status = HttpStatus.BAD_REQUEST;
		return ResponseEntity.status(status).body(createStandardError(
				status,
				"Resource not found",
				e.getMessage(),
				request.getRequestURI()));
	}

	@ExceptionHandler(NoResourceFoundException.class)
	public ResponseEntity<StandardError> resourceNotFound(NoResourceFoundException e, HttpServletRequest request) {
		HttpStatus status = HttpStatus.NOT_FOUND;
		return ResponseEntity.status(status).body(createStandardError(
				status,
				"Resource not found",
				e.getMessage(),
				request.getRequestURI()));
	}

	@ExceptionHandler(ResourceNotFoundException.class)
	public ResponseEntity<StandardError> entityNotFound(ResourceNotFoundException e, HttpServletRequest request) {
		HttpStatus status = HttpStatus.NOT_FOUND;
		return ResponseEntity.status(status).body(createStandardError(
				status,
				"Resource not found",
				e.getMessage(),
				request.getRequestURI()));
	}

	@ExceptionHandler(NoHandlerFoundException.class)
	public ResponseEntity<StandardError> entityNotFound(NoHandlerFoundException e, HttpServletRequest request) {
		HttpStatus status = HttpStatus.NOT_FOUND;
		return ResponseEntity.status(status).body(createStandardError(
				status,
				"Resource not found",
				e.getMessage(),
				request.getRequestURI()));
	}
	
	@ExceptionHandler(DatabaseException.class)
	public ResponseEntity<StandardError> database(DatabaseException e, HttpServletRequest request) {
		HttpStatus status = HttpStatus.BAD_REQUEST;
		return ResponseEntity.status(status).body(createStandardError(
				status,
				"Database exception",
				e.getMessage(),
				request.getRequestURI()));
	}

	@ExceptionHandler(BindException.class)
	public ResponseEntity<StandardError> database(BindException e, HttpServletRequest request) {
		HttpStatus status = HttpStatus.BAD_REQUEST;

		ValidationError err = ValidationError.standardBuilder()
				.timestamp(Instant.now())
				.status(status.value())
				.title("Validation exception")
				.detail("Erros na entrada das informações, consulte as mensagens para mais detalhes")
				.path(request.getRequestURI())
				.build();

		for (ObjectError objectError : e.getBindingResult().getAllErrors()) {

			if (objectError instanceof FieldError) {
				err.addError(((FieldError) objectError).getField(), messageSource.getMessage(((FieldError) objectError), LocaleContextHolder.getLocale()));
			} else {
				err.addError(((ObjectError) objectError).getObjectName(), messageSource.getMessage(((ObjectError) objectError), LocaleContextHolder.getLocale()));
			}
		}


		return ResponseEntity.status(status).body(err);
	}

	@ExceptionHandler(EntityValidationException.class)
	public ResponseEntity<StandardError> database(EntityValidationException e, HttpServletRequest request) {
		HttpStatus status = HttpStatus.BAD_REQUEST;

		ValidationError err = ValidationError.standardBuilder()
				.timestamp(Instant.now())
				.status(status.value())
				.title("Validation exception")
				.detail("Erros na entrada das informações, consulte as mensagens para mais detalhes")
				.path(request.getRequestURI())
				.build();

		for (Message message : e.getMessages()) {
			err.addError(message.getTarget(), message.getDescription());
		}

		return ResponseEntity.status(status).body(err);
	}

	@ExceptionHandler(HttpMediaTypeNotSupportedException.class)
	public ResponseEntity<StandardError> contentType(HttpMediaTypeNotSupportedException e, HttpServletRequest request) {
		HttpStatus status = HttpStatus.UNSUPPORTED_MEDIA_TYPE;
		return ResponseEntity.status(status).body(createStandardError(
				status,
				"Content type not supported",
				String.format("'%s' media type is not supported", e.getContentType().getSubtype().toString().toUpperCase()),
				request.getRequestURI()));
	}

	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	public ResponseEntity<StandardError> methodNotSupported(HttpRequestMethodNotSupportedException e, HttpServletRequest request) {
		HttpStatus status = HttpStatus.METHOD_NOT_ALLOWED;
		return ResponseEntity.status(status).body(createStandardError(
				status,
				"Method Not Allowed",
				String.format("Request method '%s' not supported", e.getMethod().toString()),
				request.getRequestURI()));
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<StandardError> messageNotReadable(HttpMessageNotReadableException e, HttpServletRequest request) {
		HttpStatus status = HttpStatus.BAD_REQUEST;
		var detail = "The body passed in the request has a syntax error";

		if (e.getCause() instanceof InvalidFormatException) {
			var args = new Object[] {
					((InvalidFormatException) e.getCause()).getValue(),
					((InvalidFormatException) e.getCause()).getTargetType().getSimpleName(),
					messageSource.getMessage(
							((InvalidFormatException) e.getCause()).getPath().get(0).getFieldName(),
							null,
							LocaleContextHolder.getLocale())
					};
			detail = messageSource.getMessage(
					"InvalidFormatException",
					args,
					LocaleContextHolder.getLocale());
		}

		return ResponseEntity.status(status).body(createStandardError(
				status,
				"Content parse error",
				detail,
				request.getRequestURI()));
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<StandardError> methodArgumentTypeMismatch(MethodArgumentTypeMismatchException e, HttpServletRequest request) {
		HttpStatus status = HttpStatus.BAD_REQUEST;
		return ResponseEntity.status(status).body(createStandardError(
				status,
				"Method Not Allowed",
				String.format("The param '%s' informed in the URL is not compatible with '%s'",
						e.getValue(),
						e.getRequiredType().getSimpleName().toString()),
				request.getRequestURI()));
	}

	@ExceptionHandler(PropertyReferenceException.class)
	public ResponseEntity<StandardError> methodArgumentTypeMismatch(PropertyReferenceException e, HttpServletRequest request) {
		HttpStatus status = HttpStatus.BAD_REQUEST;
		var args = new Object[] {
				messageSource.getMessage(
						e.getPropertyName(),
						null,
						LocaleContextHolder.getLocale()),
				messageSource.getMessage(
						e.getType().getType().getSimpleName(),
						null,
						LocaleContextHolder.getLocale())
		};
		return ResponseEntity.status(status).body(createStandardError(
				status,
				"Method Not Allowed",
				messageSource.getMessage(
						"PropertyReferenceException",
						args,
						LocaleContextHolder.getLocale()),
				request.getRequestURI()));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ValidationError> validation(MethodArgumentNotValidException e, HttpServletRequest request) {
		HttpStatus status = HttpStatus.UNPROCESSABLE_ENTITY;
		var title = messageSource.getMessage("error.title_validation_errors", null, LocaleContextHolder.getLocale());
		var detail = messageSource.getMessage("error.detail_validation_errors", null, LocaleContextHolder.getLocale());
		
		ValidationError err = ValidationError.standardBuilder()
				.timestamp(Instant.now())
				.status(status.value())
				.title(title)
				.detail(detail)
				.path(request.getRequestURI())
				.build();

		for (ObjectError objectError : e.getBindingResult().getAllErrors()) {
			String fieldName;
			String message;
	
			if (objectError instanceof FieldError fieldError) {
				// Tenta obter o nome do campo traduzido, se houver
				fieldName = messageSource.getMessage(fieldError.getField(), null, LocaleContextHolder.getLocale());
				
				// Obtém a mensagem de erro e substitui corretamente o placeholder {field}
				message = messageSource.getMessage(fieldError, LocaleContextHolder.getLocale());
				message = message.replace("{field}", fieldName);
			} else {
				fieldName = messageSource.getMessage(objectError.getObjectName(), null, LocaleContextHolder.getLocale());
				message = messageSource.getMessage(objectError, LocaleContextHolder.getLocale());
			}
	
			err.addError(fieldName, message);
		}
		
		return ResponseEntity.status(status).body(err);
	}


	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<ValidationError> validation(ConstraintViolationException e, HttpServletRequest request) {
		HttpStatus status = HttpStatus.UNPROCESSABLE_ENTITY;

		ValidationError err = ValidationError.standardBuilder()
				.timestamp(Instant.now())
				.status(status.value())
				.title("Validation exception")
				.detail("Erros na entrada das informações, consulte as mensagens para mais detalhes")
				.path(request.getRequestURI())
				.build();

		for (var f : e.getConstraintViolations()) {

			if (f.getMessage().startsWith("@{")) {
				err.addError(f.getPropertyPath().toString(),
						messageSource.getMessage(
								f.getMessage()
										.replace("@{","")
										.replace("}",""),
								f.getExecutableParameters(),
								LocaleContextHolder.getLocale())
				);
			} else {
				err.addError(f.getPropertyPath().toString(),f.getMessage());
			}
		}

		return ResponseEntity.status(status).body(err);
	}

	private StandardError createStandardError(HttpStatus status, String title, String detail, String path) {
		return StandardError.builder()
				.timestamp(Instant.now())
				.status(status.value())
				.title(title)
				.detail(detail)
				.path(path).build();
	}
}
