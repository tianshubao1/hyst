package com.verivital.hyst.ir.base;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

import com.verivital.hyst.grammar.formula.Constant;
import com.verivital.hyst.grammar.formula.DefaultExpressionPrinter;
import com.verivital.hyst.grammar.formula.Expression;
import com.verivital.hyst.ir.AutomatonExportException;
import com.verivital.hyst.ir.AutomatonValidationException;
import com.verivital.hyst.ir.Component;
import com.verivital.hyst.ir.Configuration;
import com.verivital.hyst.util.AutomatonUtil;

/**
 * A mode of a hybrid automaton.
 * 
 * name, automaton and invariant are nonnull name is a valid c identifier invariant is not null or
 * false patialFlowDynamics can only be null if urgent==true; flow may not be defined for every variable
 * (some variables may be omitted, for example havoc dynamics or dynamics defined in another
 * automaton). if nonnull, every variable defined in patialFlowDynamics must exist in the PDBaseComponent.
 * 
 * After flattening, every variable must have a defined flowDyanmics (this is one of the guarantees
 * provided by the flatten pass).
 * 
 * @author Stanley Bak (stanleybak@gmail.com)
 *
 */
public class PDAutomatonMode
{
	public String name;
	public final PDBaseComponent automaton;
	public Expression invariant;
	public boolean urgent = false;
	public LinkedHashMap<String, ExpressionInterval> partialFlowDynamics;

	/**
	 * The correct way to create a new automaton mode is using HybridAutomaton.createMode(name),
	 * which will manage the internal state of the automaton
	 * 
	 * @param ha
	 *            the hybrid automaton
	 */


	PDAutomatonMode(PDBaseComponent pdha, String name)
	{
		this.name = name;
		automaton = pdha;

		invariant = null; // this MUST be set, otherwise validation will fail
		partialFlowDynamics = new LinkedHashMap<String, ExpressionInterval>();

		for (String s : pdha.variables)
			partialFlowDynamics.put(s, null); // these MUST be set or removed,
										// otherwise validation will fail
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

		if (name == null)
			throw new AutomatonValidationException("name was null");

		Component.validateName(name, automaton.getPrintableInstanceName());

		if (automaton == null)
			throw new AutomatonValidationException("automaton was null");

		if (invariant == null)
			throw new AutomatonValidationException("invariant was null in mode " + name);

		// a lot of printers have no way to handle this; these states should
		// perhaps be trimmed from the automaton
		if (invariant.equals(Constant.FALSE))
			throw new AutomatonValidationException("invariant was equal to Constant.FALSE");

		if (partialFlowDynamics == null && urgent == false)
			throw new AutomatonValidationException("partialFlowDynamics was null but urgent was false");

		if (urgent == true && partialFlowDynamics != null)
			throw new AutomatonValidationException("Mode '" + name
					+ "' was urgent, but dynamics were also defined: " + partialFlowDynamics);

		if (partialFlowDynamics != null)
		{
			for (String s : partialFlowDynamics.keySet())
			{
				if (!automaton.variables.contains(s))
				{
					throw new AutomatonValidationException(
							"dynamics were defined for variable '" + s + "' in mode '" + name
									+ "', but the variable didn't exist in the parent PDBaseComponent '"
									+ automaton.getPrintableInstanceName() + "'");
				}
			}
		}

		if (invariant == null)
			throw new AutomatonValidationException("mode " + name + " invariant is null ");

		if (!urgent)
		{
			for (Entry<String, ExpressionInterval> entry : partialFlowDynamics.entrySet())
			{
				ExpressionInterval ei = entry.getValue();

				if (ei == null)
					throw new AutomatonValidationException(
							"Flow for " + entry.getKey() + " was null in mode " + name);
			}
		}
	}

	@Override
	public String toString()
	{
		return "[PDAutomatonMode name:" + name + ", urgent: " + urgent + ", invariant: "
				+ DefaultExpressionPrinter.instance.print(invariant) + ", partialFlowDynamics: "
				+ AutomatonUtil.getMapExpressionIntervalString(partialFlowDynamics) + "]";
	}

	/**
	 * Duplicate (deep copy) the mode, and add link the new one with this one's parent automaton.
	 * This does not copy any transitions. The automaton link is shallow-copied
	 * 
	 * @param the
	 *            parent of the new component
	 * @param newName
	 *            an automaton-unique name for the new mode
	 */
	public PDAutomatonMode copy(PDBaseComponent parent, String newName)
	{
		PDAutomatonMode rv = parent.createMode(newName);

		rv.invariant = invariant.copy();

		if (partialFlowDynamics != null)
		{
			for (Entry<String, ExpressionInterval> e : partialFlowDynamics.entrySet())
				rv.partialFlowDynamics.put(e.getKey(), e.getValue().copy());
		}
		else
			rv.partialFlowDynamics = null;

		rv.urgent = urgent;

		return rv;
	}

	/**
	 * Duplicate (deep copy) the mode, and add link the new one with this one's parent automaton.
	 * This does not copy any transitions. The automaton link is shallow-copied
	 * 
	 * @param newName
	 *            an automaton-unique name for the new mode
	 */
	public PDAutomatonMode copyWithTransitions(String newName)
	{
		PDAutomatonMode rv = copy(automaton, newName);

		// also copy the transitions
		ArrayList<PDAutomatonTransition> fromCopy = new ArrayList<PDAutomatonTransition>();
		ArrayList<PDAutomatonTransition> toCopy = new ArrayList<PDAutomatonTransition>();

		for (PDAutomatonTransition at : automaton.transitions)
		{
			if (at.from == this && at.to == this)
				throw new AutomatonExportException(
						"Can't clone automaton mode with self-loop since meaning is unclear.");
			else if (at.from == this)
				fromCopy.add(at);
			else if (at.to == this)
				toCopy.add(at);
		}

		for (PDAutomatonTransition at : fromCopy)
			at.copy(automaton).from = rv;

		for (PDAutomatonTransition at : toCopy)
			at.copy(automaton).to = rv;

		return rv;
	}

}
