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

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;

/*
 * This file heavily inspired and based on YUI Compressor.
 * http://github.com/yui/yuicompressor
 */

public class Combiner {

	public static final String PATH_PREFIX = "src/";

	/**
	 * @param args the command line arguments
	 */
	public static void main(String[] args) {

		Config cfg = new Config();
		CmdLineParser parser = new CmdLineParser(cfg);

		try {
			//parse the arguments
			parser.parseArgument(args);

			String charset = cfg.getCharset();
			if (charset == null || !Charset.isSupported(charset)) {
				charset = System.getProperty("file.encoding");
				if (charset == null) {
					charset = "UTF-8";
				}
				if (cfg.isVerbose()) {
					System.err.println("[INFO] Using charset " + charset);
				}
			}
			cfg.setCharset(charset);

			File outputFile = cfg.getOutputFile();
			Writer out = null;
			if (outputFile == null) {
				out = new OutputStreamWriter(System.out, cfg.getCharset());
			} else {
				if (cfg.isVerbose()){
					System.err.println("[INFO] Output file is '" + outputFile.getAbsolutePath() + "'");
				}
				out = new OutputStreamWriter(new FileOutputStream(outputFile), cfg.getCharset());
			}

			FileCombiner combiner = new FileCombiner(cfg);
			combiner.combine(cfg.getArguments().toArray(new String[0]), out);


		} catch (CmdLineException e) {
			System.err.println(e.getMessage());
			System.err.println("Usage: java -jar combiner-x.y.z.jar [options] [input files]\n");
			parser.printUsage(System.err);
			return;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
