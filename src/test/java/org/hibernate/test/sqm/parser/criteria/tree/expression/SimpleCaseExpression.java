/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.sqm.parser.criteria.tree.expression;


import org.hibernate.sqm.NotYetImplementedException;
import org.hibernate.sqm.parser.criteria.spi.CriteriaVisitor;
import org.hibernate.sqm.query.select.AliasedExpressionContainer;
import org.hibernate.test.sqm.parser.criteria.tree.CriteriaBuilderImpl;

import javax.persistence.criteria.CriteriaBuilder.SimpleCase;
import javax.persistence.criteria.Expression;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Models what ANSI SQL terms a simple case statement.  This is a <tt>CASE</tt> expression in the form<pre>
 * CASE [expression]
 *     WHEN [firstCondition] THEN [firstResult]
 *     WHEN [secondCondition] THEN [secondResult]
 *     ELSE [defaultResult]
 * END
 * </pre>
 *
 * @author Steve Ebersole
 */
public class SimpleCaseExpression<C,R>
		extends ExpressionImpl<R>
		implements SimpleCase<C,R>, Serializable {
	private Class<R> javaType;
	private final Expression<? extends C> expression;
	private List<WhenClause> whenClauses = new ArrayList<WhenClause>();
	private Expression<? extends R> otherwiseResult;

	public class WhenClause {
		private final LiteralExpression<C> condition;
		private final Expression<? extends R> result;

		public WhenClause(LiteralExpression<C> condition, Expression<? extends R> result) {
			this.condition = condition;
			this.result = result;
		}

		public LiteralExpression<C> getCondition() {
			return condition;
		}

		public Expression<? extends R> getResult() {
			return result;
		}

	}

	public SimpleCaseExpression(
			CriteriaBuilderImpl criteriaBuilder,
			Class<R> javaType,
			Expression<? extends C> expression) {
		super( criteriaBuilder, javaType);
		this.javaType = javaType;
		this.expression = expression;
	}

	@SuppressWarnings({ "unchecked" })
	public Expression<C> getExpression() {
		return (Expression<C>) expression;
	}

	public SimpleCase<C, R> when(C condition, R result) {
		return when( condition, buildLiteral(result) );
	}

	@SuppressWarnings({ "unchecked" })
	private LiteralExpression<R> buildLiteral(R result) {
		final Class<R> type = result != null
				? (Class<R>) result.getClass()
				: getJavaType();
		return new CaseLiteralExpression<R>( criteriaBuilder(), type, result );
	}

	public SimpleCase<C, R> when(C condition, Expression<? extends R> result) {
		WhenClause whenClause = new WhenClause(
				new LiteralExpression<C>( criteriaBuilder(), condition ),
				result
		);
		whenClauses.add( whenClause );
		adjustJavaType( result );
		return this;
	}

	@SuppressWarnings({ "unchecked" })
	private void adjustJavaType(Expression<? extends R> exp) {
		if ( javaType == null ) {
			javaType = (Class<R>) exp.getJavaType();
		}
	}

	public Expression<R> otherwise(R result) {
		return otherwise( buildLiteral(result) );
	}

	public Expression<R> otherwise(Expression<? extends R> result) {
		this.otherwiseResult = result;
		adjustJavaType( result );
		return this;
	}

	public Expression<? extends R> getOtherwiseResult() {
		return otherwiseResult;
	}

	public List<WhenClause> getWhenClauses() {
		return whenClauses;
	}

	@Override
	public org.hibernate.sqm.query.expression.Expression visitExpression(CriteriaVisitor visitor) {
		return visitor.visitCase(this);
	}

	@Override
	public void visitSelections(CriteriaVisitor visitor, AliasedExpressionContainer container) {
		throw new NotYetImplementedException();
	}

}