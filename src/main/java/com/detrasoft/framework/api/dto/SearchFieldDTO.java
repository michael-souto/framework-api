package com.detrasoft.framework.api.dto;

import com.detrasoft.framework.crud.entities.FieldType;
import com.detrasoft.framework.crud.entities.SearchField;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SearchFieldDTO {
	private String label;
	private String field;
	private FieldType type;

    public static SearchFieldDTO fromEntity(SearchField searchField) {
		if (searchField == null) {
			return null;
		}
		return new SearchFieldDTO(
				searchField.getLabel(),
				searchField.getField(),
				searchField.getType()
		);
	}
}
