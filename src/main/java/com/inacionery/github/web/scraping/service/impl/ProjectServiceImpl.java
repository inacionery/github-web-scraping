package com.inacionery.github.web.scraping.service.impl;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

import com.inacionery.github.web.scraping.model.Project;
import com.inacionery.github.web.scraping.repository.ProjectRepository;
import com.inacionery.github.web.scraping.service.CrawlerService;
import com.inacionery.github.web.scraping.service.ProjectService;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author Inácio Nery
 */
@Service
public class ProjectServiceImpl implements ProjectService {

	@Override
	public Project getProject(String name) {
		return projectRepository.findByName(name);
	}

	@Override
	public void saveProject(String name) {
		queuedNames.computeIfAbsent(name, this::createListenableFutureProject);
	}

	private ListenableFuture<Project> createListenableFutureProject(
		String name) {

		ListenableFuture<Project> future = executorService.submit(
			() -> projectRepository.save(
				new Project(
					crawlerService.getFiles(name), crawlerService.getHash(name),
					name)));

		Futures.addCallback(
			future,
			new FutureCallback<Project>() {

				@Override
				public void onFailure(Throwable throwable) {
					queuedNames.remove(name);
				}

				@Override
				public void onSuccess(Project project) {
					queuedNames.remove(name);
				}

			},
			executorService);

		return future;
	}

	@Autowired
	private CrawlerService crawlerService;

	private final ListeningExecutorService executorService =
		MoreExecutors.listeningDecorator(Executors.newCachedThreadPool());

	@Autowired
	private ProjectRepository projectRepository;

	private final Map<String, ListenableFuture<Project>> queuedNames =
		new ConcurrentHashMap<>();

}