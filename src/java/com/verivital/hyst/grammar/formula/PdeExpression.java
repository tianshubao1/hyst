/**
 * 
 */
package com.verivital.hyst.grammar.formula;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

import com.verivital.hyst.ir.AutomatonExportException;

/**
 * A partial differential equation (PDE) as part of an expression. Currently these are only allowed inside flow
 * expressions.
 * 
 * @author Stanley Bak
 *
 */
public class PdeExpression extends Expression
{

	public Operator op = Operator.ULINE;

	public List<Variable> list = new ArrayList<Variable>();

	public PdeExpression(Variable v1, Variable v2)
	{
		this.list.add(v1);
		this.list.add(v2);
	}

	@Override
	public Expression copy()
	{
		return new PdeExpression(list.get(0), list.get(1));
	}

	public String toString()
	{
		Variable v1 = list.get(0);
		Variable v2 = list.get(1);
		String s = v1.name + op + v2.name;

		return s;
	}
}
