/**
 * 
 */
package com.verivital.hyst.printers;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;

import com.verivital.hyst.ir.AutomatonExportException;
import com.verivital.hyst.ir.Configuration;
import com.verivital.hyst.main.Hyst;
import com.verivital.hyst.main.HystFrame;
import com.verivital.hyst.util.AutomatonUtil;
import com.verivital.hyst.util.CmdLineRuntimeException;
import com.verivital.hyst.util.Preconditions;
import com.verivital.hyst.util.Preconditions.PreconditionsFailedException;
import com.verivital.hyst.util.PreconditionsFlag;

/**
 * A generic tool printer class. Printers for individual tools will override this abstract class.
 * The model is printed by using printConfiguration().
 */
public class PDHAPrinter extends ToolPrinter
{

	@Option(name = "-time", usage = "reachability time", metaVar = "VAL")
	String time = "auto";

	private PDHABaseComponent ha;

	/**
	 * map from mode string names to numeric ids, starting from 1 and incremented
	 */
	private TreeMap<String, Integer> modeNamesToIds = new TreeMap<String, Integer>();
	

	@Override
	protected String getCommentPrefix()
	{
		return "%";
	}
	
	protected void printDocument(String originalFilename)
	{
		TreeMap<String, String> mapping = new TreeMap<String, String>();
		mapping.put("t", "clock");
		mapping.put("time", "clock_time");

		RenameParams.run(config, mapping);

		this.printCommentHeader();

		Expression.expressionPrinter = new HyCompExpressionPrinter(); // TODO:
																		// move
																		// to
																		// constructor?

		// begin printing the actual program
		printNewline();
		printProcedure();
	}

	/**
	 * Print the actual HyComp code
	 */
	private void printProcedure()
	{
		printVars();

		// printSettings(); // TODO: print to other file or command line call
		
		printInitialConditions();
		
		printInput();

		printComputing();

		printPlotting();

	}
	
	
	private void printVars()
	{
		printLine("function heater_model");


		printComment("Locations are encoded as a variable with a finite (enumeration / set) type.");
		this.increaseIndentation();


		printLine("h = 0.5");
		printLine("delta = 0.5");
		printLine("m = 10/h + 1;" + commentChar + "number of mesh points");
		printLine("t = 10/deltat + 1;" + commentChar + "number of time steps");
		printLine("alpha = deltat/(h*h);");
		
		printLine("A = zeros(m - 2, m - 2);");
			
		
		this.decreaseIndentation();
		
	}	

	private void printInitialConditions()
	{
		printNewline();		
		this.increaseIndentation();
		
		printComment("Intial condition");
		printLine("u0 = ones(m - 2, 1);");
		printLine("u0 = u0 * 0.2;");
																				// figure
																				// out
		this.decreaseIndentation();
	}
	
	private void printInput()
	{
		printNewline();		
		this.increaseIndentation();
		
		printComment("Input function");		
		printLine("function f = input_fun(x)");	
		printLine(indentation + "f = -0.1*x + 1;");
		printLine("end");
		
		printNewline();			
		printLine("load = zeros(m - 2, 1);");
		printLine("u = zeros(m - 2, t);");
		printLine("u(:,1) = u0;");
		
		this.decreaseIndentation();	
	};
	
	
	private void printComputing()
	{
		printNewline();	
		this.increaseIndentation();	

			printLine("for i = 2 : t");
			this.increaseIndentation();

				printLine("for j = 1 : m - 2");	

				this.increaseIndentation();

					printLine("if i == 2");
					printLine(indentationAmount + "load(j, 1) = deltat * input_fun(h * j);");
					printLine("end");

					printLine("if load(j, 1) == 0");
					printLine(indentationAmount + "load(j, 1) = 0;");
					printLine("else");
					printLine(indentationAmount + "load(j, 1) = deltat * input_fun(h * j);");
					printLine("end");

					printComment("switching part");	

					printLine("if u(j, i - 1) > 0.7");
					printLine(indentationAmount + "load(j, 1) = 0;");
					printLine("elseif u(j, i - 1) < 0.4");
					printLine(indentationAmount + "load(j, 1) = deltat * input_fun(h * j);");
					printLine("end");

				this.decreaseIndentation();
				printLine("end");

			printLine("u(:, i) = A * u(:, i - 1) + load;");	
			this.decreaseIndentation();
			printLine("end");

		this.decreaseIndentation();
	
	};	
	
	private void printPlotting()
	{

		printNewline();		
		this.increaseIndentation();
		
		printComment("Plotting");
		printLine("xlist = linspace(0,10,m);");	
		printLine("tlist = linspace(0,10,t);");
		printLine("u = [zeros(1, t); u; zeros(1, t)];");
		printLine("u = u';");
		
		printLine("surf(xlist,tlist,u)");	
		printLine("title('Numerical solution computed with 100 mesh points.')");
		printLine("xlabel('Distance x')");
		printLine("ylabel('Time t')");		

		printLine("figure");	
		printLine("plot(xlist,u(91,:))");
		printLine("title('Solution at t = 9')");
		printLine("xlabel('Distance x')");	
		printLine("ylabel('u(x,5)')");
		
		printLine("figure");	
		printLine("plot(tlist,u(:,5))");
		printLine("title('Solution at x = 4')");
		printLine("xlabel('Time t')");	
		printLine("ylabel('u(0,t)')");	

		
		this.decreaseIndentation();
		
		printLine("end");
	
	};

	




	/**
	 * Get a string representation of the name of the tool, such as "SpaceEx" or "Flow*"
	 * 
	 * @return the name of the tool
	 */
	public abstract String getToolName(){
		
		return "PDHA";
		
	}
		
		
		
	public String getCommandLineFlag(){
	
		return "pdha";
		
	};

	
	protected abstract String getCommentPrefix();

	/**
	 * Should this tool be considered release-quality, which will make it show up in the GUI
	 * 
	 * @return
	 */
	public boolean isInRelease()
	{
		return true;
	}


	/**
	 * Get the default extension for model files for this printer
	 * 
	 * @return the default extension, or null
	 */
	public String getExtension()
	{
		return .m;
	}


}
