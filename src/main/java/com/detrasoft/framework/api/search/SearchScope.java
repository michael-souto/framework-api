package com.detrasoft.framework.api.search;

import java.util.Map;

/**
 * Schema extra consultado por uma search (ramo do UNION ALL).
 *
 * @param schema     nome do schema (apenas digitos; validado pelo controller)
 * @param attributes atributos adicionados a cada linha vinda deste schema
 *                   (ex: shared=true, sharedTenant, sharedBusiness)
 */
public record SearchScope(String schema, Map<String, Object> attributes) {

	public SearchScope {
		attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
	}
}
