package com.detrasoft.framework.api.search;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.detrasoft.framework.crud.entities.FieldType;

/**
 * Monta o SQL de uma search executada em varios schemas (UNION ALL).
 *
 * Cada ramo e o SELECT original (colunas C0..Cn) com o schema do ramo, mais as
 * expressoes do ORDER BY como O0..Ok e o indice do ramo como S. A ordenacao e
 * a paginacao acontecem uma unica vez, na query externa.
 */
public final class SearchUnionBuilder {

	public static final String SCOPE_COLUMN = "S";

	private static final Pattern ORDER_ITEM = Pattern.compile(
			"^(.*?)((?:\\s+(?:ASC|DESC))?(?:\\s+NULLS\\s+(?:FIRST|LAST))?)$",
			Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

	private SearchUnionBuilder() {
	}

	/** Item do ORDER BY: expressao e sufixo (" DESC", " ASC NULLS FIRST"...). */
	public record OrderItem(String expression, String suffix) {
	}

	public static List<OrderItem> parseOrderBy(String orderBy) {
		List<OrderItem> items = new ArrayList<>();
		if (orderBy == null || orderBy.isBlank()) {
			return items;
		}
		for (String part : splitTopLevel(orderBy)) {
			String item = part.trim();
			if (item.isEmpty()) {
				continue;
			}
			Matcher m = ORDER_ITEM.matcher(item);
			if (m.matches() && !m.group(1).isBlank()) {
				items.add(new OrderItem(m.group(1).trim(), m.group(2)));
			} else {
				items.add(new OrderItem(item, ""));
			}
		}
		return items;
	}

	/** Divide por virgulas fora de parenteses e de aspas simples/duplas. */
	static List<String> splitTopLevel(String sql) {
		List<String> parts = new ArrayList<>();
		int depth = 0;
		boolean single = false;
		boolean dbl = false;
		int start = 0;
		for (int i = 0; i < sql.length(); i++) {
			char c = sql.charAt(i);
			if (c == '\'' && !dbl) {
				single = !single;
			} else if (c == '"' && !single) {
				dbl = !dbl;
			} else if (!single && !dbl) {
				if (c == '(') {
					depth++;
				} else if (c == ')') {
					depth--;
				} else if (c == ',' && depth == 0) {
					parts.add(sql.substring(start, i));
					start = i + 1;
				}
			}
		}
		parts.add(sql.substring(start));
		return parts;
	}

	/**
	 * Ramo do UNION: {@code select} termina na ultima coluna Cn e {@code body}
	 * comeca em " FROM" (sem ORDER BY).
	 */
	public static String branch(String select, String body, List<OrderItem> order, int scopeIndex) {
		StringBuilder sql = new StringBuilder(select.stripTrailing());
		for (int k = 0; k < order.size(); k++) {
			sql.append(", ").append(order.get(k).expression()).append(" AS O").append(k);
		}
		sql.append(", ").append(scopeIndex).append(" AS ").append(SCOPE_COLUMN);
		sql.append(body);
		return sql.toString();
	}

	/** Lista mesclada: mesma ordenacao original; empate desempata pelo ramo (home primeiro). */
	public static String union(List<String> branches, List<OrderItem> order) {
		StringBuilder sql = new StringBuilder("SELECT * FROM (");
		sql.append(String.join(" UNION ALL ", branches));
		sql.append(") U ORDER BY ");
		for (int k = 0; k < order.size(); k++) {
			sql.append("O").append(k).append(order.get(k).suffix()).append(", ");
		}
		sql.append(SCOPE_COLUMN);
		return sql.toString();
	}

	/**
	 * Totalizadores (mergeMode "sum"): uma linha com a soma das colunas
	 * numericas; as demais ficam com o valor do schema corrente (ramo 0).
	 */
	public static String sum(List<String> branches, List<FieldType> columnTypes) {
		StringBuilder sql = new StringBuilder("SELECT ");
		for (int i = 0; i < columnTypes.size(); i++) {
			if (i > 0) {
				sql.append(", ");
			}
			FieldType type = columnTypes.get(i);
			if (type == FieldType.number || type == FieldType.currency) {
				sql.append("SUM(C").append(i).append(")");
			} else {
				sql.append("(ARRAY_AGG(C").append(i).append(" ORDER BY ").append(SCOPE_COLUMN).append("))[1]");
			}
			sql.append(" AS C").append(i);
		}
		sql.append(" FROM (").append(String.join(" UNION ALL ", branches)).append(") U");
		return sql.toString();
	}
}
