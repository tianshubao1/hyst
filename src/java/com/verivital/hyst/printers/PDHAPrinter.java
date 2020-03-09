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

	private BaseComponent ha;

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

	
	public void setConfig(Configuration c)
	{
		this.config = c;
	}

	protected OutputType outputType = OutputType.STDOUT;
	private PrintStream outputStream; // used if printType = STDOUT or FILE
	private HystFrame outputFrame; // used if printType = GUI
	public StringBuffer outputString; // used if printType = STRING

	// static
	private static DecimalFormat df;

	public void setOutputFile(String filename)
	{
		outputType = OutputType.FILE;
		outputFilename = filename;
	}

	public void setOutputGui(HystFrame frame)
	{
		outputType = OutputType.GUI;
		outputFrame = frame;
	}

	public void setOutputNone()
	{
		outputType = OutputType.NONE;
	}

	public void setOutputString()
	{
		outputType = OutputType.STRING;
		outputString = new StringBuffer();
	}

	/**
	 * Prints the networked automaton out to the given file
	 * 
	 * @param networkedAutomaton
	 *            the automaton to print
	 */
	public void print(Configuration c, String argument, String originalFilename)
	{
		this.originalFilename = originalFilename;

		boolean shouldCloseStream = false;

		setBaseName(originalFilename);

		argument = argument.trim();
		String[] args = AutomatonUtil.extractArgs(argument);

		try
		{
			parser.parseArgument(args);
		}
		catch (CmdLineException e)
		{
			String message = "Error Using Printer for " + getToolName() + ",\n Message: "
					+ e.getMessage() + "\nArguments: '" + argument + "'\n" + getParamHelp();

			throw new CmdLineRuntimeException(message, e);
		}

		try
		{
			outputString = null;
			outputStream = null;

			if (outputType == OutputType.STDOUT)
			{
				outputStream = System.out;
			}
			else if (outputType == OutputType.FILE)
			{
				shouldCloseStream = true;
				outputStream = new PrintStream(
						new BufferedOutputStream(new FileOutputStream(outputFilename)));
			}
			else if (outputType == OutputType.STRING)
				outputString = new StringBuffer();

			this.config = c;

			preconditions.check(c, getToolName());
			printAutomaton();
		}
		catch (PreconditionsFailedException e)
		{
			throw new PreconditionsFailedException(
					"Preconditions for tool " + getToolName() + " failed", e);
		}
		catch (FileNotFoundException e)
		{
			throw new AutomatonExportException("File Not Found", e);
		}
		catch (SecurityException e)
		{
			throw new AutomatonExportException("Security Error", e);
		}
		finally
		{
			if (shouldCloseStream && outputStream != null)
				outputStream.close();
		}
	}

	protected void setBaseName(String originalFilename)
	{
		if (originalFilename == null || originalFilename.length() == 0)
			baseName = "root";
		else
		{
			baseName = new File(originalFilename).getName();
			int i = baseName.lastIndexOf(".");

			if (i != -1)
				baseName = baseName.substring(0, i);
		}
	}

	/**
	 * Print a newline in the output file stream
	 */
	protected void printNewline()
	{
		if (outputType == OutputType.STDOUT || outputType == OutputType.FILE)
			outputStream.println();
		else if (outputType == OutputType.GUI)
			outputFrame.addOutput("\n");
		else if (outputType == OutputType.STRING)
			outputString.append("\n");
	}

	/**
	 * Increase indent while printing
	 */
	protected void increaseIndentation()
	{
		indentation += indentationAmount;
	}

	/**
	 * Decrease indent while printing
	 */
	protected void decreaseIndentation()
	{
		if (indentation.length() > 0)
			indentation = indentation.substring(indentationAmount.length());
	}

	/**
	 * Create a comment block sting from comment text
	 * 
	 * @param text
	 *            the text of the commend
	 * @return the comment string
	 */
	protected String createCommentText(String text)
	{
		return this.indentation + commentChar + " "
				+ text.replace("\n", "\n" + this.indentation + commentChar + " ") + "\n";
	}

	/**
	 * Print several lines of text in a comment block
	 * 
	 * @param comment
	 */
	protected void printCommentBlock(String comment)
	{
		String s = createCommentText(comment);

		if (outputType == OutputType.STDOUT || outputType == OutputType.FILE)
			outputStream.println(s);
		else if (outputType == OutputType.GUI)
			outputFrame.addOutput(s);
		else if (outputType == OutputType.STRING)
			outputString.append(s);
	}

	/**
	 * print a short comment
	 * 
	 * @param comment
	 */
	protected void printComment(String comment)
	{
		printLine(this.commentChar + comment);
	}

	protected String getCommentHeader()
	{
		return "Created by " + Hyst.TOOL_NAME + "\n" + "Hybrid Automaton in " + this.getToolName()
				+ "\n" + "Converted from file: " + originalFilename + "\n"
				+ "Command Line arguments: " + Hyst.programArguments;
	}

	/**
	 * Print header information as a comment with parameters, etc.
	 */
	protected void printCommentHeader()
	{
		printCommentBlock(getCommentHeader());
	}

	protected void printLine(String line)
	{
		this.printLine(line, true);
	}

	protected void printLine(String line, boolean indent)
	{
		if (indent && line.equals(decreaseIndentationString))
			decreaseIndentation();

		String s = "";
		if (indent)
			s += this.indentation;

		s += line;

		if (outputType == OutputType.STDOUT || outputType == OutputType.FILE)
			outputStream.println(s);
		else if (outputType == OutputType.GUI)
			outputFrame.addOutput(s);
		else if (outputType == OutputType.STRING)
			outputString.append(s + "\n");

		if (indent && line.equals("{"))
			increaseIndentation();
	}

	protected void print(String s)
	{
		this.print(s, true);
	}

	protected void print(String s, boolean indent)
	{
		String newS;
		if (indent)
			newS = this.indentation + s;
		else
			newS = s;

		if (outputType == OutputType.STDOUT || outputType == OutputType.FILE)
			outputStream.print(newS);
		else if (outputType == OutputType.GUI)
			outputFrame.addOutput(newS);
		else if (outputType == OutputType.STRING)
			outputString.append(newS);
	}

	/**
	 * Get a string representation of the name of the tool, such as "SpaceEx" or "Flow*"
	 * 
	 * @return the name of the tool
	 */
	public abstract String getToolName();

	/**
	 * Get the command line flag for this tool
	 * 
	 * @return
	 */
	public abstract String getCommandLineFlag();

	/**
	 * Get a single-line comment character for the tool's output format
	 * 
	 * @return
	 */
	protected abstract String getCommentPrefix();

	/**
	 * Should this tool be considered release-quality, which will make it show up in the GUI
	 * 
	 * @return
	 */
	public boolean isInRelease()
	{
		return false;
	}

	public static void initDecimalPrinter()
	{
		df = new DecimalFormat("0.#", new DecimalFormatSymbols(Locale.ENGLISH));
		df.setMaximumFractionDigits(50);
	}

	static
	{
		initDecimalPrinter();
	}

	public static String doubleToString(double n)
	{
		return df.format(n);
	}

	public void flush()
	{
		if (outputType == OutputType.STDOUT || outputType == OutputType.FILE)
			outputStream.flush();
	}

	public String getParamHelp()
	{
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		parser.printUsage(out);

		return out.toString();
	}

	/**
	 * Get the default extension for model files for this printer
	 * 
	 * @return the default extension, or null
	 */
	public String getExtension()
	{
		return null;
	}

	/**
	 * Print the automaton. The configuration is stored in the global config variable.
	 * checkPreconditions() is called before this method, which enforces printer assumptions (for
	 * example, that the model is flat).
	 */
	protected abstract void printAutomaton();
}
