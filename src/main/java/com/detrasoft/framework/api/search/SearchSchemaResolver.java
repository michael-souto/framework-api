package com.detrasoft.framework.api.search;

/**
 * Resolve o schema corrente usado em {@code :detrasoft_id}. Opcional: sem bean
 * (ou retornando null) o controller usa a claim "detrasoftId" do token.
 */
public interface SearchSchemaResolver {

	String currentSchema();
}
