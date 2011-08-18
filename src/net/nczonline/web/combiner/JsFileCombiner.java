package net.nczonline.web.combiner;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

import net.nczonline.web.combiner.JsFileCombiner.JsComparator;


public class JsFileCombiner extends FileCombiner<JsComparator> {

	public static class JsComparator implements Comparator<SourceFile> {

		public int compare(SourceFile s1, SourceFile s2) {
			if (s1.equals(s2)) {
				return 0;
			}
			if (s1.hasDependency(s2)) {
				return 1;
			}
			if(s2.hasDependency(s1)) {
				return -1;
			}
			int d = s1.getDependencySize() - s2.getDependencySize();
			if (d == 0) {
				return s2.getName().compareTo(s1.getName());
			}
			return d;
		}
	}

	public static final String PATH_PREFIX = "src/";

	private static final String
		SKY_JS_NAME = "sky.js",
		SKY_CLASS_NAME = "sky/_kernel/Class.js";

	/**
	 *
	 * @param cfg
	 */
	public JsFileCombiner(Config cfg) {
		super(cfg, PATH_PREFIX);
	}

	@Override
	protected Collection<File> processFile(BufferedReader in, SourceFile sourceFile) throws IOException {
		Set<File> foundDeps = new HashSet<File>(){
			private static final long serialVersionUID = 1L;
			public boolean add(File f) {
				if (f == null) {
					return false;
				}
				return super.add(f);
			}
		};

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
		sourceFile.setContents(fileData.toString());
		return foundDeps;
	}

	private boolean isSkyJs(SourceFile sourceFile) {
		return sourceFile.getName().contains(SKY_JS_NAME);
	}

	private static String f(String require) {
		return require.replace("sky.require(\"", "").replace("\");", "").replaceAll("\\.", "/") + ".js";
	}

	@Override
	protected File getDep(String filename, SourceFile sourceFile) {
		if (isSkyJs(sourceFile)) {
			return null;
		}

		return super.getDep(filename, sourceFile);
	}
}
