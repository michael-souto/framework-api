package com.detrasoft.framework.api.dto;

import java.util.List;

import org.springframework.data.domain.Page;

import com.detrasoft.framework.core.notification.Message;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SearchReponseDTO {
	private String title;
	private Page<?> data;
	private List<SearchFieldDTO> columns;
	private List<Message> messages;
	private int totalRecords;
}
