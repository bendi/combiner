package net.nczonline.web.combiner;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import net.nczonline.web.combiner.CssFileCombiner.CssComparator;


public class CssFileCombiner extends FileCombiner<CssComparator> {

	public static class CssComparator implements Comparator<SourceFile> {

		public int compare(SourceFile o1, SourceFile o2) {
			if (o1.equals(o2)) {
				return 0;
			}
			return 1;
		}

	}

	private static String PATH_PREFIX = "resources/themes/";

	public CssFileCombiner(Config cfg) {
		super(cfg, PATH_PREFIX);
	}

	@Override
	protected Collection<File> processFile(BufferedReader in, SourceFile sourceFile) throws IOException {
		List<File> foundDeps = new ArrayList<File>(){
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

	@Override
	protected Collection<SourceFile> preprocessBeforeOutput(Collection<SourceFile> col) {
		List<SourceFile> list = new ArrayList<SourceFile>(col);
		return list.subList(1, list.size());
	}
}
