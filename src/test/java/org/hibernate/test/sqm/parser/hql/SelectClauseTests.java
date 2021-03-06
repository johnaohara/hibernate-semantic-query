/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: Apache License, Version 2.0
 * See the LICENSE file in the root directory or visit http://www.apache.org/licenses/LICENSE-2.0
 */
package org.hibernate.test.sqm.parser.hql;

import java.util.List;
import java.util.Map;

import org.hibernate.sqm.SemanticQueryInterpreter;
import org.hibernate.sqm.StrictJpaComplianceViolation;
import org.hibernate.sqm.domain.DomainMetamodel;
import org.hibernate.sqm.domain.SingularAttribute;
import org.hibernate.sqm.path.AttributeBinding;
import org.hibernate.sqm.query.SqmQuerySpec;
import org.hibernate.sqm.query.SqmSelectStatement;
import org.hibernate.sqm.query.expression.AttributeReferenceSqmExpression;
import org.hibernate.sqm.query.expression.BinaryArithmeticSqmExpression;
import org.hibernate.sqm.query.expression.CollectionValuePathSqmExpression;
import org.hibernate.sqm.query.expression.MapEntrySqmExpression;
import org.hibernate.sqm.query.expression.MapKeyPathSqmExpression;
import org.hibernate.sqm.query.from.SqmFrom;
import org.hibernate.sqm.query.select.SqmDynamicInstantiation;
import org.hibernate.sqm.query.select.SqmDynamicInstantiationTarget;
import org.hibernate.sqm.query.select.SqmSelection;

import org.hibernate.test.sqm.ConsumerContextImpl;
import org.hibernate.test.sqm.domain.BasicTypeImpl;
import org.hibernate.test.sqm.domain.EntityTypeImpl;
import org.hibernate.test.sqm.domain.ExplicitDomainMetamodel;
import org.hibernate.test.sqm.domain.StandardBasicTypeDescriptors;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.hamcrest.CoreMatchers;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

/**
 * Test various forms of selections
 *
 * @author Steve Ebersole
 */
public class SelectClauseTests {
	private final ConsumerContextImpl consumerContext = new ConsumerContextImpl( buildMetamodel() );

	@Rule
	public ExpectedException expectedException = ExpectedException.none();

	private DomainMetamodel buildMetamodel() {
		ExplicitDomainMetamodel metamodel = new ExplicitDomainMetamodel();

		EntityTypeImpl entity2Type = metamodel.makeEntityType( "com.acme.Entity2" );
		entity2Type.makeSingularAttribute(
				"basic1",
				SingularAttribute.Classification.BASIC,
				StandardBasicTypeDescriptors.INSTANCE.LONG
		);

		EntityTypeImpl entityType = metamodel.makeEntityType( "com.acme.Entity" );
		entityType.makeSingularAttribute(
				"basic",
				SingularAttribute.Classification.BASIC,
				StandardBasicTypeDescriptors.INSTANCE.LONG
		);
		entityType.makeSingularAttribute(
				"basic1",
				SingularAttribute.Classification.BASIC,
				StandardBasicTypeDescriptors.INSTANCE.LONG
		);
		entityType.makeSingularAttribute(
				"basic2",
				SingularAttribute.Classification.BASIC,
				StandardBasicTypeDescriptors.INSTANCE.STRING
		);
		entityType.makeSingularAttribute(
				"basic3",
				SingularAttribute.Classification.BASIC,
				StandardBasicTypeDescriptors.INSTANCE.STRING
		);
		entityType.makeSingularAttribute(
				"basic4",
				SingularAttribute.Classification.BASIC,
				StandardBasicTypeDescriptors.INSTANCE.STRING
		);
		entityType.makeSingularAttribute(
				"from",
				SingularAttribute.Classification.BASIC,
				StandardBasicTypeDescriptors.INSTANCE.STRING
		);

		EntityTypeImpl legType = metamodel.makeEntityType( "com.acme.Leg" );

		EntityTypeImpl tripType = metamodel.makeEntityType( "com.acme.Trip" );
		tripType.makeMapAttribute(
				"mapLegs",
				StandardBasicTypeDescriptors.INSTANCE.STRING,
				legType
		);
		tripType.makeListAttribute(
				"collectionLegs",
				StandardBasicTypeDescriptors.INSTANCE.INTEGER,
				legType
		);

		return metamodel;
	}

	@Test
	public void testSimpleAliasSelection() {
		SqmSelectStatement statement = interpret( "select o from Entity o" );
		assertEquals( 1, statement.getQuerySpec().getSelectClause().getSelections().size() );
		SqmSelection selection = statement.getQuerySpec().getSelectClause().getSelections().get( 0 );
		assertThat( selection.getExpression(), instanceOf( SqmFrom.class ) );
	}

	@Test
	public void testSimpleAttributeSelection() {
		SqmSelectStatement statement = interpret( "select o.basic from Entity o" );
		assertEquals( 1, statement.getQuerySpec().getSelectClause().getSelections().size() );
		SqmSelection selection = statement.getQuerySpec().getSelectClause().getSelections().get( 0 );
		assertThat( selection.getExpression(), instanceOf( AttributeReferenceSqmExpression.class ) );
	}

	@Test
	public void testCompoundAttributeSelection() {
		SqmSelectStatement statement = interpret( "select o.basic1, o.basic2 from Entity o" );
		assertEquals( 2, statement.getQuerySpec().getSelectClause().getSelections().size() );
		assertThat(
				statement.getQuerySpec().getSelectClause().getSelections().get( 0 ).getExpression(),
				instanceOf( AttributeReferenceSqmExpression.class )
		);
		assertThat(
				statement.getQuerySpec().getSelectClause().getSelections().get( 1 ).getExpression(),
				instanceOf( AttributeReferenceSqmExpression.class )
		);
	}

	@Test
	public void testMixedAliasAndAttributeSelection() {
		SqmSelectStatement statement = interpret( "select o, o.basic1 from Entity o" );
		assertEquals( 2, statement.getQuerySpec().getSelectClause().getSelections().size() );
		assertThat(
				statement.getQuerySpec().getSelectClause().getSelections().get( 0 ).getExpression(),
				instanceOf( SqmFrom.class )
		);
		assertThat(
				statement.getQuerySpec().getSelectClause().getSelections().get( 1 ).getExpression(),
				instanceOf( AttributeBinding.class )
		);
	}

	@Test
	public void testSimpleDynamicInstantiationSelection() {
		SqmSelectStatement statement = interpret( "select new org.hibernate.test.sqm.parser.hql.SelectClauseTests$DTO(o.basic1, o.basic2) from Entity o" );
		assertEquals( 1, statement.getQuerySpec().getSelectClause().getSelections().size() );
		assertThat(
				statement.getQuerySpec().getSelectClause().getSelections().get( 0 ).getExpression(),
				instanceOf( SqmDynamicInstantiation.class )
		);
	}

	@Test
	public void testMultipleDynamicInstantiationSelection() {
		SqmSelectStatement statement = interpret(
				"select new org.hibernate.test.sqm.parser.hql.SelectClauseTests$DTO(o.basic1, o.basic2), " +
						"new org.hibernate.test.sqm.parser.hql.SelectClauseTests$DTO(o.basic1, o.basic2) " +
						"from Entity o"
		);
		assertEquals( 2, statement.getQuerySpec().getSelectClause().getSelections().size() );
		assertThat(
				statement.getQuerySpec().getSelectClause().getSelections().get( 0 ).getExpression(),
				instanceOf( SqmDynamicInstantiation.class )
		);
		assertThat(
				statement.getQuerySpec().getSelectClause().getSelections().get( 1 ).getExpression(),
				instanceOf( SqmDynamicInstantiation.class )
		);
	}

	@Test
	public void testMixedAttributeAndDynamicInstantiationSelection() {
		SqmSelectStatement statement = interpret(
				"select new org.hibernate.test.sqm.parser.hql.SelectClauseTests$DTO(o.basic1, o.basic2), o.basic3 from Entity o"
		);
		assertEquals( 2, statement.getQuerySpec().getSelectClause().getSelections().size() );
		assertThat(
				statement.getQuerySpec().getSelectClause().getSelections().get( 0 ).getExpression(),
				instanceOf( SqmDynamicInstantiation.class )
		);
		assertThat(
				statement.getQuerySpec().getSelectClause().getSelections().get( 1 ).getExpression(),
				instanceOf( AttributeReferenceSqmExpression.class )
		);
	}

	@Test
	public void testNestedDynamicInstantiationSelection() {
		SqmSelectStatement statement = interpret(
				"select new org.hibernate.test.sqm.parser.hql.SelectClauseTests$DTO(" +
						"    o.basic1, " +
						"    o.basic2, " +
						"    new org.hibernate.test.sqm.parser.hql.SelectClauseTests$DTO(o.basic3, o.basic4) " +
						" ) " +
						"from Entity o"
		);
		assertEquals( 1, statement.getQuerySpec().getSelectClause().getSelections().size() );
		assertThat(
				statement.getQuerySpec().getSelectClause().getSelections().get( 0 ).getExpression(),
				instanceOf( SqmDynamicInstantiation.class )
		);

		SqmDynamicInstantiation dynamicInstantiation = (SqmDynamicInstantiation) statement.getQuerySpec().getSelectClause().getSelections().get( 0 ).getExpression();
		assertThat(
				dynamicInstantiation.getInstantiationTarget().getNature(),
				equalTo( SqmDynamicInstantiationTarget.Nature.CLASS )
		);
		assertThat(
				dynamicInstantiation.getInstantiationTarget().getJavaType(),
				CoreMatchers.<Class>equalTo( DTO.class )
		);

		assertEquals( 3, dynamicInstantiation.getArguments().size() );
		assertThat(
				dynamicInstantiation.getArguments().get( 0 ).getExpression(),
				instanceOf( AttributeReferenceSqmExpression.class )
		);
		assertThat(
				dynamicInstantiation.getArguments().get( 1 ).getExpression(),
				instanceOf( AttributeReferenceSqmExpression.class )
		);
		assertThat(
				dynamicInstantiation.getArguments().get( 2 ).getExpression(),
				instanceOf( SqmDynamicInstantiation.class )
		);
		SqmDynamicInstantiation nestedInstantiation = (SqmDynamicInstantiation) dynamicInstantiation.getArguments().get( 2 ).getExpression();
		assertThat(
				nestedInstantiation.getInstantiationTarget().getNature(),
				equalTo( SqmDynamicInstantiationTarget.Nature.CLASS )
		);
		assertThat(
				nestedInstantiation.getInstantiationTarget().getJavaType(),
				CoreMatchers.<Class>equalTo( DTO.class )
		);

	}

	@Test
	public void testSimpleDynamicListInstantiation() {
		SqmSelectStatement statement = interpret( "select new list(o.basic1, o.basic2) from Entity o" );
		assertEquals( 1, statement.getQuerySpec().getSelectClause().getSelections().size() );
		assertThat(
				statement.getQuerySpec().getSelectClause().getSelections().get( 0 ).getExpression(),
				instanceOf( SqmDynamicInstantiation.class )
		);
		SqmDynamicInstantiation instantiation = (SqmDynamicInstantiation) statement.getQuerySpec().getSelectClause().getSelections().get( 0 ).getExpression();
		assertThat(
				instantiation.getInstantiationTarget().getNature(),
				equalTo( SqmDynamicInstantiationTarget.Nature.LIST )
		);
		assertThat(
				instantiation.getInstantiationTarget().getJavaType(),
				CoreMatchers.<Class>equalTo( List.class )
		);

		assertEquals( 2, instantiation.getArguments().size() );
		assertThat(
				instantiation.getArguments().get( 0 ).getExpression(),
				instanceOf( AttributeReferenceSqmExpression.class )
		);
		assertThat(
				instantiation.getArguments().get( 1 ).getExpression(),
				instanceOf( AttributeReferenceSqmExpression.class )
		);
	}

	@Test
	public void testSimpleDynamicMapInstantiation() {
		SqmSelectStatement statement = interpret( "select new map(o.basic1 as a, o.basic2 as b) from Entity o" );
		assertEquals( 1, statement.getQuerySpec().getSelectClause().getSelections().size() );
		assertThat(
				statement.getQuerySpec().getSelectClause().getSelections().get( 0 ).getExpression(),
				instanceOf( SqmDynamicInstantiation.class )
		);
		SqmDynamicInstantiation instantiation = (SqmDynamicInstantiation) statement.getQuerySpec().getSelectClause().getSelections().get( 0 ).getExpression();
		assertThat(
				instantiation.getInstantiationTarget().getNature(),
				equalTo( SqmDynamicInstantiationTarget.Nature.MAP )
		);
		assertThat(
				instantiation.getInstantiationTarget().getJavaType(),
				CoreMatchers.<Class>equalTo( Map.class )
		);

		assertEquals( 2, instantiation.getArguments().size() );
		assertThat(
				instantiation.getArguments().get( 0 ).getExpression(),
				instanceOf( AttributeReferenceSqmExpression.class )
		);
		assertThat(
				instantiation.getArguments().get( 1 ).getExpression(),
				instanceOf( AttributeReferenceSqmExpression.class )
		);
	}

	@Test
	public void testBinaryArithmeticExpression() {
		final String query = "select o.basic + o.basic1 as b from Entity o";
		final SqmSelectStatement selectStatement = interpret( query );

		final SqmQuerySpec querySpec = selectStatement.getQuerySpec();
		final SqmSelection selection = querySpec.getSelectClause().getSelections().get( 0 );
		BinaryArithmeticSqmExpression expression = (BinaryArithmeticSqmExpression) selection.getExpression();
		AttributeReferenceSqmExpression leftHandOperand = (AttributeReferenceSqmExpression) expression.getLeftHandOperand();
		assertThat( leftHandOperand.getLeftHandSide().getExpressionType().getTypeName(), is( "com.acme.Entity" ) );
		assertThat( leftHandOperand.getBoundAttribute().getName(), is( "basic" ) );

		AttributeReferenceSqmExpression rightHandOperand = (AttributeReferenceSqmExpression) expression.getRightHandOperand();
		assertThat( rightHandOperand.getLeftHandSide().getExpressionType().getTypeName(), is( "com.acme.Entity" ) );
		assertThat( rightHandOperand.getBoundAttribute().getName(), is( "basic1" ) );
	}

	@Test
	public void testBinaryArithmeticExpressionWithMultipleFromSpaces() {
		final String query = "select o.basic + a.basic1 as b from Entity o, Entity2 a";
		final SqmSelectStatement selectStatement = interpret( query );

		final SqmQuerySpec querySpec = selectStatement.getQuerySpec();
		final SqmSelection selection = querySpec.getSelectClause().getSelections().get( 0 );
		BinaryArithmeticSqmExpression expression = (BinaryArithmeticSqmExpression) selection.getExpression();
		AttributeReferenceSqmExpression leftHandOperand = (AttributeReferenceSqmExpression) expression.getLeftHandOperand();
		assertThat( leftHandOperand.getLeftHandSide().getExpressionType().getTypeName(), is( "com.acme.Entity" ) );
		assertThat( leftHandOperand.getBoundAttribute().getName(), is( "basic" ) );

		AttributeReferenceSqmExpression rightHandOperand = (AttributeReferenceSqmExpression) expression.getRightHandOperand();
		assertThat( rightHandOperand.getLeftHandSide().getExpressionType().getTypeName(), is( "com.acme.Entity2" ) );
		assertThat( rightHandOperand.getBoundAttribute().getName(), is( "basic1" ) );
	}

	@Test
	public void testMapKeyFunction() {
		SqmSelectStatement statement = interpret( "SELECT KEY( l ) FROM Trip t JOIN t.mapLegs l" );

		assertEquals( 1, statement.getQuerySpec().getSelectClause().getSelections().size() );
		assertThat(
				statement.getQuerySpec().getSelectClause().getSelections().get( 0 ).getExpression(),
				instanceOf( MapKeyPathSqmExpression.class )
		);

		MapKeyPathSqmExpression mapKeyPathExpression = (MapKeyPathSqmExpression) statement.getQuerySpec().getSelectClause().getSelections().get( 0 ).getExpression();
		assertThat( mapKeyPathExpression.getMapKeyType(), instanceOf( BasicTypeImpl.class ) );
		assertThat( mapKeyPathExpression.getMapKeyType().getTypeName(), is( String.class.getName() ) );

		assertThat( mapKeyPathExpression.getCollectionAlias(), is( "l") );
	}

	@Test
	public void testMapValueFunction() {
		SqmSelectStatement statement = interpret( "SELECT VALUE( l ) FROM Trip t JOIN t.mapLegs l" );

		assertEquals( 1, statement.getQuerySpec().getSelectClause().getSelections().size() );
		assertThat(
				statement.getQuerySpec().getSelectClause().getSelections().get( 0 ).getExpression(),
				instanceOf( CollectionValuePathSqmExpression.class )
		);

		CollectionValuePathSqmExpression collectionValuePathExpression = (CollectionValuePathSqmExpression) statement.getQuerySpec().getSelectClause().getSelections().get( 0 ).getExpression();

		assertThat( collectionValuePathExpression.getValueType(), instanceOf( EntityTypeImpl.class ) );
		assertThat( collectionValuePathExpression.getValueType().getTypeName(), is( "com.acme.Leg" ) );
		assertThat( collectionValuePathExpression.getPluralAttributeBinding().getIdentificationVariable(), is( "l") );
	}

	@Test
	public void testCollectionValueFunction() {
		SqmSelectStatement statement = interpret( "SELECT VALUE( l ) FROM Trip t JOIN t.collectionLegs l" );

		assertEquals( 1, statement.getQuerySpec().getSelectClause().getSelections().size() );
		assertThat(
				statement.getQuerySpec().getSelectClause().getSelections().get( 0 ).getExpression(),
				instanceOf( CollectionValuePathSqmExpression.class )
		);

		CollectionValuePathSqmExpression collectionValuePathExpression = (CollectionValuePathSqmExpression) statement.getQuerySpec().getSelectClause().getSelections().get( 0 ).getExpression();
		assertThat( collectionValuePathExpression.getElementType(), instanceOf( EntityTypeImpl.class ) );
		assertThat( collectionValuePathExpression.getElementType().getTypeName(), is( "com.acme.Leg" ) );
		assertThat( collectionValuePathExpression.getPluralAttributeBinding().getIdentificationVariable(), is( "l") );
	}

	@Test
	public void testCollectionValueFunctionNotSupportedInStrictMode() {
		consumerContext.enableStrictJpaCompliance();

		expectedException.expect( StrictJpaComplianceViolation.class );
		expectedException.expectMessage( "Encountered application of value() function to path expression which does not resolve to a persistent Map" );

		interpret( "SELECT VALUE( l ) FROM Trip t JOIN t.collectionLegs l" );
	}

	@Test
	public void testMapEntryFunction() {
		SqmSelectStatement statement = interpret( "SELECT ENTRY( l ) FROM Trip t JOIN t.mapLegs l" );

		assertEquals( 1, statement.getQuerySpec().getSelectClause().getSelections().size() );
		assertThat(
				statement.getQuerySpec().getSelectClause().getSelections().get( 0 ).getExpression(),
				instanceOf( MapEntrySqmExpression.class )
		);

		MapEntrySqmExpression mapEntryFunction = (MapEntrySqmExpression) statement.getQuerySpec().getSelectClause().getSelections().get( 0 ).getExpression();

		// Key
		assertThat( mapEntryFunction.getMapKeyType(), instanceOf( BasicTypeImpl.class ) );
		assertThat( mapEntryFunction.getMapKeyType().getTypeName(), is( String.class.getName() ) );

		// value/element
		assertThat( mapEntryFunction.getMapValueType(), instanceOf( EntityTypeImpl.class ) );
		assertThat( ( (EntityTypeImpl) mapEntryFunction.getMapValueType() ).getTypeName(), is( "com.acme.Leg" ) );

		assertThat( mapEntryFunction.getCollectionAlias(), is( "l" ) );
	}

	private SqmSelectStatement interpret(String query) {
		return (SqmSelectStatement) SemanticQueryInterpreter.interpret( query, consumerContext );
	}

	public static class DTO {
	}
}
