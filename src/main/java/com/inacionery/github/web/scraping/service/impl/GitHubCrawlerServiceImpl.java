package com.inacionery.github.web.scraping.service.impl;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

import com.inacionery.github.web.scraping.model.File;
import com.inacionery.github.web.scraping.service.CrawlerService;

import java.net.HttpURLConnection;
import java.net.URL;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.HttpStatus;
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
			file1.getCount() + file2.getCount(), file1.getExtension(),
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

	private <T> T getContent(String url, Function<Document, T> function) {
		return function.apply(getDocument(url));
	}

	private String getDirectoryCssQuery(String name, String branch) {
		return String.format(DIRECTORY_CSS_QUERY, name, branch);
	}

	private List<File> getDirectoryFiles(
		String name, Document document, String branch) {

		return document.select(
			getDirectoryCssQuery(name, branch)
		).parallelStream(
		).map(
			directoryElement -> {
				String directoryURL = directoryElement.attr("abs:href");

				return getFiles(
					name, getDocument(directoryURL),
					directoryURL.substring(
						directoryURL.indexOf(name + "/tree/") + name.length() +
							6)
				).parallelStream(
				).collect(
					Collectors.toList()
				);
			}
		).flatMap(
			List::parallelStream
		).collect(
			Collectors.toList()
		);
	}

	private Document getDocument(String url) {
		return retry(
			() -> {
				try {
					return Jsoup.connect(
						url
					).timeout(
						3000
					).followRedirects(
						false
					).get();
				}
				catch (HttpStatusException e) {
					if (e.getStatusCode() == HttpStatus.NOT_FOUND.value()) {
						return null;
					}

					throw e;
				}
			},
			() -> null);
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

		ListenableFuture<List<File>> directoryFilesFuture =
			executorService.submit(
				() -> getDirectoryFiles(name, document, branch));

		ListenableFuture<List<File>> treeFilesFuture = executorService.submit(
			() -> getTreeFiles(name, document, branch));

		try {
			return Stream.concat(
				directoryFilesFuture.get(
				).parallelStream(),
				treeFilesFuture.get(
				).parallelStream()
			).collect(
				Collectors.collectingAndThen(
					Collectors.toMap(
						File::getExtension, Function.identity(),
						fileBinaryOperator()),
					map -> new ArrayList<>(map.values()))
			);
		}
		catch (ExecutionException | InterruptedException e) {
			return Collections.emptyList();
		}
	}

	private String getHashCssQuery(String name) {
		return String.format(HASH_CSS_QUERY, name);
	}

	private Long getLines(String fileURL) {
		return retry(
			() -> getContent(
				fileURL,
				document -> {
					Element fileLinesElement = document.selectFirst(
						FILE_LINES_CSS_QUERY);

					if (fileLinesElement != null) {
						String lines = fileLinesElement.text();

						if (lines.indexOf(" lines") > -1) {
							lines = lines.substring(0, lines.indexOf(" lines"));

							if (lines.lastIndexOf(" ") > -1) {
								lines = lines.substring(
									lines.lastIndexOf(" ") + 1);
							}

							return Long.valueOf(lines);
						}
						else if ((lines.indexOf("Bytes") > -1) ||
								 (lines.indexOf("KB") > -1) ||
								 (lines.indexOf("MB") > -1)) {

							return null;
						}
					}

					throw new RuntimeException();
				}),
			() -> null);
	}

	private String getRepositoryURL(String name) {
		return String.format(GIT_HUB_URL, name);
	}

	private List<File> getTreeFiles(
		String name, Document document, String branch) {

		return document.select(
			getFileCssQuery(name, branch)
		).parallelStream(
		).map(
			fileElement -> {
				ListenableFuture<Long> linesFuture = executorService.submit(
					() -> getLines(fileElement.attr("abs:href")));

				String fileName = fileElement.attr("title");

				ListenableFuture<Long> bytesFuture = executorService.submit(
					() -> getBytes(name, branch, fileName));

				try {
					return new File(
						bytesFuture.get(), 1L, getExtension(fileName),
						linesFuture.get());
				}
				catch (ExecutionException | InterruptedException exception) {
					throw new RuntimeException(exception);
				}
			}
		).collect(
			Collectors.toList()
		);
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

				Thread.sleep(100);
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

}