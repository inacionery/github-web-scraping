package com.inacionery.github.web.scraping.model;

import java.util.List;
import java.util.Objects;

/**
 * @author Inácio Nery
 */
public class Project {

	public Project(List<File> files, String hash, String name) {
		this.files = files;
		this.hash = hash;
		this.name = name;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}

		if (obj == null) {
			return false;
		}

		if (getClass() != obj.getClass()) {
			return false;
		}

		Project other = (Project)obj;

		if (Objects.equals(files, other.files) &&
			Objects.equals(hash, other.hash) &&
			Objects.equals(name, other.name)) {

			return true;
		}

		return false;
	}

	public List<File> getFiles() {
		return files;
	}

	public String getHash() {
		return hash;
	}

	public String getName() {
		return name;
	}

	@Override
	public int hashCode() {
		return Objects.hash(files, hash, name);
	}

	public void setFiles(List<File> files) {
		this.files = files;
	}

	public void setHash(String hash) {
		this.hash = hash;
	}

	public void setName(String name) {
		this.name = name;
	}

	private List<File> files;
	private String hash;
	private String name;

}