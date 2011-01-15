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

public abstract class FileCombiner {

	private Map<String, SourceFile> sourceFiles = new HashMap<String, SourceFile>();
	private final Config cfg;
	private final String pathPrefix;

	/**
	 *
	 * @param cfg
	 * @param pathPrefix
	 */
	public FileCombiner(Config cfg, String pathPrefix) {
		this.cfg = cfg;
		this.pathPrefix = pathPrefix;
	}

	/**
	 * Combines a list of files and outputs the result onto the given writer.
	 * @param files The files to combine.
	 * @param out
	 * @throws IOException
	 */
	public void combine(File[] files, Writer out) throws IOException{
		Collection<SourceFile> sourceFiles = processSourceFiles(Arrays.asList(files));
		writeToOutput(sourceFiles, out);
	}

	/**
	 * Combines a list of files and outputs the result onto the given writer.
	 * @param filenames The filenames of the files to combine.
	 * @param out
	 * @throws IOException
	 */
	public void combine(String[] filenames, Writer out) throws IOException {
		File[] files = getFiles(filenames);
		combine(files, out);
	}

	/**
	 * Gets sorted collection with dependencies
	 *
	 * It calls itself recursively in order to check files list found by {@link #processSourceFile(SourceFile)}
	 *
	 * @param files
	 * @return
	 * @throws IOException
	 */
	private Collection<SourceFile> processSourceFiles(Collection<File> files) throws IOException {
		for(File depFile : files) {
			//get a source file object
			SourceFile depSourceFile = getSourceFile(depFile);

			if (!depSourceFile.isReady()){
				Collection<File> foundDeps = processSourceFile(depSourceFile);
				processSourceFiles(foundDeps);
			}
		}
		return new TreeSet<SourceFile>(sourceFiles.values());
	}

	/**
	 * Simple accessor - makes sure that element is inserted only when it doesn't exist
	 *
	 * @param file
	 * @return
	 */
	private SourceFile getSourceFile(File file) {
		String name = file.getAbsolutePath();
		if (!sourceFiles.containsKey(name)){
			sourceFiles.put(name, new SourceFile(name));
		}
		return sourceFiles.get(name);
	}

	/**
	 * This method starts file reader and calls {@link #processFile(BufferedReader, SourceFile)}
	 *
	 * @param sourceFile
	 * @return collection of files in case during analysis there were some dependencies found
	 * @throws IOException
	 */
	private Collection<File> processSourceFile(SourceFile sourceFile) throws IOException {
		BufferedReader in = null;
		try {
			in = new BufferedReader(new InputStreamReader(new FileInputStream(sourceFile.getName()), cfg.getCharset()));

			log("Processing file '" + sourceFile.getName() + "'");

			Collection<File> foundDeps = processFile(in, sourceFile);
			if (foundDeps.isEmpty()) {
				log("... No dependencies found");
			}
			return foundDeps;
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch(Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * This method should acually do the job.
	 *
	 * @param in
	 * @param sourceFile
	 * @return
	 * @throws IOException
	 */
	protected abstract Collection<File> processFile(BufferedReader in, SourceFile sourceFile) throws IOException;

	/**
	 * Checks file existence and circular dependency
	 *
	 * In case file given by {@link #pathPrefix} and filename doesn't exist it quits with exit(1)
	 *
	 * In case sourceFile is already a dependency of filename it leaves.
	 *
	 * @param filename
	 * @param sourceFile
	 * @return
	 */
	protected File getDep(String filename, SourceFile sourceFile) {
		File depFile = new File(pathPrefix + filename);

		//verify that the file actually exists
		if (!depFile.isFile()){
			throw new IllegalStateException("Dependency file not found: '" + filename + "'");
		}

		log("... has dependency on " + filename);
		//get a source file object
		SourceFile depSourceFile = getSourceFile(depFile);

		if (!sourceFile.addDependency(depSourceFile)) {
			throw new IllegalStateException("Circular dependencies: '" + depSourceFile.getName() + "' and '" + sourceFile.getName() + "'");
		}
		//if there's no contents, then it needs to be processed
		if (depSourceFile.getContents() == null){
			return depFile;
		}

		return null;
	}

	/**
	 *
	 * @param finalFiles
	 * @param out
	 * @throws IOException
	 */
	private void writeToOutput(Collection<SourceFile> finalFiles, Writer out) throws IOException {
		boolean separator = cfg.isSeparator();
		for(SourceFile finalFile : finalFiles) {
			log("Adding '" + finalFile.getName() + "' to output.");
			if (separator){
				out.write("\n/*------" + finalFile.getName() + "------*/\n");
			}
			out.write(finalFile.getContents());
		}
	}

	/**
	 *
	 * @param filenames
	 * @return
	 */
	private File[] getFiles(String[] filenames) {
		Set<File> files = new HashSet<File>();

		for(String filename : filenames) {
			File file = new File(pathPrefix + filename);
			if (file.isFile()){
				files.add(file);
				log("Adding file '" + file.getAbsolutePath() + "'");
			} else {
				error("Cannot find file '" + file.getAbsolutePath() + "'");
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
