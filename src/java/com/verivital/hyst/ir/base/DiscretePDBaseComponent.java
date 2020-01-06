package com.verivital.hyst.ir.base;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.Set;

import com.verivital.hyst.grammar.formula.Expression;
import com.verivital.hyst.grammar.formula.FormulaParser;
import com.verivital.hyst.grammar.formula.Operation;
import com.verivital.hyst.grammar.formula.Variable;
import com.verivital.hyst.ir.AutomatonValidationException;
import com.verivital.hyst.ir.Component;
import com.verivital.hyst.ir.Configuration;
import com.verivital.hyst.main.Hyst;
import com.verivital.hyst.util.AutomatonUtil;

/**
 * Main (flattened) hybrid automaton class for the internal representation.
 * 
 * After parsing a model into the intermediate representation, the following guarantees are
 * provided: name is not null
 * 
 * modes is not null, and there is at least one mode in modes transitions is not null, but may be
 * empty
 * 
 * labels (exported labels) must match at least one label in a transition
 * 
 * the defined flows in all non-urgent locations must be for the same variables
 * 
 * @author Stanley Bak (stanleybak@gmail.com)
 *
 */
public class DiscretePDBaseComponent extends Component
{
	public LinkedHashMap<String, DiscretePDAutomatonMode> modes = new LinkedHashMap<String, DiscretePDAutomatonMode>();
	public ArrayList<AutomatonTransition> transitions = new ArrayList<AutomatonTransition>();
	public int discreteSize size = 1;
	
	/**
	 * Create a new mode in this hybrid automaton. By default the invariant is null (must be
	 * manually set) and the flows are x'=null for all x (these must be assigned), or flows can be
	 * set to null and the mode's urgent flag enabled
	 * 
	 * @param name
	 *            a name for the mode (must be unique)
	 * @return the created PDAutomatonMode object
	 */
	 
	public DiscretePDBaseComponent(int n){
		if(n >= 1)
			size = n;
	} 
	
	public DiscretePDAutomatonMode createMode(List<String> names)
	{
		DiscretePDAutomatonMode rv = new DiscretePDAutomatonMode(this, names);

		if (modes.containsKey(names))
			throw new AutomatonValidationException("Mode with name '" + name + "' already exists.");

		modes.put(names, rv);

		return rv;
	}

	/**
	 * Create a new mode in this hybrid automaton. By default the invariant is null (must be
	 * manually set) and the flows are x'=<allDynamics>
	 * 
	 * @param name
	 *            a name for the mode (must be unique)
	 * @param allDynamics
	 *            the dynamics for every variable
	 * @return the created PDAutomatonMode object
	 */
	public DiscretePDAutomatonMode createMode(List<String> names, ExpressionInterval patialFlowDynamics)
	{
		PDAutomatonMode am = createMode(names);
		//do PDE discretization here
		if(patialFlowDynamics == "u__")
			Expression = u_1 - 2*u_2 + u_3;	
			
			
		for (String v : variables)
			am.patialFlowDynamics.put(v, allDynamics.copy());

		return am;
	}

	/**
	 * Create mode with given invariant and flows as strings
	 * 
	 * @param name
	 *            then name of the mode
	 * @param invariant
	 *            the mode invariant
	 * @param flows
	 *            the mode flow expression
	 * @return the created Mode
	 */
	public DiscretePDAutomatonMode createMode(List<String> names, String invariant, String flowString)
	{
		PDAutomatonMode rv = new PDAutomatonMode(this, names);

		if (modes.containsKey(names))
			throw new AutomatonValidationException("Mode with name '" + name + "' already exists.");

		modes.put(names, rv);

		rv.invariant = FormulaParser.parseInvariant(invariant);
		Expression flowExpression = FormulaParser.parseFlow(flowString);
		
		//discretization
		for(int i = 1; i <= size; i++){
		
		}
		rv.patialFlowDynamics = new LinkedHashMap<String, ExpressionInterval>();

		for (Entry<String, Expression> e : AutomatonUtil
				.parseFlowExpression(variables, flowExpression).entrySet())
			rv.patialFlowDynamics.put(e.getKey(), new ExpressionInterval(e.getValue()));

		return rv;
	}

	/**
	 * Create a new transition in this hybrid automaton. Guard is initially null; be sure to assign
	 * it or validation will fail.
	 * 
	 * @param from
	 *            the source
	 * @param to
	 *            the destination
	 * @return the created AutomatonTransition object
	 */
	public AutomatonTransition createTransition(DiscretePDAutomatonMode from, DiscretePDAutomatonMode to)
	{
		AutomatonTransition rv = new AutomatonTransition(this, from, to);

		if (this != from.automaton || this != to.automaton)
			throw new AutomatonValidationException(
					"created transition between different Hybrid Automata");

		transitions.add(rv);

		return rv;
	}

	/**
	 * Check if the guarantees expected of this class are met. This is run prior to any printing
	 * procedures.
	 * 
	 * @throws AutomatonValidationException
	 *             if guarantees are violated
	 */
	public void validate()
	{
		if (!Configuration.DO_VALIDATION)
			return;

		super.validate();

		if (modes == null)
			throw new AutomatonValidationException("modes was null");

		if (modes.size() < 1)
			throw new AutomatonValidationException("modes size was not >= 1");

		if (transitions == null)
			throw new AutomatonValidationException("transitions was null");

		for (Entry<String, PDAutomatonMode> e : modes.entrySet())
		{
			if (!e.getKey().equals(e.getValue().name))
			{
				throw new AutomatonValidationException("mode map name mismatch. In map name is "
						+ e.getKey() + "," + "but in the PDAutomatonMode it's " + e.getValue().name);
			}
		}

		for (PDAutomatonMode m : modes.values())
			m.validate();

		for (AutomatonTransition t : transitions)
			t.validate();

		for (String label : labels)
		{
			boolean found = false;

			for (AutomatonTransition t : transitions)
			{
				if (label.equals(t.label))
				{
					found = true;
					break;
				}
			}

			if (!found)
			{
				String msg = "Exported label '" + label + "' was not used in PDBaseComponent '"
						+ getPrintableInstanceName() + "'.";
				Hyst.log(msg
						+ " This would block all transitions using this label in other components, and is typically a mistake.");
				throw new AutomatonValidationException(msg);
			}
		}

		// the defined flows in all locations must be for the same set of
		// variables
		Set<String> firstModeFlows = null;
		String firstModeName = null;

		for (Entry<String, PDAutomatonMode> e : modes.entrySet())
		{
			String name = e.getKey();
			PDAutomatonMode am = e.getValue();

			if (am.urgent)
				continue;

			Set<String> flows = am.patialFlowDynamics.keySet();

			if (firstModeName == null)
			{
				firstModeName = name;
				firstModeFlows = flows;
			}
			else
			{
				if (!flows.equals(firstModeFlows))
				{
					throw new AutomatonValidationException("PDBaseComponent "
							+ getPrintableInstanceName()
							+ ": Variables with defined flows in mode '" + firstModeName + "' ("
							+ firstModeFlows + ") differ from mode '" + name + "' (" + flows + ")");
				}
			}

			for (Entry<String, ExpressionInterval> entry : am.patialFlowDynamics.entrySet())
			{
				Expression exp = entry.getValue().getExpression();

				try
				{
					checkExpression(exp);
				}
				catch (AutomatonValidationException ave)
				{
					throw new AutomatonValidationException("PDBaseComponent "
							+ getPrintableInstanceName() + ": Flow in mode '" + am.name
							+ "' for variable '" + entry.getKey() + "'='" + exp.toDefaultString()
							+ "' uses a variable/constant not in the component. "
							+ ave.getMessage());
				}
			}
		}
	}

	/**
	 * Checks that Variables in an Expression are defined in the component.
	 * 
	 * @param e
	 *            the expression to check
	 */
	private void checkExpression(Expression e)
	{
		if (e instanceof Variable)
		{
			Variable v = (Variable) e;

			if (!variables.contains(v.name) && !constants.containsKey(v.name))
				throw new AutomatonValidationException(
						"Variable/constant not in automaton: '" + v.name + "'");
		}
		else if (e instanceof Operation)
		{
			Operation o = e.asOperation();

			for (Expression child : o.children)
				checkExpression(child);
		}
	}

	@Override
	public String toString()
	{
		StringBuilder str = new StringBuilder();

		str.append("[PDBaseComponent: ");

		str.append(super.toString());

		str.append("\nModes (" + modes.size() + " total):");

		Set<Entry<String, PDAutomatonMode>> set = modes.entrySet();
		for (Entry<String, PDAutomatonMode> e : set)
			str.append("\n " + e.getKey() + ": " + e.getValue());

		str.append("\nTransitions (" + transitions.size() + " total):");

		for (AutomatonTransition t : transitions)
			str.append("\n " + t);

		str.append("]");

		return str.toString();
	}

	@Override
	public Collection<String> getAllVariables()
	{
		ArrayList<String> rv = new ArrayList<String>(variables.size());

		for (String v : variables)
		{
			String fullName = getFullyQualifiedVariableName(v);
			rv.add(fullName);
		}

		return rv;
	}

	@Override
	protected Component copyComponent()
	{
		DiscretePDBaseComponent rv = new DiscretePDBaseComponent();

		// copy modes
		for (Entry<String, DiscretePDAutomatonMode> e : modes.entrySet())
		{
			PDAutomatonMode am = e.getValue();
			rv.modes.put(e.getKey(), am.copy(rv, am.name));
		}

		// copy transitions
		for (AutomatonTransition at : transitions)
			at.copy(rv); // this adds it to rv

		return rv;
	}

	/**
	 * Get all the variables, constants, and label names in the component
	 * 
	 * @return
	 */
	public Collection<String> getAllNames()
	{
		Collection<String> rv = new ArrayList<String>();

		rv.addAll(variables);
		rv.addAll(constants.keySet());
		rv.addAll(labels);

		return rv;
	}

	/**
	 * Find a transition in the automaton
	 * 
	 * @param from
	 *            the mode from
	 * @param to
	 *            the mode to
	 * @return the first transition between modes named 'from' and 'to', or null if not found
	 */
	public AutomatonTransition findTransition(String from, String to)
	{
		AutomatonTransition rv = null;

		for (AutomatonTransition at : transitions)
		{
			if (at.from.name.equals(from) && at.to.name.equals(to))
			{
				rv = at;
				break;
			}
		}

		return rv;
	}
	
	
}
