package com.inacionery.github.web.scraping.service.test;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.reset;

import com.inacionery.github.web.scraping.model.File;
import com.inacionery.github.web.scraping.model.Project;
import com.inacionery.github.web.scraping.repository.ProjectRepository;
import com.inacionery.github.web.scraping.service.ProjectService;

import java.util.Collections;

import org.junit.Assert;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

/**
 * @author Inácio Nery
 */
@SpringBootTest
public class ProjectServiceTest {

	@Test
	public void shouldReturnProject() throws Exception {
		Project project = new Project(
			Collections.singletonList(new File(15835L, 2L, "java", 738L)),
			"c01d29e0b02c33712c73ee6af81150c209b91541",
			"inacionery/github-web-scraping");

		given(
			projectRepository.findByName("inacionery/github-web-scraping")
		).willReturn(
			project
		);

		Assert.assertEquals(
			project,
			projectService.getProject("inacionery/github-web-scraping"));

		reset(projectRepository);
	}

	@MockBean
	private ProjectRepository projectRepository;

	@Autowired
	private ProjectService projectService;

}