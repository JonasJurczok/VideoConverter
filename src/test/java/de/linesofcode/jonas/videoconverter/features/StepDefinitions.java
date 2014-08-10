package de.linesofcode.jonas.videoconverter.features;

import com.google.common.collect.Lists;
import cucumber.api.DataTable;
import cucumber.api.PendingException;
import cucumber.api.java.After;
import cucumber.api.java.Before;
import cucumber.api.java.en.And;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import de.linesofcode.jonas.videoconverter.BooleanAwareProperties;
import de.linesofcode.jonas.videoconverter.VideoConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static de.linesofcode.jonas.videoconverter.BooleanAwareProperties.Properties.INPUT_DIRECTORY;
import static de.linesofcode.jonas.videoconverter.BooleanAwareProperties.Properties.OUTPUT_DIRECTORY;
import static java.nio.file.FileVisitResult.CONTINUE;
import static java.nio.file.Files.copy;
import static java.nio.file.Files.createDirectories;
import static java.nio.file.Files.walkFileTree;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public class StepDefinitions {

	private static final Logger LOG = LoggerFactory.getLogger(StepDefinitions.class);

	private BooleanAwareProperties properties;
	private VideoConverter converter;

	@Before
	public void setUp() {
		properties = new BooleanAwareProperties();
		converter = new VideoConverter(properties);
	}

	@After
	public void tearDown() {
		// TODO: delete output directory for making tests independent.
	}

	@Given("^the following configuration:$")
	public void the_following_configuration(final DataTable configuration) throws Throwable {
		final List<List<String>> raw = configuration.raw();

		for (int i = 1; i < raw.size(); i++) {
			final List<String> row = raw.get(i);
			final String key = row.get(0);
			final String value = row.get(1);

			LOG.info("Key [{}], Value [{}]", key, value);
			properties.setProperty(key, value);
		}
	}

	@And("^the following files are in the input folder$")
	public void the_following_files_are_in_the_input_folder(final DataTable files) throws Throwable {
		final String inputPath = properties.getProperty(INPUT_DIRECTORY);
		final Path inputDirectory = Paths.get(inputPath);

		createDirectories(inputDirectory);

		final Path source = Paths.get("src/test/resources/test.mp4");

		final List<List<String>> raw = files.raw();
		for (int i = 1; i < raw.size(); i++) {
			final String destinationPath = raw.get(i).get(0);
			final Path destination = inputDirectory.resolve(destinationPath);
			LOG.debug("Copying test file [{}] to [{}].", source.toAbsolutePath(), destination.toAbsolutePath());
			copy(source, destination, REPLACE_EXISTING);
		}
	}

	@When("^the program is executed$")
	public void the_program_is_executed() throws Throwable {
	        converter.convert();
	}

	@Then("^the output folder should contain the following paths$")
	public void the_output_folder_should_contain_the_following_paths(final List<String> input) throws Throwable {
		List<String> raw = new ArrayList<>();
		raw.add("Path");

		final String outputFolder = properties.getProperty(OUTPUT_DIRECTORY);
		final Path outputPath = Paths.get(outputFolder);
		final PathCollector collector = new PathCollector(raw);
		walkFileTree(outputPath, collector);

		for (int i = 0; i < raw.size(); i++) {
			final String path = raw.get(i);
			final String replaced = path.replace(outputPath.toAbsolutePath().toString(), "").replace("\\", "/");
			if (replaced.startsWith("/")) {
				raw.set(i, replaced.substring(1));
			} else {
				raw.set(i, replaced);
			}
		}

		List<String> expected = new ArrayList<>(input);
		Collections.sort(raw);
		Collections.sort(expected);

		final DataTable actual = DataTable.create(raw);
		DataTable.create(expected).diff(actual);
	}

	private class PathCollector implements FileVisitor<Path> {

		private final List<String> rawDataTable;

		public PathCollector(List<String> rawDataTable) {
			this.rawDataTable = rawDataTable;
		}

		@Override
		public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
			return CONTINUE;
		}

		@Override
		public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
			rawDataTable.add(file.toAbsolutePath().toString());
			return CONTINUE;
		}

		@Override
		public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
			return CONTINUE;
		}

		@Override
		public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
			return CONTINUE;
		}
	}
}
