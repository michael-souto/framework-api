package com.detrasoft.framework.api.controllers.exceptions;

import com.detrasoft.framework.core.notification.Message;
import com.detrasoft.framework.crud.services.exceptions.DatabaseException;
import com.detrasoft.framework.crud.services.exceptions.EntityValidationException;
import com.detrasoft.framework.crud.services.exceptions.ResourceNotFoundException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("null")
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
	public ResponseEntity<StandardError> entityValidation(EntityValidationException e, HttpServletRequest request) {
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

	@ExceptionHandler(GenericValidationException.class)
	public ResponseEntity<StandardError> genericValidation(GenericValidationException e, HttpServletRequest request) {
		HttpStatus status = HttpStatus.BAD_REQUEST;
		ValidationError err = ValidationError.standardBuilder()
				.timestamp(Instant.now())
				.status(status.value())
				.title("Validation exception")
				.detail(e.getMessage())
				.path(request.getRequestURI())
				.build();

		return ResponseEntity.status(status).body(err);
	}

	@ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> contentType(
            HttpMediaTypeNotSupportedException e) {
        
        Map<String, Object> error = new HashMap<>();
        error.put("timestamp", Instant.now().toString());
        error.put("status", HttpStatus.UNSUPPORTED_MEDIA_TYPE.value());
        error.put("error", "Unsupported Media Type");
        
        var contentType = e.getContentType();
        if (contentType != null) {
            error.put("message", String.format(
                "Tipo de conteúdo '%s' não é suportado. Tipos suportados: %s",
                contentType.getSubtype(),
                e.getSupportedMediaTypes()
            ));
        } else {
            error.put("message", String.format(
                "Tipo de conteúdo não especificado ou inválido. Tipos suportados: %s",
                e.getSupportedMediaTypes()
            ));
        }
        
        error.put("supportedMediaTypes", e.getSupportedMediaTypes());
        
        return ResponseEntity
            .status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
            .contentType(MediaType.APPLICATION_JSON)
            .body(error);
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
	public ResponseEntity<?> messageNotReadable(HttpMessageNotReadableException e, HttpServletRequest request) {
		HttpStatus status = HttpStatus.BAD_REQUEST;
		var locale = LocaleContextHolder.getLocale();

		// Percorre a cadeia de causas (exceções podem estar aninhadas)
		InvalidFormatException ife = findCauseOfType(e, InvalidFormatException.class);
		if (ife != null) {
			// Build the full nested field path from Jackson's reference chain
			// e.g. "patient.healthProfile.allergies[0].severity"
			String fieldPath = buildFieldPath(ife.getPath());

			// Extract the leaf field name for translation
			String leafFieldName = extractLeafFieldName(fieldPath);
			String translatedFieldName = messageSource.getMessage(leafFieldName, null, leafFieldName, locale);

			String message;
			if (ife.getTargetType().isEnum()) {
				// For enum types, list the valid values
				String validValues = Arrays.stream(ife.getTargetType().getEnumConstants())
						.map(Object::toString)
						.collect(Collectors.joining(", "));
				var args = new Object[] { ife.getValue(), translatedFieldName, validValues };
				message = messageSource.getMessage(
						"error.invalid_enum_value",
						args,
						String.format("O valor \"%s\" não é válido para o campo \"%s\". Valores aceitos: [%s]",
								ife.getValue(), translatedFieldName, validValues),
						locale);
			} else {
				var args = new Object[] { ife.getValue(), translatedFieldName };
				message = messageSource.getMessage(
						"error.invalid_format_exception",
						args,
						locale);
			}

			return buildValidationErrorResponse(fieldPath, message, request, locale);
		}

		com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException upe = findCauseOfType(e, com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException.class);
		if (upe != null) {
			String fieldPath = buildFieldPath(upe.getPath());
			String leafFieldName = extractLeafFieldName(fieldPath);
			String translatedFieldName = messageSource.getMessage(leafFieldName, null, leafFieldName, locale);
			var args = new Object[] { translatedFieldName };
			String message = messageSource.getMessage(
					"error.unrecognized_property",
					args,
					String.format("O campo \"%s\" não é reconhecido", translatedFieldName),
					locale);
			return buildValidationErrorResponse(fieldPath, message, request, locale);
		}

		com.fasterxml.jackson.databind.exc.MismatchedInputException mie = findCauseOfType(e, com.fasterxml.jackson.databind.exc.MismatchedInputException.class);
		if (mie != null) {
			String fieldPath = buildFieldPath(mie.getPath());

			if (!fieldPath.isEmpty()) {
				String leafFieldName = extractLeafFieldName(fieldPath);
				String translatedFieldName = messageSource.getMessage(leafFieldName, null, leafFieldName, locale);
				String targetTypeName = mie.getTargetType() != null ? mie.getTargetType().getSimpleName() : "desconhecido";

				String message;
				if (mie.getTargetType() != null && mie.getTargetType().isEnum()) {
					String validValues = Arrays.stream(mie.getTargetType().getEnumConstants())
							.map(Object::toString)
							.collect(Collectors.joining(", "));
					var args = new Object[] { "", translatedFieldName, validValues };
					message = messageSource.getMessage(
							"error.invalid_enum_value",
							args,
							String.format("O valor informado não é válido para o campo \"%s\". Valores aceitos: [%s]",
									translatedFieldName, validValues),
							locale);
				} else {
					var args = new Object[] { translatedFieldName, targetTypeName };
					message = messageSource.getMessage(
							"error.invalid_format_exception",
							args,
							String.format("O valor informado não é compatível para o campo \"%s\"", translatedFieldName),
							locale);
				}

				return buildValidationErrorResponse(fieldPath, message, request, locale);
			}
		}

		// Fallback: tenta extrair informação útil da mensagem original (ex: enum inválido)
		String detail = extractReadableDetailFromCause(e.getCause(), locale);

		return ResponseEntity.status(status).body(createStandardError(
				status,
				messageSource.getMessage("error.content_parse_error", null, "Erro ao analisar o conteúdo", locale),
				detail,
				request.getRequestURI()));
	}

	/**
	 * Percorre a cadeia de causas para encontrar uma exceção do tipo especificado.
	 */
	@SuppressWarnings("unchecked")
	private <T extends Throwable> T findCauseOfType(Throwable ex, Class<T> type) {
		Throwable cause = ex;
		while (cause != null) {
			if (type.isInstance(cause)) {
				return (T) cause;
			}
			cause = cause.getCause();
		}
		return null;
	}

	/**
	 * Padrões para extrair detalhes da mensagem do Jackson em caso de enum inválido.
	 * Formato: "Cannot deserialize value of type `X` from String \"Y\": not one of the values accepted for Enum class: [A, B, C]"
	 */
	private static final Pattern ENUM_INVALID_VALUE_PATTERN = Pattern.compile("from String \"([^\"]+)\"");
	private static final Pattern ENUM_VALID_VALUES_PATTERN = Pattern.compile("Enum class: \\[([^\\]]+)\\]");
	private static final Pattern ENUM_TYPE_PATTERN = Pattern.compile("value of type `([^`]+)`");

	/**
	 * Extrai uma mensagem legível da causa da exceção, incluindo casos de enum inválido
	 * que podem não ter sido capturados como InvalidFormatException.
	 */
	private String extractReadableDetailFromCause(Throwable cause, java.util.Locale locale) {
		if (cause == null) {
			return messageSource.getMessage("error.syntax_error_body", null,
					"O corpo da solicitação possui um erro de sintaxe", locale);
		}
		String msg = cause.getMessage();
		if (msg != null && msg.contains("not one of the values accepted for Enum")) {
			String invalidValue = extractGroup(msg, ENUM_INVALID_VALUE_PATTERN);
			String validValues = extractGroup(msg, ENUM_VALID_VALUES_PATTERN);
			String enumType = extractEnumSimpleName(msg);

			if (invalidValue != null && validValues != null) {
				String fieldLabel = enumType != null
						? messageSource.getMessage(enumType, null, enumType, locale)
						: "o campo";
				var args = new Object[] { invalidValue, fieldLabel, validValues };
				return messageSource.getMessage("error.invalid_enum_with_details", args,
						String.format("O valor \"%s\" não é válido para %s. Valores aceitos: [%s]",
								invalidValue, fieldLabel, validValues),
						locale);
			}
			return messageSource.getMessage("error.invalid_enum_generic", null,
					"Um valor de enum inválido foi informado. Verifique os valores aceitos para o campo.", locale);
		}
		if (msg != null && msg.contains("Cannot deserialize")) {
			String invalidValue = extractGroup(msg, ENUM_INVALID_VALUE_PATTERN);
			if (invalidValue != null) {
				var args = new Object[] { invalidValue };
				return messageSource.getMessage("error.invalid_value_with_details", args,
						String.format("O valor \"%s\" é incompatível com o tipo esperado.", invalidValue),
						locale);
			}
			return messageSource.getMessage("error.invalid_value_generic", null,
					"Um valor incompatível foi informado no corpo da solicitação. Verifique o formato dos dados.", locale);
		}
		if (msg != null && (msg.contains("Unexpected character") || msg.contains("Unrecognized token"))) {
			return messageSource.getMessage("error.syntax_error_body", null,
					"O corpo da solicitação possui um erro de sintaxe", locale);
		}
		// Mensagem genérica como último recurso
		return messageSource.getMessage("error.syntax_error_body", null,
				"O corpo da solicitação possui um erro de sintaxe", locale);
	}

	private String extractGroup(String message, Pattern pattern) {
		Matcher m = pattern.matcher(message);
		return m.find() ? m.group(1).trim() : null;
	}

	private String extractEnumSimpleName(String message) {
		String fullType = extractGroup(message, ENUM_TYPE_PATTERN);
		if (fullType == null) return null;
		int lastDot = fullType.lastIndexOf('.');
		return lastDot >= 0 ? fullType.substring(lastDot + 1) : fullType;
	}

	private ResponseEntity<ValidationError> buildValidationErrorResponse(String fieldPath, String message, HttpServletRequest request, java.util.Locale locale) {
		var title = messageSource.getMessage("error.title_validation_errors", null, "Preenchimento incorreto", locale);
		var detail = messageSource.getMessage("error.detail_validation_errors", null, locale);

		ValidationError err = ValidationError.standardBuilder()
				.timestamp(Instant.now())
				.status(HttpStatus.UNPROCESSABLE_ENTITY.value())
				.title(title)
				.detail(detail)
				.path(request.getRequestURI())
				.build();

		err.addError(fieldPath, message);
		return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(err);
	}

	private String extractLeafFieldName(String fieldPath) {
		String leafFieldName = fieldPath;
		if (fieldPath.contains(".")) {
			leafFieldName = fieldPath.substring(fieldPath.lastIndexOf('.') + 1);
		}
		return leafFieldName.replaceAll("\\[\\d+\\]", "");
	}

	/**
	 * Builds the full nested field path from Jackson's JsonMappingException reference chain.
	 * e.g. [Reference(patient), Reference(healthProfile), Reference(allergies, index=0), Reference(severity)]
	 * becomes "patient.healthProfile.allergies[0].severity"
	 */
	private String buildFieldPath(java.util.List<JsonMappingException.Reference> path) {
		var sb = new StringBuilder();
		for (var ref : path) {
			if (ref.getFieldName() != null) {
				if (sb.length() > 0) sb.append('.');
				sb.append(ref.getFieldName());
			} else if (ref.getIndex() >= 0) {
				sb.append('[').append(ref.getIndex()).append(']');
			}
		}
		return sb.toString();
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
		var title = messageSource.getMessage("error.title_validation_errors", null, LocaleContextHolder.getLocale());
		var detail = messageSource.getMessage("error.detail_validation_errors", null, LocaleContextHolder.getLocale());

		ValidationError err = ValidationError.standardBuilder()
				.timestamp(Instant.now())
				.status(status.value())
				.title(title)
				.detail(detail)
				.path(request.getRequestURI())
				.build();

		for (var f : e.getConstraintViolations()) {
			String propertyPath = f.getPropertyPath().toString();

			// Extract the leaf field name from the property path
			// e.g. "insert.entity.addresses[0].customData" -> "customData"
			String fieldName = propertyPath;
			if (propertyPath.contains(".")) {
				String lastSegment = propertyPath.substring(propertyPath.lastIndexOf('.') + 1);
				// Remove array index notation if present, e.g. "addresses[0]" -> "addresses"
				fieldName = lastSegment.replaceAll("\\[\\d+\\]", "");
			}

			// Translate the field name using messageSource (e.g. "countryCode" -> "Código do país")
			// Falls back to the raw field name if no translation is found
			String translatedFieldName = messageSource.getMessage(fieldName, null, fieldName, LocaleContextHolder.getLocale());

			String message;
			if (f.getMessage().startsWith("@{")) {
				message = messageSource.getMessage(
						f.getMessage()
								.replace("@{","")
								.replace("}",""),
						f.getExecutableParameters(),
						LocaleContextHolder.getLocale());
			} else {
				message = f.getMessage();
			}

			// Replace the {field} placeholder with the translated field name
			message = message.replace("{field}", translatedFieldName);

			err.addError(propertyPath, message);
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
