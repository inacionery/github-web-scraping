package com.inacionery.github.web.scraping.repository;

import com.inacionery.github.web.scraping.model.Project;

/**
 * @author Inácio Nery
 */
public interface ProjectRepository {

	Project findByName(String name);

	Project save(Project project);

}