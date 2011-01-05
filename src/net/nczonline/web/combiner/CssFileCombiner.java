package net.nczonline.web.combiner;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class CssFileCombiner extends FileCombiner {

	private static String PATH_PREFIX = "resources/themes/";

	public CssFileCombiner(Config cfg) {
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

		StringBuffer fileData = new StringBuffer();
		String line = null;
		while((line = in.readLine()) != null) {
			if (line.startsWith("@import url("))  {
				String filename = f(line);
				foundDeps.add(getDep(filename, sourceFile));
			} else {
				fileData.append(line).append("\n");
			}
		}
		sourceFile.setContents(fileData.toString());
		return foundDeps;
	}

	private static String f(String im) {
		return im.replaceAll("@import url\\(\"(.*?)\"\\);", "$1");
	}
}
