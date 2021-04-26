package com.inacionery.github.web.scraping.service;

import com.inacionery.github.web.scraping.model.File;

import java.util.List;

/**
 * @author Inácio Nery
 */
public interface CrawlerService {

	List<File> getFiles(String name);

	String getHash(String name);

}