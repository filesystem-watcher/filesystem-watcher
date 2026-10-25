package io.github.filesystemwatcher;

import io.github.filesystemwatcher.utilities.AwaitilityUtils;
import io.github.filesystemwatcher.utilities.FilesystemUtils;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class ConstraintsTest {

	private static Path temporaryDirectory;

	@BeforeAll
	static void setUp(@TempDir Path tempDirectory) {
		temporaryDirectory = tempDirectory;
	}

	private static class ConstraintTestSetup {
		private final Path firstDirectory;
		private final Path secondDirectory;
		private final Path thirdDirectory;
		private final Path firstFile;
		private final Path secondFile;
		private final Path thirdFile;
		private final Path link;

		private final Path recursive;
		private final Path firstRecursiveDirectory;
		private final Path secondRecursiveDirectory;
		private final Path firstRecursiveFile;
		private final Path secondRecursiveFile;
		private final Path recursiveLink;

		ConstraintTestSetup(Path temporaryDirectory) {
			this.firstDirectory = FilesystemUtils.createDirectory(temporaryDirectory, "first_directory");
			this.secondDirectory = FilesystemUtils.createDirectory(temporaryDirectory, "second_directory");
			this.thirdDirectory = FilesystemUtils.createDirectory(temporaryDirectory, "third_directory");
			this.firstFile = FilesystemUtils.createFile(temporaryDirectory, "firstFile.txt");
			this.secondFile = FilesystemUtils.createFile(temporaryDirectory, "secondFile.xls");
			this.thirdFile = FilesystemUtils.createFile(temporaryDirectory, "thirdFile.txt");
			this.link = FilesystemUtils.createLink(thirdDirectory, "link", temporaryDirectory);

			this.recursive = FilesystemUtils.createDirectory(temporaryDirectory, "recursive_directory");
			this.firstRecursiveDirectory = FilesystemUtils.createDirectory(recursive, "first_recursive_directory");
			this.secondRecursiveDirectory = FilesystemUtils.createDirectory(recursive, "second_recursive_directory");
			this.firstRecursiveFile = FilesystemUtils.createFile(recursive, "firstFile.txt");
			this.secondRecursiveFile = FilesystemUtils.createFile(recursive, "secondFile.xls");
			this.recursiveLink = FilesystemUtils.createLink(secondRecursiveDirectory, "recursiveLink", recursive);
		}

		List<Path> getAll() {
			return Arrays.asList(firstDirectory,
					secondDirectory,
					thirdDirectory,
					firstFile,
					secondFile,
					thirdFile,
					link,
					recursive,
					firstRecursiveFile,
					secondRecursiveFile,
					firstRecursiveDirectory,
					secondRecursiveDirectory,
					recursiveLink
			);
		}

		List<Path> getAllNotRecursive() {
			return Arrays.asList(
					firstFile, secondFile, thirdFile, firstDirectory, secondDirectory, thirdDirectory, recursive, link
			);
		}
	}

	@ParameterizedTest
	@MethodSource
	void shouldFindProperFilesForCorrectConstraints(FilesystemConstraints constraints, List<Path> expectedPaths) {
		List<FilesystemEvent> receivedEvents = Collections.synchronizedList(new ArrayList<>());

		FilesystemMonitor monitor = FilesystemMonitor.builder()
				.watchedPath(temporaryDirectory)
				.watchedConsumer(receivedEvents::add)
				.watchedConstraints(constraints)
				.build();

		monitor.startWatching();
		try {
			List<FilesystemEvent> expectedEvents = expectedPaths.stream()
					.map(path -> FilesystemEvent.of(path, FilesystemEventType.INITIAL))
					.collect(Collectors.toList());
			expectedEvents.forEach(event -> AwaitilityUtils.awaitForElement(receivedEvents, event));
			Assertions.assertThat(initialEvents(receivedEvents)).containsExactlyInAnyOrderElementsOf(expectedEvents);
		} finally {
			monitor.stopWatching();
		}
	}

	private static List<FilesystemEvent> initialEvents(List<FilesystemEvent> events) {
		synchronized (events) {
			return events.stream()
					.filter(event -> event.getEventType() == FilesystemEventType.INITIAL)
					.collect(Collectors.toList());
		}
	}

	private static Stream<Arguments> shouldFindProperFilesForCorrectConstraints() {
		ConstraintTestSetup setup = new ConstraintTestSetup(temporaryDirectory);

		return Stream.of(
				Arguments.of(
						FilesystemConstraints.DEFAULT,
						setup.getAllNotRecursive()
				),
				Arguments.of(
						FilesystemConstraints.DEFAULT.withRecursive(true),
						setup.getAll()
				),
				Arguments.of(
						FilesystemConstraints.DEFAULT.withFilenameSubstrings(Arrays.asList("first")),
						Arrays.asList(setup.firstFile, setup.firstDirectory)
				),
				Arguments.of(
						FilesystemConstraints.DEFAULT.withFilenamePatterns(Arrays.asList(Pattern.compile("^first.*"))),
						Arrays.asList(setup.firstFile, setup.firstDirectory)
				),
				Arguments.of(
						FilesystemConstraints.DEFAULT.withRecursive(true).withFilenameSubstrings(Arrays.asList("first")),
						Arrays.asList(setup.firstFile, setup.firstDirectory, setup.firstRecursiveFile, setup.firstRecursiveDirectory)
				),
				Arguments.of(
						FilesystemConstraints.DEFAULT.withRecursive(true).withFilenamePatterns(Arrays.asList(Pattern.compile("^first.*"))),
						Arrays.asList(setup.firstFile, setup.firstDirectory, setup.firstRecursiveFile, setup.firstRecursiveDirectory)
				),
				Arguments.of(
						FilesystemConstraints.DEFAULT.withFileTypes(Arrays.asList(FilesystemConstraints.FileType.DIRECTORY)),
						Arrays.asList(setup.firstDirectory, setup.secondDirectory, setup.thirdDirectory, setup.recursive)
				),
				Arguments.of(
						FilesystemConstraints.DEFAULT.withRecursive(true).withFileTypes(Arrays.asList(FilesystemConstraints.FileType.DIRECTORY)),
						Arrays.asList(setup.firstDirectory, setup.secondDirectory, setup.thirdDirectory, setup.recursive, setup.firstRecursiveDirectory, setup.secondRecursiveDirectory)
				)
		);
	}

	@Test
	void shouldUseDefaultConstraintsWhenNoneAreGiven(@TempDir Path directory) {
		Path file = FilesystemUtils.createFile(directory, "file.txt");
		List<FilesystemEvent> receivedEvents = Collections.synchronizedList(new ArrayList<>());

		FilesystemMonitor monitor = FilesystemMonitor.builder()
				.watchedPath(directory)
				.watchedConsumer(receivedEvents::add)
				.build();

		monitor.startWatching();
		try {
			AwaitilityUtils.awaitForElement(receivedEvents, FilesystemEvent.of(file, FilesystemEventType.INITIAL));
		} finally {
			monitor.stopWatching();
		}
	}
}
