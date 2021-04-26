package com.inacionery.github.web.scraping.repository.impl;

import com.inacionery.github.web.scraping.model.Project;
import com.inacionery.github.web.scraping.repository.ProjectRepository;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

/**
 * @author Inácio Nery
 */
@Component
public class ProjectRepositoryImpl implements ProjectRepository {

	@Override
	public Project findByName(String name) {
		return projects.get(name);
	}

	@Override
	public Project save(Project project) {
		projects.put(project.getName(), project);

		return project;
	}

	private final Map<String, Project> projects = new ConcurrentHashMap<>();

}