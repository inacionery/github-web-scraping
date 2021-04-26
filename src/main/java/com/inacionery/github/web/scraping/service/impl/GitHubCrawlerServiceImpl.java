package com.inacionery.github.web.scraping.service.impl;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

import com.inacionery.github.web.scraping.model.File;
import com.inacionery.github.web.scraping.service.CrawlerService;

import java.net.HttpURLConnection;
import java.net.URL;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/**
 * @author Inácio Nery
 */
@Service
public class GitHubCrawlerServiceImpl implements CrawlerService {

	@Override
	public List<File> getFiles(String name) {
		return getFiles(
			name, getDocument(getRepositoryURL(name)), getBranch(name));
	}

	@Override
	public String getHash(String name) {
		Document document = getDocument(getRepositoryURL(name) + "/commits/");

		if (document != null) {
			Elements hashElements = document.select(getHashCssQuery(name));

			for (Element hashElement : hashElements) {
				String hash = hashElement.attr("abs:href");

				return hash.substring(hash.indexOf("commit/") + 7);
			}
		}

		return null;
	}

	private ListenableFuture<Document> createListenableFutureProject(
		String url) {

		ListenableFuture<Document> future = executorService.submit(
			() -> gitHubCrawlerServiceImpl.retrieveDocument(url));

		Futures.addCallback(
			future,
			new FutureCallback<Document>() {

				@Override
				public void onFailure(Throwable throwable) {
					queuedURLs.remove(url);
				}

				@Override
				public void onSuccess(Document document) {
					queuedURLs.remove(url);
				}

			},
			executorService);

		return future;
	}

	private BinaryOperator<File> fileBinaryOperator() {
		return (file1, file2) -> new File(
			Stream.of(
				file1.getBytes(), file2.getBytes()
			).filter(
				Objects::nonNull
			).reduce(
				Long::sum
			).orElse(
				null
			),
			file1.getExtension(),
			Stream.of(
				file1.getLines(), file2.getLines()
			).filter(
				Objects::nonNull
			).reduce(
				Long::sum
			).orElse(
				null
			));
	}

	private String getBranch(String name) {
		Document document = getDocument(getRepositoryURL(name));

		if (document == null) {
			return null;
		}

		Element branchElement = document.selectFirst(BRANCH_CSS_QUERY);

		if (branchElement == null) {
			return null;
		}

		return branchElement.text();
	}

	private Long getBytes(String name, String branch, String file) {
		return retry(
			() -> {
				HttpURLConnection httpURLConnection = null;

				try {
					URL url = new URL(
						String.format(GIT_HUB_RAW_URL, name, branch, file));

					httpURLConnection = (HttpURLConnection)url.openConnection();

					httpURLConnection.setRequestMethod("HEAD");

					return httpURLConnection.getContentLengthLong();
				}
				finally {
					if (httpURLConnection != null) {
						httpURLConnection.disconnect();
					}
				}
			},
			() -> null);
	}

	private String getDirectoryCssQuery(String name, String branch) {
		return String.format(DIRECTORY_CSS_QUERY, name, branch);
	}

	private Document getDocument(String url) {
		queuedURLs.computeIfAbsent(url, this::createListenableFutureProject);

		return gitHubCrawlerServiceImpl.retrieveDocumentCache(url);
	}

	private String getExtension(String fileName) {
		if (fileName.indexOf(".") > -1) {
			return fileName.substring(fileName.lastIndexOf(".") + 1);
		}

		return fileName;
	}

	private String getFileCssQuery(String name, String branch) {
		return String.format(FILES_CSS_QUERY, name, branch);
	}

	private List<File> getFiles(String name, Document document, String branch) {
		if (document == null) {
			return Collections.emptyList();
		}

		Map<String, File> files = new HashMap<>();

		Elements directoryElements = document.select(
			getDirectoryCssQuery(name, branch));

		for (Element directoryElement : directoryElements) {
			String directoryURL = directoryElement.attr("abs:href");

			files = files.entrySet(
			).stream(
			).collect(
				Collectors.toMap(
					Map.Entry::getKey, Map.Entry::getValue,
					fileBinaryOperator(),
					() -> getFiles(
						name, getDocument(directoryURL),
						directoryURL.substring(
							directoryURL.indexOf(name + "/tree/") +
								name.length() + 6)
					).stream(
					).collect(
						Collectors.toMap(
							File::getExtension, Function.identity())
					))
			);
		}

		Elements fileElements = document.select(getFileCssQuery(name, branch));

		for (Element fileElement : fileElements) {
			String fileURL = fileElement.attr("abs:href");

			Long lines = getLines(fileURL);

			String fileName = fileElement.attr("title");

			String extension = getExtension(fileName);

			Long bytes = getBytes(name, branch, fileName);

			files.merge(
				extension, new File(bytes, extension, lines),
				fileBinaryOperator());
		}

		return new ArrayList<>(files.values());
	}

	private String getHashCssQuery(String name) {
		return String.format(HASH_CSS_QUERY, name);
	}

	private Long getLines(String fileURL) {
		Document document = getDocument(fileURL);

		if (document != null) {
			Element fileLinesElement = document.selectFirst(
				FILE_LINES_CSS_QUERY);

			if (fileLinesElement != null) {
				String lines = fileLinesElement.text();

				if (lines.indexOf(" lines") > -1) {
					lines = lines.substring(0, lines.indexOf(" lines"));

					if (lines.lastIndexOf(" ") > -1) {
						lines = lines.substring(lines.lastIndexOf(" ") + 1);
					}

					return Long.valueOf(lines);
				}
			}
		}

		return null;
	}

	private String getRepositoryURL(String name) {
		return String.format(GIT_HUB_URL, name);
	}

	@CachePut
	private Document retrieveDocument(String url) {
		return retry(
			() -> Jsoup.connect(
				url
			).timeout(
				3000
			).followRedirects(
				false
			).get(),
			() -> null);
	}

	@Cacheable
	private Document retrieveDocumentCache(String url) {
		return gitHubCrawlerServiceImpl.retrieveDocument(url);
	}

	private <T> T retry(Callable<T> callable, Callable<T> callBack) {
		long deadline = System.currentTimeMillis() + 20000;

		while (true) {
			try {
				try {
					return callable.call();
				}
				catch (Exception e) {
					if (System.currentTimeMillis() > deadline) {
						return callBack.call();
					}
				}

				Thread.sleep(2000);
			}
			catch (Exception e) {
				logger.error("Exception occured : {}", e);
			}
		}
	}

	private static final String BRANCH_CSS_QUERY =
		"#branch-select-menu > summary > span";

	private static final String DIRECTORY_CSS_QUERY =
		"span > a[href^='/%s/tree/%s']";

	private static final String FILE_LINES_CSS_QUERY = "div.text-mono";

	private static final String FILES_CSS_QUERY =
		"span > a[href^='/%s/blob/%s']";

	private static final String GIT_HUB_RAW_URL =
		"https://raw.githubusercontent.com/%s/%s/%s";

	private static final String GIT_HUB_URL = "https://github.com/%s";

	private static final String HASH_CSS_QUERY = "div > a[href^='/%s/commit/']";

	private static final Logger logger = LoggerFactory.getLogger(
		GitHubCrawlerServiceImpl.class);

	private final ListeningExecutorService executorService =
		MoreExecutors.listeningDecorator(Executors.newCachedThreadPool());

	@Autowired
	private GitHubCrawlerServiceImpl gitHubCrawlerServiceImpl;

	private final Map<String, ListenableFuture<Document>> queuedURLs =
		new ConcurrentHashMap<>();

}