/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: Apache License, Version 2.0
 * See the LICENSE file in the root directory or visit http://www.apache.org/licenses/LICENSE-2.0
 */
package org.hibernate.test.sqm.parser.criteria.tree;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.persistence.criteria.AbstractQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.ParameterExpression;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Selection;
import javax.persistence.criteria.Subquery;
import javax.persistence.metamodel.EntityType;

import org.hibernate.sqm.NotYetImplementedException;

import org.hibernate.test.sqm.parser.criteria.tree.path.FromImplementor;
import org.hibernate.test.sqm.parser.criteria.tree.path.RootImpl;

/**
 * Models basic query structure.  Used as a delegate in implementing both
 * {@link javax.persistence.criteria.CriteriaQuery} and
 * {@link Subquery}.
 * <p/>
 * Note the <tt>ORDER BY</tt> specs are neglected here.  That's because it is not valid
 * for a subquery to define an <tt>ORDER BY</tt> clause.  So we just handle them on the
 * root query directly...
 *
 * @author Steve Ebersole
 */
public class QueryStructure<T> implements Serializable {
	private final AbstractQuery<T> owner;
	private final CriteriaBuilderImpl criteriaBuilder;
	private final boolean isSubQuery;

	public QueryStructure(AbstractQuery<T> owner, CriteriaBuilderImpl criteriaBuilder) {
		this.owner = owner;
		this.criteriaBuilder = criteriaBuilder;
		this.isSubQuery = Subquery.class.isInstance( owner );
	}

	private boolean distinct;
	private Selection<? extends T> selection;
	private Set<Root<?>> roots = new LinkedHashSet<Root<?>>();
	private Set<FromImplementor> correlationRoots;
	private Predicate restriction;
	private List<Expression<?>> groupings = Collections.emptyList();
	private Predicate having;
	private List<Subquery<?>> subqueries;



	// SELECTION ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public boolean isDistinct() {
		return distinct;
	}

	public void setDistinct(boolean distinct) {
		this.distinct = distinct;
	}

	public Selection<? extends T> getSelection() {
		return selection;
	}

	public void setSelection(Selection<? extends T> selection) {
		this.selection = selection;
	}


	// ROOTS ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public Set<Root<?>> getRoots() {
		return roots;
	}

	public <X> Root<X> from(Class<X> entityClass) {
		org.hibernate.sqm.domain.EntityType entityType = criteriaBuilder.consumerContext()
				.getDomainMetamodel()
				.resolveEntityType( entityClass );
		if ( entityType == null ) {
			throw new IllegalArgumentException( entityClass + " is not an entity" );
		}
		return from( entityType );
	}

	private <X> Root<X> from(org.hibernate.sqm.domain.EntityType entityType) {
		RootImpl<X> root = new RootImpl<X>( criteriaBuilder, entityType );
		roots.add( root );
		return root;
	}

	public <X> Root<X> from(String entityName) {
		org.hibernate.sqm.domain.EntityType entityType = criteriaBuilder.consumerContext()
				.getDomainMetamodel()
				.resolveEntityType( entityName );
		if ( entityType == null ) {
			throw new IllegalArgumentException( entityName + " is not an entity" );
		}
		return from( entityType );
	}

	public <X> Root<X> from(EntityType<X> entityType) {
		throw new NotYetImplementedException(  );
//		RootImpl<X> root = new RootImpl<X>( criteriaBuilder, entityType );
//		roots.add( root );
//		return root;
	}


	// CORRELATION ROOTS ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public void addCorrelationRoot(FromImplementor fromImplementor) {
		if ( !isSubQuery ) {
			throw new IllegalStateException( "Query is not identified as sub-query" );
		}
		if ( correlationRoots == null ) {
			correlationRoots = new HashSet<FromImplementor>();
		}
		correlationRoots.add( fromImplementor );
	}

	public Set<Join<?, ?>> collectCorrelatedJoins() {
		if ( !isSubQuery ) {
			throw new IllegalStateException( "Query is not identified as sub-query" );
		}
		final Set<Join<?, ?>> correlatedJoins;
		if ( correlationRoots != null ) {
			correlatedJoins = new HashSet<Join<?,?>>();
			for ( FromImplementor<?,?> correlationRoot : correlationRoots ) {
				if (correlationRoot instanceof Join<?,?> && correlationRoot.isCorrelated()) {
					correlatedJoins.add( (Join<?,?>) correlationRoot );
				}
				correlatedJoins.addAll( correlationRoot.getJoins() );
			}
		}
		else {
			correlatedJoins = Collections.emptySet();
		}
		return correlatedJoins;
	}


	// RESTRICTIONS ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public Predicate getRestriction() {
		return restriction;
	}

	public void setRestriction(Predicate restriction) {
		this.restriction = restriction;
	}


	// GROUPINGS ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public List<Expression<?>> getGroupings() {
		return groupings;
	}

	public void setGroupings(List<Expression<?>> groupings) {
		this.groupings = groupings;
	}

	public void setGroupings(Expression<?>... groupings) {
		if ( groupings != null && groupings.length > 0 ) {
			this.groupings = Arrays.asList( groupings );
		}
		else {
			this.groupings = Collections.emptyList();
		}
	}

	public Predicate getHaving() {
		return having;
	}

	public void setHaving(Predicate having) {
		this.having = having;
	}


	// SUB-QUERIES ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public List<Subquery<?>> getSubqueries() {
		return subqueries;
	}

	public List<Subquery<?>> internalGetSubqueries() {
		if ( subqueries == null ) {
			subqueries = new ArrayList<Subquery<?>>();
		}
		return subqueries;
	}

	public <U> Subquery<U> subquery(Class<U> subqueryType) {
//		CriteriaSubqueryImpl<U> subquery = new CriteriaSubqueryImpl<U>( criteriaBuilder, subqueryType, owner );
//		internalGetSubqueries().add( subquery );
//		return subquery;

		throw new NotYetImplementedException(  );
	}


	// PARAMETERS ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public Set<ParameterExpression<?>> collectParameters() {
		final Set<ParameterExpression<?>> parameters = new LinkedHashSet<ParameterExpression<?>>();
//		final ParameterRegistry registry = new ParameterRegistry() {
//			public void registerParameter(ParameterExpression<?> parameter) {
//				parameters.add( parameter );
//			}
//		};
//
//		ParameterContainer.Helper.possibleParameter(selection, registry);
//		ParameterContainer.Helper.possibleParameter(restriction, registry);
//		ParameterContainer.Helper.possibleParameter(having, registry);
//		if ( subqueries != null ) {
//			for ( Subquery subquery : subqueries ) {
//				ParameterContainer.Helper.possibleParameter(subquery, registry);
//			}
//		}
//
//		// both group-by and having expressions can (though unlikely) contain parameters...
//		ParameterContainer.Helper.possibleParameter(having, registry);
//		if ( groupings != null ) {
//			for ( Expression<?> grouping : groupings ) {
//				ParameterContainer.Helper.possibleParameter(grouping, registry);
//			}
//		}
//
		return parameters;
	}
}
