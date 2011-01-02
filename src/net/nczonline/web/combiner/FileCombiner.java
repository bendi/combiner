/*
 * Copyright (c) 2009 Nicholas C. Zakas. All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package net.nczonline.web.combiner;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class FileCombiner {

	private static final String
		SKY_JS_NAME = "sky.js",
		SKY_CLASS_NAME = "sky/_kernel/Class.js";

	private Map<String, SourceFile> sourceFiles = new HashMap<String, SourceFile>();
	private final Config cfg;

	public FileCombiner(Config cfg) {
		this.cfg = cfg;
	}

	/**
	 * Combines a list of files and outputs the result onto the given writer.
	 * @param files The files to combine.
	 * @param out
	 */
	public void combine(File[] files, Writer out){
		Collection<SourceFile> foundFiles = processSourceFiles(Arrays.asList(files));
		Collection<SourceFile> finalFiles = constructFileList(foundFiles);
		writeToOutput(finalFiles, out);
	}

	/**
	 * Combines a list of files and outputs the result onto the given writer.
	 * @param filenames The filenames of the files to combine.
	 * @param out
	 */
	public void combine(String[] filenames, Writer out) {
		File[] files = getFiles(filenames);
		combine(files, out);
	}

	private Collection<SourceFile> processSourceFiles(Collection<File> files) {
		for(File depFile : files) {
			//get a source file object
			SourceFile depSourceFile = getSourceFile(depFile);

			if (!depSourceFile.isReady()){
				Collection<File> foundDeps = processSourceFile(depSourceFile);
				processSourceFiles(foundDeps);
			}
		}
		return sourceFiles.values();
	}

	private SourceFile getSourceFile(File file) {
		String name = file.getAbsolutePath();
		if (!sourceFiles.containsKey(name)){
			sourceFiles.put(name, new SourceFile(name));
		}
		return sourceFiles.get(name);
	}

	private Collection<File> processSourceFile(SourceFile sourceFile) {
		Set<File> foundDeps = new HashSet<File>(){
			private static final long serialVersionUID = 1L;
			public boolean add(File f) {
				if (f == null) {
					return false;
				}
				return super.add(f);
			}
		};

		try {
			BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(sourceFile.getName()), cfg.getCharset()));

			log("Processing file '" + sourceFile.getName() + "'");

			// all files have implicit dependency on sky.js
			foundDeps.add(getDep(SKY_JS_NAME, sourceFile));

			StringBuffer fileData = new StringBuffer();
			String line = null;
			boolean skipRequire = false;
			while((line = in.readLine()) != null) {
				if (skipRequire) {
					if (line.equals("};")) {
						fileData.append("sky.require = function(){};\n");
						skipRequire = false;
					}
				} else if (line.startsWith("sky.require("))  {
					String filename = f(line);
					foundDeps.add(getDep(filename, sourceFile));
				} else if(line.contains("new sky.Class(")) {
					foundDeps.add(getDep(SKY_CLASS_NAME, sourceFile));
					fileData.append(line).append("\n");
				} else if(line.contains("sky.require = function")) {
					skipRequire = true;
				} else if(line.contains("sky.require")) {
					// skip sky.require calls
				} else {
					fileData.append(line).append("\n");
				}
			}
			if (foundDeps.isEmpty()) {
				log("... No dependencies found");
			}
			sourceFile.setContents(fileData.toString());
			in.close();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
		return foundDeps;
	}

	private static String f(String require) {
		return require.replace("sky.require(\"", "").replace("\");", "").replaceAll("\\.", "/") + ".js";
	}

	private File getDep(String filename, SourceFile sourceFile) {
		File depFile = new File(Combiner.PATH_PREFIX + filename);

		//verify that the file actually exists
		if (!depFile.isFile()){
			error("Dependency file not found: '" + filename + "'");
			System.exit(1);
		}

		if (isSkyJs(sourceFile)) {
			return null;
		}

		log("... has dependency on " + filename);
		//get a source file object
		SourceFile depSourceFile = getSourceFile(depFile);

		sourceFile.addDependency(depSourceFile);
		//if there's no contents, then it needs to be processed
		if (depSourceFile.getContents() == null){
			return depFile;
		}

		return null;
	}

	private boolean isSkyJs(SourceFile sourceFile) {
		return sourceFile.getName().contains(SKY_JS_NAME);
	}

	private Collection<SourceFile> constructFileList(Collection<SourceFile> foundFiles){
		SourceFile[] files = foundFiles.toArray(new SourceFile[0]);

		//check for circular references
		for (int i=0; i < files.length; i++){

			log("Verifying dependencies of '" + files[i].getName() + "'");

			for (int j=i+1; j < files.length; j++){

				boolean dependsOn = files[i].hasDependency(files[j]);
				boolean isDependencyOf = files[j].hasDependency(files[i]);

				if (dependsOn && isDependencyOf){
					error("Circular dependencies: '" + files[i].getName() + "' and '" + files[j].getName() + "'");
					System.exit(1);
				}
			}
		}

		return new TreeSet<SourceFile>(foundFiles);
	}

	private void writeToOutput(Collection<SourceFile> finalFiles, Writer out){
		try {
			boolean separator = cfg.isSeparator();
			for(SourceFile finalFile : finalFiles) {
				log("Adding '" + finalFile.getName() + "' to output.");
				if (separator){
					out.write("\n/*------" + finalFile.getName() + "------*/\n");
				}
				out.write(finalFile.getContents());
			}
		} catch (IOException ex) {
			ex.printStackTrace();
			System.exit(1);
		}
	}

	private File[] getFiles(String[] filenames) {
		Set<File> files = new HashSet<File>();

		for(String filename : filenames) {
			File file = new File(Combiner.PATH_PREFIX + filename);
			if (file.isFile()){
				files.add(file);
				log("Adding file '" + file.getAbsolutePath() + "'");
			} else {
				error("Couldn't find file '" + filename + "'");
			}
		}
		return files.toArray(new File[0]);

	}

	private void log(String s) {
		if(cfg.isVerbose()) System.out.println("[INFO] " + s);
	}
	private void error(String s) {
		System.err.println("[ERROR] " + s);
	}
}
