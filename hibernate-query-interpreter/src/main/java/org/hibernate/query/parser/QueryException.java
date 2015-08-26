/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: Apache License, Version 2.0
 * See the LICENSE file in the root directory or visit http://www.apache.org/licenses/LICENSE-2.0
 */
package org.hibernate.query.parser;

/**
 * Base for all HQL parser specific exceptions.
 *
 * @author Steve Ebersole
 */
public class QueryException extends RuntimeException {
	public QueryException(String message) {
		super( message );
	}

	public QueryException(String message, Throwable cause) {
		super( message, cause );
	}
}
