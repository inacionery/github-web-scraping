package com.inacionery.github.web.scraping.controller.test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasSize;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.inacionery.github.web.scraping.model.File;
import com.inacionery.github.web.scraping.model.Project;
import com.inacionery.github.web.scraping.service.CrawlerService;
import com.inacionery.github.web.scraping.service.ProjectService;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import org.mockito.internal.verification.VerificationModeFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * @author Inácio Nery
 */
@AutoConfigureMockMvc
@SpringBootTest
public class FileControllerTest {

	@Test
	public void shouldReturnAccepted() throws Exception {
		given(
			crawlerService.getHash("inacionery/github-web-scraping")
		).willReturn(
			"c01d29e0b02c33712c73ee6af81150c209b91541"
		);

		given(
			projectService.getProject("inacionery/github-web-scraping")
		).willReturn(
			null
		);

		mockMvc.perform(
			get(
				"/files"
			).param(
				"name", "inacionery/github-web-scraping"
			).contentType(
				MediaType.APPLICATION_JSON
			)
		).andExpect(
			status().isAccepted()
		);

		verify(
			projectService, VerificationModeFactory.times(1)
		).getProject(
			"inacionery/github-web-scraping"
		);

		reset(crawlerService);
		reset(projectService);
	}

	@Test
	public void shouldReturnBadRequest() throws Exception {
		given(
			crawlerService.getHash("inacionery/github-web-scraping")
		).willReturn(
			null
		);

		mockMvc.perform(
			get(
				"/files"
			).param(
				"name", "inacionery/github-web-scraping"
			).contentType(
				MediaType.APPLICATION_JSON
			)
		).andExpect(
			status().isBadRequest()
		);

		reset(crawlerService);
	}

	@Test
	public void shouldReturnOk() throws Exception {
		Project project = new Project(
			Collections.singletonList(new File(15835L, "java", 738L)),
			"c01d29e0b02c33712c73ee6af81150c209b91541",
			"inacionery/github-web-scraping");

		given(
			crawlerService.getHash("inacionery/github-web-scraping")
		).willReturn(
			"c01d29e0b02c33712c73ee6af81150c209b91541"
		);

		given(
			projectService.getProject("inacionery/github-web-scraping")
		).willReturn(
			project
		);

		mockMvc.perform(
			get(
				"/files"
			).param(
				"name", "inacionery/github-web-scraping"
			).contentType(
				MediaType.APPLICATION_JSON
			)
		).andExpect(
			status().isOk()
		).andExpect(
			jsonPath("$", hasSize(1))
		).andExpect(
			jsonPath("$[0].bytes", is(15835))
		).andExpect(
			jsonPath("$[0].extension", is("java"))
		).andExpect(
			jsonPath("$[0].lines", is(738))
		);

		verify(
			projectService, VerificationModeFactory.times(1)
		).getProject(
			"inacionery/github-web-scraping"
		);

		reset(crawlerService);
		reset(projectService);
	}

	@MockBean
	private CrawlerService crawlerService;

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private ProjectService projectService;

}