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


import java.util.HashSet;
import java.util.Set;

public class SourceFile {

	private final String name;
	private final Set<SourceFile> dependencies = new HashSet<SourceFile>();
	private String contents = null;

	/**
	 * Creates a new SourceFile based on a file.
	 * @param name
	 */
	public SourceFile(String name){
		this.name = name;
	}

	public String getName(){
		return name;
	}

	public String getContents() {
		return contents;
	}

	public void setContents(String contents) {
		this.contents = contents;
	}

	public boolean addDependency(SourceFile dependency) {
		if (dependency.hasDependency(this)) {
			return false;
		}
		dependencies.add(dependency);
		return true;
	}

	public int getDependencySize(){
		return dependencies.size();
	}

	public boolean hasDependencies(){
		return !dependencies.isEmpty();
	}

	public boolean hasDependency(SourceFile s) {
		return dependencies.contains(s);
	}

	/**
	 * if there's no contents, then it needs to be processed
	 * if it already has dependencies, then it's already been processed
	 * (prevents infinite loop if a circular dependency is detected)
	 *
	 * @return
	 */
	public boolean isReady() {
		return getContents() != null || hasDependencies();
	}

	@Override
	public int hashCode() {
		return 31*getName().hashCode();
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || !(o instanceof SourceFile)) {
			return false;
		}
		SourceFile s = (SourceFile)o;
		if (this == s || getName().equals(s.getName())) {
			return true;
		}
		return false;
	}

	@Override
	public String toString() {
		return getName();
	}
}
