package com.inacionery.github.web.scraping.service;

import com.inacionery.github.web.scraping.model.Project;

/**
 * @author Inácio Nery
 */
public interface ProjectService {

	Project getProject(String name);

	Project addProject(String name);

	void updateProject(String name);

}