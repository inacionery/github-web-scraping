package com.inacionery.github.web.scraping.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Objects;

/**
 * @author Inácio Nery
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class File {

	public File(Long bytes, String extension, Long lines) {
		this.bytes = bytes;
		this.extension = extension;
		this.lines = lines;
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

		File other = (File)obj;

		if (Objects.equals(bytes, other.bytes) &&
			Objects.equals(extension, other.extension) &&
			Objects.equals(lines, other.lines)) {

			return true;
		}

		return false;
	}

	public Long getBytes() {
		return bytes;
	}

	public String getExtension() {
		return extension;
	}

	public Long getLines() {
		return lines;
	}

	@Override
	public int hashCode() {
		return Objects.hash(bytes, extension, lines);
	}

	public void setBytes(Long bytes) {
		this.bytes = bytes;
	}

	public void setExtension(String extension) {
		this.extension = extension;
	}

	public void setLines(Long lines) {
		this.lines = lines;
	}

	private Long bytes;
	private String extension;
	private Long lines;

}