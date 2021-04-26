package com.inacionery.github.web.scraping.controller;

import com.inacionery.github.web.scraping.model.Project;
import com.inacionery.github.web.scraping.service.CrawlerService;
import com.inacionery.github.web.scraping.service.ProjectService;

import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Inácio Nery
 */
@RequestMapping("/files")
@RestController
public class FileController {

	@GetMapping
	@ResponseBody
	public ResponseEntity<?> getFiles(
		@RequestParam(required = true) String name) {

		String hash = crawlerService.getHash(name);

		if (hash == null) {
			return new ResponseEntity<>(
				name + " repository does not exist.", HttpStatus.BAD_REQUEST);
		}

		Project project = _projectService.getProject(name);

		if ((project != null) && Objects.equals(hash, project.getHash())) {
			return new ResponseEntity<>(project.getFiles(), HttpStatus.OK);
		}

		_projectService.saveProject(name);

		return new ResponseEntity<>("Scraping on " + name, HttpStatus.ACCEPTED);
	}

	@Autowired
	private ProjectService _projectService;

	@Autowired
	private CrawlerService crawlerService;

}