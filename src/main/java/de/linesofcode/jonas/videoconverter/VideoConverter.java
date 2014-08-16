package de.linesofcode.jonas.videoconverter;

import com.google.common.base.Optional;
import com.google.common.io.PatternFilenameFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.util.List;

import static com.google.common.base.Optional.absent;
import static com.google.common.base.Optional.of;
import static com.google.common.collect.Lists.newArrayList;
import static de.linesofcode.jonas.videoconverter.BooleanAwareProperties.Properties.COPY_ONLY;
import static de.linesofcode.jonas.videoconverter.BooleanAwareProperties.Properties.DELETE_INPUT_FILE;
import static de.linesofcode.jonas.videoconverter.BooleanAwareProperties.Properties.DRY_RUN;
import static de.linesofcode.jonas.videoconverter.BooleanAwareProperties.Properties.FFMPEG;
import static de.linesofcode.jonas.videoconverter.BooleanAwareProperties.Properties.FILE_PROJECT_DELIMITER;
import static de.linesofcode.jonas.videoconverter.BooleanAwareProperties.Properties.INPUT_DIRECTORY;
import static de.linesofcode.jonas.videoconverter.BooleanAwareProperties.Properties.ORIGINAL_FILE_SUFFIX;
import static de.linesofcode.jonas.videoconverter.BooleanAwareProperties.Properties.OUTPUT_FILE_SUFFIX;
import static de.linesofcode.jonas.videoconverter.BooleanAwareProperties.Properties.OUTPUT_DIRECTORY;
import static de.linesofcode.jonas.videoconverter.BooleanAwareProperties.Properties.UPLOAD_TO_YOUTUBE;
import static java.nio.file.Paths.get;


public final class VideoConverter {

	private static final Logger LOG = LoggerFactory.getLogger(VideoConverter.class);
	private static final String FILE_NAME = "VideoConverter.properties";

	private final BooleanAwareProperties PROPERTIES;
	private final YoutubeController youtubeController;

	public VideoConverter() {
		LOG.trace("Setting up video converter...");
		PROPERTIES = new BooleanAwareProperties(FILE_NAME);
		youtubeController = new YoutubeController(PROPERTIES);
		LOG.trace("Finished.");
	}

	public VideoConverter(final String profileName) {
		LOG.trace("Setting up video converter...");
		PROPERTIES = new BooleanAwareProperties(FILE_NAME, profileName);
		youtubeController = new YoutubeController(PROPERTIES);
		LOG.trace("Finished.");
	}

	public VideoConverter(final BooleanAwareProperties properties, final YoutubeController youtubeController) {
		PROPERTIES = properties;
		this.youtubeController = youtubeController;
	}

	public void convert() {
		verifyInputDirectoryExistsOrFail();

		LOG.info("Starting conversion process...");

		final String inputPath = PROPERTIES.getProperty(INPUT_DIRECTORY);
		final File inputDirectory = new File(inputPath);

		final PatternFilenameFilter filter = new PatternFilenameFilter(".*\\.avi");
		final List<File> inputFiles = newArrayList(inputDirectory.listFiles(filter));

		for (final File file : inputFiles) {
			final String fileName = file.getName();
			LOG.info("Converting file [{}]", fileName);

			final Optional<String> projectName = extractProjectNameFromFileName(fileName);

			if (!projectName.isPresent()) {
				LOG.warn("Project name not found. Ignoring file.");
				continue;
			}

			LOG.info("Identified project name [{}].", projectName.get());

			final Optional<File> projectDirectory = createProjectDirectoryOrFail(projectName.get());

			if (!projectDirectory.isPresent()) {
				LOG.warn("Could not create project directory. Ignoring file.");
				continue;
			}

			final File outputFile = generateOutputFile(file, projectDirectory);
			LOG.info("Output file will be [{}]", outputFile.getAbsolutePath());

			final boolean onlyCopy = PROPERTIES.getBooleanProperty(COPY_ONLY);

			if (!onlyCopy) {
				runFFMPEG(file, outputFile);

				if (PROPERTIES.getBooleanProperty(UPLOAD_TO_YOUTUBE)) {
					youtubeController.upload(get(outputFile.getAbsolutePath()));
				}
			}

			if (onlyCopy) {
				final String destinationName = generateDestinationFileName(file);
				final File destination = new File(projectDirectory.get(), destinationName);
				LOG.info("Moving original file [{}] to project directory [{}].", file.getAbsolutePath(), destination.getAbsolutePath());
				file.renameTo(destination);
			} else {
				final boolean shouldDeleteSourceFile = PROPERTIES.getBooleanProperty(DELETE_INPUT_FILE);
				if (shouldDeleteSourceFile) {
					LOG.info("Deleting source file [{}].", file.getAbsolutePath());
					file.delete();
				} else {
					final String destinationName = generateDestinationFileName(file);
					final File destination = new File(projectDirectory.get(), destinationName);
					LOG.info("Moving original file [{}] to project directory [{}].", file.getAbsolutePath(), destination.getAbsolutePath());
					file.renameTo(destination);
				}
			}
		}
	}

	private String generateDestinationFileName(final File input) {
		LOG.trace("Generating output file name for original file.");

		final String rawName = extractRawName(input);

		LOG.debug("Raw filename is [{}].", rawName);

		final String fileName;
		String suffix = null;
		try {
			suffix = PROPERTIES.getProperty(ORIGINAL_FILE_SUFFIX);
			LOG.info("Original file suffix is [{}].", suffix);
		} catch (NullPointerException e) {
			LOG.info("Original file suffix not provided.");
		}

		if (suffix == null ||suffix.isEmpty()) {
			fileName = rawName + ".avi";
		} else {
			fileName = rawName + " " + suffix + ".avi";
		}

		LOG.debug("Generated filename is [{}]", fileName);

		return fileName;
	}

	private String extractRawName(final File input) {
		final String delimiter = PROPERTIES.getProperty(FILE_PROJECT_DELIMITER);
		LOG.debug("Found delimiter [{}].", delimiter);

		final String source = input.getName();
		final String nameWithoutExtension = source.substring(0, source.lastIndexOf('.'));
		return nameWithoutExtension.substring(nameWithoutExtension.indexOf(delimiter) + 1).trim();
	}

	private void runFFMPEG(final File file, final File outputFile) {
		final boolean isDryRun = PROPERTIES.getBooleanProperty(DRY_RUN);

		if (isDryRun) {
			LOG.info("Dry run. Skipping ffmpeg.");
			return;
		}

		final FfmpegController controller = new FfmpegController(PROPERTIES);

		controller.process(file, outputFile);
	}

	private File generateOutputFile(final File file, Optional<File> projectDirectory) {
		LOG.trace("Generating output file name for input [{}]...", file.getAbsolutePath());

		final String rawName = extractRawName(file);

		LOG.debug("Raw filename is [{}].", rawName);

		final String fileName;
		String suffix = null;
		try {
			suffix = PROPERTIES.getProperty(OUTPUT_FILE_SUFFIX);
			LOG.debug("Output file suffix is [{}].", suffix);
		} catch (NullPointerException e) {
			LOG.info("Output file suffix not configured.");
		}

		if (suffix == null || suffix.isEmpty()) {
			fileName = rawName + ".mp4";
		} else {
			fileName = rawName + suffix + ".mp4";
		}

		LOG.debug("Generated filename is [{}]", fileName);

		LOG.trace("Finished.");
		return new File(projectDirectory.get(), fileName);
	}

	private Optional<File> createProjectDirectoryOrFail(final String projectName){
		LOG.trace("Creating project directory...");

		final String path = PROPERTIES.getProperty(OUTPUT_DIRECTORY);
		final File directory = new File(path, projectName);

		if (directory.exists()) {
			LOG.info("Project directory [{}] exists.", directory.getAbsolutePath());
		} else {
			LOG.info("Project directory [{}] does not exist. Creating it now.", directory.getAbsolutePath());
			directory.mkdirs();
		}

		if (!directory.exists()) {
			absent();
		}

		LOG.trace("Finished.");
		return of(directory);
	}

	private Optional<String> extractProjectNameFromFileName(final String name) {
		LOG.trace("Extracting project name.");

		final String delimiter = PROPERTIES.getProperty(FILE_PROJECT_DELIMITER);
		LOG.debug("Using configured delimiter [{}].", delimiter);

		final String[] parts = name.split(delimiter);

		if (parts.length < 2) {
			return absent();
		}

		return of(parts[0].trim());
	}

	private void verifyInputDirectoryExistsOrFail() {
		LOG.debug("Verifying source directory...");
		final String path = PROPERTIES.getProperty(INPUT_DIRECTORY);
		final File directory = new File(path);

		if (!directory.exists()) {
			throw new RuntimeException("Source directory [" + path + "] does not exist!");
		}

		if (!directory.isDirectory()) {
			throw new RuntimeException("Source directory [" + path + "] exists but is no directory!");
		}

		LOG.info("Source directory [{}] found.", path);
	}
}
