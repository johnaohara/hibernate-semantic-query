/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: Apache License, Version 2.0
 * See the LICENSE file in the root directory or visit http://www.apache.org/licenses/LICENSE-2.0
 */
package org.hibernate.sqm.domain;

/**
 * Specialization of Attribute for persistent collection values
 *
 * @author Steve Ebersole
 */
public interface PluralAttribute extends Attribute, Bindable {
	/**
	 * Classifications of the plurality
	 */
	enum CollectionClassification {
		SET,
		LIST,
		MAP,
		BAG
	}

	/**
	 * both Hibernate and JPA define this in terms of the collection itself,
	 * but really it describe the collection's element classification
	 */
	enum ElementClassification {
		BASIC,
		EMBEDDABLE,
		ANY,
		ONE_TO_MANY,
		MANY_TO_MANY
	}

	CollectionClassification getCollectionClassification();
	ElementClassification getElementClassification();

	/**
	 * Really only used for "id bag" mappings.  Defines the type for the identifier
	 * value used to uniquely identify each collection row
	 *
	 * @return The collection (id bag) id type
	 */
	BasicType getCollectionIdType();

	/**
	 * Obtain's the type used for the list-index or map-key of the collection.  Will
	 * return {@code null} if the collection is not a list (or array) or Map.
	 *
	 * @return The list-index or map-key type, or {@code null} if the collection is
	 * not a list (or array) or Map.
	 */
	Type getIndexType();

	/**
	 * Obtain's the type used for the elements/values of the collection.
	 *
	 * @return The element/value type.
	 */
	Type getElementType();
}
