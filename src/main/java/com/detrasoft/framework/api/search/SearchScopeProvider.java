package com.detrasoft.framework.api.search;

import java.util.List;

/**
 * Fornece schemas adicionais para uma search. Opcional: sem nenhum bean a
 * search roda apenas no schema corrente (comportamento original).
 */
public interface SearchScopeProvider {

	/** Schemas extras para a search {@code searchId}; lista vazia quando nao se aplica. */
	List<SearchScope> extraScopes(String searchId);
}
