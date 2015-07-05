package org.datanucleus.store.rdbms.sql.expression;

import org.datanucleus.store.rdbms.mapping.java.JavaTypeMapping;
import org.datanucleus.store.rdbms.mapping.java.WrapperContainerMapping;
import org.datanucleus.store.rdbms.sql.SQLStatement;
import org.datanucleus.store.rdbms.sql.SQLTable;

public class WrapperContainerExpression extends DelegatedExpression {
	protected SQLExpression[] wrappedExpressions;

	public WrapperContainerExpression(SQLStatement stmt, SQLTable table,
			JavaTypeMapping mapping) {
		super(stmt, table, mapping);
		WrapperContainerMapping wcm = (WrapperContainerMapping) mapping;
		
		JavaTypeMapping wrappedMapping = wcm.getWrappedMapping();
		if ( wrappedMapping != null )
		{
            delegate = stmt.getSQLExpressionFactory().newExpression(stmt, table,
    				wrappedMapping);
		}
	}
}
