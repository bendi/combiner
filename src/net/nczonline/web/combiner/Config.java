package net.nczonline.web.combiner;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;

public class Config {
	@Option(name="-v", aliases={"--verbose"}, usage="Display informational messages and warnings")
	private boolean verbose = false;

	@Option(name="-s", aliases={"--separator"}, usage="Output a separator between combined files")
	private boolean separator = false;

	@Option(name="-charset", usage="Read the input file using <charset>")
	private String charset = null;

	@Option(name="-o", aliases={"--output"}, usage="Place the output into <file>. Defaults to stdout.")
	private File outputFile = null;

	@Argument
	private List<String> arguments = new ArrayList<String>();

	public List<String> getArguments() {
		return arguments;
	}

	public void setArguments(List<String> arguments) throws IOException {
		this.arguments = arguments;
	}
	public boolean isVerbose() {
		return verbose;
	}
	public void setVerbose(boolean verbose) {
		this.verbose = verbose;
	}
	public boolean isSeparator() {
		return separator;
	}
	public void setSeparator(boolean separator) {
		this.separator = separator;
	}
	public String getCharset() {
		return charset;
	}
	public void setCharset(String charset) {
		this.charset = charset;
	}
	public File getOutputFile() {
		return outputFile;
	}
	public void setOutputFile(File outputFile) {
		this.outputFile = outputFile;
	}

}
