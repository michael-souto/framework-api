package com.detrasoft.framework.api.controllers;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.detrasoft.framework.api.dto.SearchFieldDTO;
import com.detrasoft.framework.api.dto.SearchReponseDTO;
import com.detrasoft.framework.core.resource.ResourceFunctions;
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

	private List<Map<String, Object>> resultList;

	private List<SearchField> columns;
	private String title;
	private String from;
	private String where;
	private String groupBy;
	private String orderBy;

	private List<SearchConfiguration> searchConfigurations = new ArrayList<>();

	public GenericSearchController () throws IOException {
		loadAllSearchConfigurations();
	}

	@Autowired
	SearchRepository searchRepository;

	private void loadConfiguration(String configurationId) {
		var config = getSearchConfigurationById(configurationId);
		if (config != null) {
			title = Translator.getTranslatedText(config.getTitle());
			from = config.getFrom();
			where = config.getWhere();
			groupBy = config.getGroupBy();
			orderBy = config.getOrderBy();
			columns = config.getColumns();
		}
	}

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
		loadConfiguration(id);
		SearchReponseDTO response = new SearchReponseDTO();
		
		response.setColumns(convertColumnsToDTO(columns));
		response.setTitle(title);
		return ResponseEntity.ok(response);
	}

	@GetMapping(value = "/{id}")
	public ResponseEntity<SearchReponseDTO> search(@PathVariable String id, @RequestParam Map<String, String> queryParams,
			@RequestParam(name = "unpaged", required = false) boolean unpaged, Pageable pageable) {
		loadConfiguration(id);
		SearchReponseDTO response = new SearchReponseDTO();
		resultList = new ArrayList<>();

		// Converte os query parameters em uma lista de SearchFields
		List<SearchField> searchFields = new ArrayList<>();
		queryParams.forEach((key, value) -> {
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
				searchFields.add(searchField);
			}
		});

		String query = getSQLNativeCommand(searchFields);
		List<Object[]> resultSQL;
		if (!unpaged) {
			resultSQL = searchRepository.findNativeSQL(query, pageable);

		} else {
			resultSQL = searchRepository.findNativeSQL(query);
		}

		for (Object[] row : resultSQL) {
			Map<String, Object> rowMap = new HashMap<>();
			for (int i = 0; i < columns.size(); i++) {
				SearchField column = columns.get(i);
				rowMap.put(column.getField(), row[i]);
			}
			resultList.add(rowMap);
		}
	
		response.setTotalRecords(searchRepository.countNativeSQL(query));
		Page<?> resultListPage = new PageImpl<>(resultList, pageable, response.getTotalRecords());

		response.setTitle(title);
		response.setColumns(convertColumnsToDTO(columns));
		response.setData(resultListPage);

		return ResponseEntity.ok(response);
	}

	private String getSQLNativeCommand(List<SearchField> searchFields) {
		String selectSQL = "SELECT ";
		for (int i = 0; i < columns.size(); i++) {
			selectSQL = selectSQL + columns.get(i).getColumnName() + " AS C" + i + " ";
			if (!(i == columns.size() - 1)) {
				selectSQL = selectSQL + ", ";
			}
		}

		String fromSQL = " FROM " + from;

		String whereSQL = (where != null ? where + " AND " : "");

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
				// STRING
				if (searchFields.get(i).getType().equals(FieldType.string)) {
					String value = searchFields.get(i).getValue().toString().replace(" ", "%");
					whereSQL = whereSQL + columnName + " ILIKE ('%" + value + "%')";
				} // DATE
				else if (searchFields.get(i).getType().equals(FieldType.date)) {
					String value = searchFields.get(i).getValue().toString().substring(0, 10);
					whereSQL = whereSQL + "date_trunc('day'," + columnName + ") = '" + value + "'";
				} // RANGE of DATE
				else if (searchFields.get(i).getType().equals(FieldType.rangedate)) {
					List<String> valueList = (List<String>) searchFields.get(i).getValue();
					valueList.removeIf(d -> d == null);
					DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
					if (valueList.size() == 2) {
						ZonedDateTime initDate = ZonedDateTime.parse(valueList.get(0), DateTimeFormatter.ISO_DATE_TIME);
						String initDateFormatted = initDate.format(formatter);
						ZonedDateTime endDate = ZonedDateTime.parse(valueList.get(1), DateTimeFormatter.ISO_DATE_TIME);
						String endDateFormatted = endDate.format(formatter);
						whereSQL = whereSQL + columnName + " BETWEEN '" + initDateFormatted + "' AND '"
								+ endDateFormatted + "' ";
					} else if (valueList.size() == 1) {
						ZonedDateTime date = ZonedDateTime.parse(valueList.get(0), DateTimeFormatter.ISO_DATE_TIME);
						String dateFormatted = date.format(formatter);
						whereSQL = whereSQL + "date_trunc('day'," + columnName + ") = '" + dateFormatted + "'";
					}
				} // CURRENCY 
				else if (searchFields.get(i).getType().equals(FieldType.currency)) {
					String value = searchFields.get(i).getValue().toString().replace(" ", "%");
					whereSQL = whereSQL + columnName + " = " + value.replace(',', '.');

				} 
				else if (searchFields.get(i).getType().equals(FieldType.guid)) {
					whereSQL = whereSQL + columnName + " = '" + searchFields.get(i).getValue().toString() + "'";
				}
				else {
					whereSQL = whereSQL + columnName + " = " + searchFields.get(i).getValue().toString();
				}

				whereSQL = whereSQL + " AND ";

			}
		}
		if (whereSQL.length() > 5 && whereSQL.substring(whereSQL.length() - 5, whereSQL.length()).equals(" AND ")) {
			whereSQL = whereSQL.substring(0, whereSQL.length() - 5);
		}
		whereSQL = (!whereSQL.equals("") ? " WHERE " + whereSQL : "");

		String groupBySQL = groupBy != null ? " GROUP BY " + groupBy : "";

		String orderBySQL = orderBy != null ? " ORDER BY " + orderBy : "";

		return selectSQL + fromSQL + whereSQL + groupBySQL + orderBySQL;
	}

	private void loadAllSearchConfigurations() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        Resource[] resources = new PathMatchingResourcePatternResolver().getResources("classpath:searches/*.json");

        for (Resource resource : resources) {
            InputStream is = resource.getInputStream();
            SearchConfiguration config = mapper.readValue(is, SearchConfiguration.class);
            searchConfigurations.add(config);
        }
    }

	private List<SearchFieldDTO> convertColumnsToDTO(List<SearchField> columns) {
		List<SearchFieldDTO> columnsDTO = new ArrayList<>();
		for (SearchField column : columns) {
			columnsDTO.add(new SearchFieldDTO(
				Translator.getTranslatedText(column.getLabel()),
				column.getField(),
				column.getType()
		));
		}
		return columnsDTO;
	}
}
