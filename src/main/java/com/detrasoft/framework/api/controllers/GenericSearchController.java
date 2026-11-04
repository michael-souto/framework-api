package com.detrasoft.framework.api.controllers;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.detrasoft.framework.api.dto.SearchFieldDTO;
import com.detrasoft.framework.api.dto.SearchReponseDTO;
import com.detrasoft.framework.api.search.SearchSchemaResolver;
import com.detrasoft.framework.api.search.SearchScope;
import com.detrasoft.framework.api.search.SearchScopeProvider;
import com.detrasoft.framework.api.search.SearchUnionBuilder;
import com.detrasoft.framework.core.context.GenericContext;
import com.detrasoft.framework.core.resource.Translator;
import com.detrasoft.framework.crud.entities.FieldType;
import com.detrasoft.framework.crud.entities.SearchConfiguration;
import com.detrasoft.framework.crud.entities.SearchField;
import com.detrasoft.framework.crud.repositories.SearchRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.core.io.Resource;

@RestController
@RequestMapping("/search")
public class GenericSearchController {

	private static final Logger log = LoggerFactory.getLogger(GenericSearchController.class);

	/** Schemas de tenant sao numericos (detrasoftId); qualquer outro valor e descartado. */
	private static final Pattern SCHEMA_PATTERN = Pattern.compile("^\\d{1,19}$");

	/** ":nome" sem pegar casts "::tipo"; o nome termina na fronteira de palavra. */
	private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("(?<![:\\w]):([A-Za-z_][A-Za-z0-9_]*)\\b");

	/** Preenchidos a partir do token em applyContextPlaceholders; nunca pelo cliente. */
	private static final Set<String> CONTEXT_PLACEHOLDERS = Set.of(
			"user_id", "software", "detrasoft_id", "timezone_offset", "current_datetime", "current_date");

	private List<SearchConfiguration> searchConfigurations = new ArrayList<>();

	public GenericSearchController() {
		loadAllSearchConfigurations();
	}

	@Autowired
	SearchRepository searchRepository;

	/** Opcional: schemas extras mesclados via UNION ALL (ex: workspaces compartilhados). */
	@Autowired
	ObjectProvider<SearchScopeProvider> scopeProviders;

	/** Opcional: schema corrente para :detrasoft_id (padrao: claim do token). */
	@Autowired
	ObjectProvider<SearchSchemaResolver> schemaResolver;

	private SearchConfiguration getSearchConfigurationById(String id) {
		Optional<SearchConfiguration> config = searchConfigurations.stream()
				.filter(c -> c.getId().equalsIgnoreCase(id))
				.findFirst();
		if (config.isPresent()) {
			return config.get();
		} else {
			return null;
		}
	}

	@GetMapping(value = "/{id}/columns")
	public ResponseEntity<SearchReponseDTO> getSchema(@PathVariable String id) {
		SearchConfiguration config = getSearchConfigurationById(id);
		if (config == null) {
			return ResponseEntity.notFound().build();
		}
		SearchReponseDTO response = new SearchReponseDTO();
		response.setColumns(convertColumnsToDTO(config.getColumns()));
		response.setTitle(Translator.getTranslatedText(config.getTitle()));

		return ResponseEntity.ok(response);
	}

	@GetMapping(value = "/{id}")
	public ResponseEntity<SearchReponseDTO> search(@PathVariable String id,
			@RequestParam Map<String, String> queryParams,
			@RequestParam(name = "unpaged", required = false) boolean unpaged, Pageable pageable) {

		SearchConfiguration config = getSearchConfigurationById(id);
		if (config == null) {
			return ResponseEntity.notFound().build();
		}

		List<Map<String, Object>> resultList = new ArrayList<>();
		List<SearchField> columns = config.getColumns();
		String title = Translator.getTranslatedText(config.getTitle());
		String from = config.getFrom();
		String where = config.getWhere();
		String groupBy = config.getGroupBy();
		String orderBy = config.getOrderBy();

		SearchReponseDTO response = new SearchReponseDTO();
		resultList = new ArrayList<>();

		String anyValue = queryParams.get("any");

		// Converte os query parameters em uma lista de SearchFields
		List<SearchField> searchFields = new ArrayList<>();
		Map<String, String> customSearchFields = new HashMap<>();
		queryParams.forEach((key, value) -> {
			if ("page".equals(key) || "size".equals(key) || "sort".equals(key) || "unpaged".equals(key)
					|| "any".equals(key)) {
				return;
			}
			// Procura a coluna correspondente na configuração
			Optional<SearchField> matchingColumn = columns.stream()
					.filter(column -> column.getField().equalsIgnoreCase(key))
					.findFirst();
			if (matchingColumn.isPresent()) {
				SearchField searchField = new SearchField();
				searchField.setField(key);
				searchField.setValue(value);
				searchField.setType(matchingColumn.get().getType());
				searchField.setColumnName(matchingColumn.get().getColumnName());
				searchField.setWhere(matchingColumn.get().getWhere());
				searchField.setPrincipal(matchingColumn.get().isPrincipal());
				searchField.setKey(matchingColumn.get().isKey());
				searchField.setHidden(matchingColumn.get().isHidden());
				searchFields.add(searchField);
			} else {
				customSearchFields.put(key, value);
			}
		});

		if (customSearchFields.size() > 0 && where != null) {
			where = applyCustomPlaceholders(where, customSearchFields);
		}

		String select = buildSelectSQL(columns);
		String body = buildBodySQL(searchFields, columns, from, where, groupBy, anyValue);
		String homeSchema = resolveCurrentSchema();
		String homeQuery = applyContextPlaceholders(select + body + orderBySQL(orderBy), homeSchema);

		List<SearchScope> extraScopes = extraScopes(id, homeSchema);
		boolean sumMode = "sum".equalsIgnoreCase(config.getMergeMode());
		boolean merged = !extraScopes.isEmpty();
		String query = merged
				? buildMergedQuery(select, body, orderBy, columns, homeSchema, extraScopes, sumMode)
				: homeQuery;

		List<Object[]> resultSQL;
		int totalRecords;
		try {
			resultSQL = runQuery(query, unpaged, pageable);
			totalRecords = searchRepository.countNativeSQL(query);
		} catch (RuntimeException e) {
			if (!merged) {
				throw e;
			}
			// Um schema externo com problema nao pode derrubar a listagem do proprio workspace.
			log.warn("Search '{}' mesclada falhou; usando apenas o schema {}: {}", id, homeSchema, e.getMessage());
			merged = false;
			query = homeQuery;
			resultSQL = runQuery(query, unpaged, pageable);
			totalRecords = searchRepository.countNativeSQL(query);
		}

		boolean unionRows = merged && !sumMode;
		for (Object[] row : resultSQL) {
			Map<String, Object> rowMap = new HashMap<>();
			for (int i = 0; i < columns.size(); i++) {
				SearchField column = columns.get(i);
				// SUM(bigint) volta como BigDecimal; mantem o mesmo tipo da query sem mescla
				rowMap.put(column.getField(), merged && sumMode ? normalizeNumber(row[i]) : row[i]);
			}
			if (unionRows) {
				int scope = ((Number) row[row.length - 1]).intValue();
				if (scope > 0 && scope <= extraScopes.size()) {
					extraScopes.get(scope - 1).attributes().forEach(rowMap::putIfAbsent);
				}
			}
			resultList.add(rowMap);
		}

		response.setTotalRecords(totalRecords);
		Page<?> resultListPage = new PageImpl<>(resultList, pageable, response.getTotalRecords());

		response.setTitle(title);
		response.setColumns(convertColumnsToDTO(columns));
		response.setData(resultListPage);

		return ResponseEntity.ok(response);
	}

	/**
	 * Substitui placeholders ":chave" do where pelos query params do cliente.
	 *
	 * - Chaves de contexto (:user_id, :detrasoft_id...) sao reservadas: vem do
	 *   token em applyContextPlaceholders e o cliente nao pode sobrescreve-las
	 *   (antes "?detrasoft_id=5" trocava o schema das subqueries).
	 * - Match com fronteira de palavra: ":user" nao corrompe ":user_id".
	 * - Dentro de literal ('...') o valor e escapado; fora dele vira literal
	 *   entre aspas. O Postgres converte o literal para o tipo da coluna, entao
	 *   comparacoes com numero/uuid/data continuam funcionando.
	 */
	private String applyCustomPlaceholders(String where, Map<String, String> customSearchFields) {
		Matcher matcher = PLACEHOLDER_PATTERN.matcher(where);
		StringBuilder result = new StringBuilder();
		int last = 0;
		while (matcher.find()) {
			String key = matcher.group(1);
			String rawValue = customSearchFields.get(key);
			if (rawValue == null || CONTEXT_PLACEHOLDERS.contains(key)) {
				continue;
			}
			result.append(where, last, matcher.start());
			String value = normalizeDateValue(rawValue);
			if (isInsideStringLiteral(where, matcher.start())) {
				result.append(escapeLiteral(value));
			} else {
				result.append(quoteLiteral(value));
			}
			last = matcher.end();
		}
		result.append(where.substring(last));
		return result.toString();
	}

	// data ou datetime ISO-8601 (ex: 2025-05-01 ou 2025-05-01T15:30:00) -> formato canonico
	private String normalizeDateValue(String rawValue) {
		if (rawValue.matches("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}$")) {
			LocalDateTime ldt = LocalDateTime.parse(rawValue, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
			return ldt.atZone(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
		}
		if (rawValue.matches("^\\d{4}-\\d{2}-\\d{2}$")) {
			return LocalDate.parse(rawValue, DateTimeFormatter.ISO_LOCAL_DATE).format(DateTimeFormatter.ISO_LOCAL_DATE);
		}
		return rawValue;
	}

	/** Paridade de aspas simples antes da posicao ('' escapado conta 2, mantendo a paridade). */
	private static boolean isInsideStringLiteral(String sql, int position) {
		int quotes = 0;
		for (int i = 0; i < position; i++) {
			if (sql.charAt(i) == '\'') {
				quotes++;
			}
		}
		return quotes % 2 == 1;
	}

	/**
	 * Escapa um valor para uso DENTRO de um literal SQL ('...'). Pressupoe
	 * standard_conforming_strings=on (padrao do PostgreSQL desde 9.1), em que a
	 * barra invertida nao e caractere de escape em literais comuns.
	 */
	static String escapeLiteral(String value) {
		if (value == null) {
			return "";
		}
		return value.replace("\u0000", "").replace("'", "''");
	}

	static String quoteLiteral(String value) {
		return "'" + escapeLiteral(value) + "'";
	}

	/**
	 * Schema corrente para :detrasoft_id. Com SearchSchemaResolver (ex: tenant
	 * validado do header) usa-o; sem ele, ou com valor invalido, usa a claim.
	 */
	private String resolveCurrentSchema() {
		SearchSchemaResolver resolver = schemaResolver != null ? schemaResolver.getIfAvailable() : null;
		if (resolver != null) {
			String schema = resolver.currentSchema();
			if (schema != null && SCHEMA_PATTERN.matcher(schema).matches()) {
				return schema;
			}
		}
		return GenericContext.getContexts("detrasoftId");
	}

	/** Schemas extras validos, sem repetir e sem o schema corrente. Falha do provider = sem extras. */
	private List<SearchScope> extraScopes(String searchId, String homeSchema) {
		if (scopeProviders == null || homeSchema == null) {
			return List.of();
		}
		Map<String, SearchScope> scopes = new LinkedHashMap<>();
		try {
			scopeProviders.orderedStream().forEach(provider -> {
				List<SearchScope> provided = provider.extraScopes(searchId);
				if (provided == null) {
					return;
				}
				for (SearchScope scope : provided) {
					if (scope != null && scope.schema() != null
							&& SCHEMA_PATTERN.matcher(scope.schema()).matches()
							&& !scope.schema().equals(homeSchema)) {
						scopes.putIfAbsent(scope.schema(), scope);
					}
				}
			});
		} catch (RuntimeException e) {
			log.warn("SearchScopeProvider falhou para a search '{}': {}", searchId, e.getMessage());
			return List.of();
		}
		return new ArrayList<>(scopes.values());
	}

	/** Ramo 0 = schema corrente; ramo i = extraScopes[i-1]. */
	private String buildMergedQuery(String select, String body, String orderBy, List<SearchField> columns,
			String homeSchema, List<SearchScope> extraScopes, boolean sumMode) {
		List<SearchUnionBuilder.OrderItem> order = sumMode ? List.of() : SearchUnionBuilder.parseOrderBy(orderBy);
		List<String> branches = new ArrayList<>();
		branches.add(applyContextPlaceholders(SearchUnionBuilder.branch(select, body, order, 0), homeSchema));
		for (int i = 0; i < extraScopes.size(); i++) {
			branches.add(applyContextPlaceholders(
					SearchUnionBuilder.branch(select, body, order, i + 1), extraScopes.get(i).schema()));
		}
		if (sumMode) {
			return SearchUnionBuilder.sum(branches, columns.stream().map(SearchField::getType).toList());
		}
		return SearchUnionBuilder.union(branches, order);
	}

	private List<Object[]> runQuery(String query, boolean unpaged, Pageable pageable) {
		return unpaged ? searchRepository.findNativeSQL(query) : searchRepository.findNativeSQL(query, pageable);
	}

	private static Object normalizeNumber(Object value) {
		if (value instanceof BigDecimal decimal && decimal.stripTrailingZeros().scale() <= 0) {
			try {
				return decimal.longValueExact();
			} catch (ArithmeticException e) {
				return value;
			}
		}
		return value;
	}

	private String applyContextPlaceholders(String query, String schema) {
		if (query == null) {
			return null;
		}

		String userId = GenericContext.getContexts("userId");
		query = query.replace(":user_id", userId != null ? userId : "");

		String software = GenericContext.getContexts("software");
		query = query.replace(":software", software != null ? software : "");

		query = query.replace(":detrasoft_id", schema != null ? schema : "");

		String timezoneOffset = GenericContext.getContexts("timezoneOffset");
		ZoneOffset zoneOffset;
		if (timezoneOffset != null && !timezoneOffset.trim().isEmpty()) {
			try {
				zoneOffset = ZoneOffset.of(timezoneOffset.trim());
			} catch (Exception e) {
				zoneOffset = ZoneOffset.UTC;
				timezoneOffset = "+00:00";
			}
		} else {
			zoneOffset = ZoneOffset.UTC;
			timezoneOffset = "+00:00";
		}

		query = query.replace(":timezone_offset", timezoneOffset);
		query = query.replace(":current_datetime",
				LocalDateTime.now(zoneOffset).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
		query = query.replace(":current_date", LocalDate.now(zoneOffset).format(DateTimeFormatter.ISO_LOCAL_DATE));

		return query;
	}

	private static String buildSelectSQL(List<SearchField> columns) {
		String selectSQL = "SELECT ";
		for (int i = 0; i < columns.size(); i++) {
			selectSQL = selectSQL + columns.get(i).getColumnName() + " AS C" + i + " ";
			if (!(i == columns.size() - 1)) {
				selectSQL = selectSQL + ", ";
			}
		}
		return selectSQL;
	}

	private static String orderBySQL(String orderBy) {
		return orderBy != null && !orderBy.isBlank() ? " ORDER BY " + orderBy : "";
	}

	/** FROM + WHERE + GROUP BY (sem ORDER BY, que depende de a query ser mesclada ou nao). */
	private String buildBodySQL(List<SearchField> searchFields, List<SearchField> columns, String from,
			String where, String groupBy, String anyValue) {
		String fromSQL = " FROM " + from;

		// Parenteses: um "a OR b" do JSON nao pode engolir os filtros do cliente
		String whereSQL = (where != null && !where.isBlank() ? "(" + where + ") AND " : "");

		for (int i = 0; i < searchFields.size(); i++) {
			if ((searchFields.get(i).getValue() != null) && !(searchFields.get(i).getValue().equals(""))) {

				String columnName = searchFields.get(i).getWhere() != null ? searchFields.get(i).getWhere()
						: searchFields.get(i).getColumnName();
				if (columnName == null || columnName.equals("")) {
					for (SearchField column : columns) {
						if (column.getField().equals(searchFields.get(i).getField())) {
							columnName = column.getColumnName();
						}
					}
				}
				// Todo valor do cliente entra como literal escapado (escapeLiteral/quoteLiteral).
				// STRING
				if (searchFields.get(i).getType().equals(FieldType.string)) {
					String value = escapeLiteral(searchFields.get(i).getValue().toString()).replace(" ", "%");
					whereSQL = whereSQL + columnName + " ILIKE ('%" + value + "%')";
				} // DATE
				else if (searchFields.get(i).getType().equals(FieldType.date)) {
					String value = searchFields.get(i).getValue().toString();
					value = value.length() > 10 ? value.substring(0, 10) : value;
					whereSQL = whereSQL + "date_trunc('day'," + columnName + ") = " + quoteLiteral(value);
				} // RANGE of DATE
				else if (searchFields.get(i).getType().equals(FieldType.rangedate)) {
					List<String> valueList = Arrays.asList(searchFields.get(i).getValue().toString().split(";"));
					valueList.removeIf(d -> d == null);
					DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
					if (valueList.size() == 2) {
						whereSQL = whereSQL +
								"date_trunc('day'," + columnName + ")"
								+ " BETWEEN date_trunc('day', TIMESTAMP WITH TIME ZONE " + quoteLiteral(valueList.get(0))
								+ ") AND date_trunc('day', TIMESTAMP WITH TIME ZONE " + quoteLiteral(valueList.get(1)) + ")";

					} else if (valueList.size() == 1) {
						ZonedDateTime date = ZonedDateTime.parse(valueList.get(0), DateTimeFormatter.ISO_DATE_TIME);
						String dateFormatted = date.format(formatter);
						whereSQL = whereSQL + "date_trunc('day'," + columnName + ") = '" + dateFormatted + "'";
					}
				} // RANGE of DATETIME
				else if (searchFields.get(i).getType().equals(FieldType.rangedatetime)) {
					DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");

					if (searchFields.get(i).getValue().toString().indexOf("-to-") > 0
							&& searchFields.get(i).getValue().toString().split("-to-").length == 2) {
						List<String> valueList = Arrays.asList(searchFields.get(i).getValue().toString().split("-to-"));
						valueList.removeIf(d -> d == null);
						whereSQL = whereSQL + " ( " + columnName + " ) BETWEEN " + quoteLiteral(valueList.get(0))
								+ "::timestamptz AND " + quoteLiteral(valueList.get(1)) + "::timestamptz ";

					} else {
						List<String> valueList = Arrays.asList(searchFields.get(i).getValue().toString().split(","));
						valueList.removeIf(d -> d == null);
						for (String d : valueList) {
							ZonedDateTime date = ZonedDateTime.parse(d, DateTimeFormatter.ISO_DATE_TIME);
							String dateFormatted = date.format(formatter);
							whereSQL = whereSQL + "date_trunc('day'," + columnName + ") = '" + dateFormatted + "'";
						}
					}
				}
				// CURRENCY
				else if (searchFields.get(i).getType().equals(FieldType.currency)) {
					String value = searchFields.get(i).getValue().toString().replace(" ", "%");
					whereSQL = whereSQL + columnName + " = " + quoteLiteral(value.replace(',', '.'));

				} else if (searchFields.get(i).getType().equals(FieldType.guid)) {
					whereSQL = whereSQL + columnName + " = " + quoteLiteral(searchFields.get(i).getValue().toString());
				} else {
					// number/bool/etc.: literal entre aspas e convertido pelo Postgres para o tipo da coluna
					whereSQL = whereSQL + columnName + " = " + quoteLiteral(searchFields.get(i).getValue().toString());
				}

				whereSQL = whereSQL + " AND ";

			}
		}

		String anyClause = buildAnyClause(columns, anyValue);
		if (!anyClause.isEmpty()) {
			whereSQL = whereSQL + "(" + anyClause + ") AND ";
		}

		if (whereSQL.length() > 5 && whereSQL.substring(whereSQL.length() - 5, whereSQL.length()).equals(" AND ")) {
			whereSQL = whereSQL.substring(0, whereSQL.length() - 5);
		}
		whereSQL = (!whereSQL.equals("") ? " WHERE " + whereSQL : "");

		String groupBySQL = groupBy != null && !groupBy.isBlank() ? " GROUP BY " + groupBy : "";

		return fromSQL + whereSQL + groupBySQL;
	}

	private String buildAnyClause(List<SearchField> columns, String anyValue) {
		if (anyValue == null || anyValue.isEmpty()) {
			return "";
		}
		String value = escapeLiteral(anyValue).replace(" ", "%");
		List<String> conditions = new ArrayList<>();
		for (SearchField column : columns) {
			if (FieldType.string.equals(column.getType()) && !column.isHidden()
					&& column.getColumnName() != null && !column.getColumnName().isEmpty()) {
				conditions.add("CAST(" + column.getColumnName() + " AS TEXT) ILIKE ('%" + value + "%')");
			}
		}
		return String.join(" OR ", conditions);
	}

	private void loadAllSearchConfigurations() {
		ObjectMapper mapper = new ObjectMapper();
		List<Resource> resourceList = new ArrayList<>();
		try {
			Resource[] resources = new PathMatchingResourcePatternResolver().getResources("classpath:searches/*.json");
			if (resources != null) {
				for (Resource r : resources) {
					if (r != null && r.exists()) {
						resourceList.add(r);
					}
				}
			}
		} catch (Throwable e) {
			// Fallback para GraalVM Native Image onde listing dinâmico de classpath não é suportado
		}

		if (resourceList.isEmpty()) {
			String[] knownSearches = {"user.json", "profile.json", "role.json"};
			for (String search : knownSearches) {
				try {
					Resource res = new org.springframework.core.io.ClassPathResource("searches/" + search);
					if (res.exists()) {
						resourceList.add(res);
					}
				} catch (Throwable ignored) {
				}
			}
		}

		for (Resource resource : resourceList) {
			try (InputStream is = resource.getInputStream()) {
				SearchConfiguration config = mapper.readValue(is, SearchConfiguration.class);
				if (config != null) {
					searchConfigurations.add(config);
				}
			} catch (Throwable e) {
				// Ignora recurso com erro individual de parse
			}
		}
	}

	private List<SearchFieldDTO> convertColumnsToDTO(List<SearchField> columns) {
		List<SearchFieldDTO> columnsDTO = new ArrayList<>();
		for (SearchField column : columns) {
			columnsDTO.add(new SearchFieldDTO(
					Translator.getTranslatedText(column.getLabel()),
					column.getField(),
					column.isHidden(),
					column.isPrincipal(),
					column.isKey(),
					column.getType()));
		}
		return columnsDTO;
	}
}