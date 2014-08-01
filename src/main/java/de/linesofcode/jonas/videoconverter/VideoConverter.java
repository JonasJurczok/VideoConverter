package de.linesofcode.jonas.videoconverter;

import com.google.common.base.Optional;
import com.google.common.io.PatternFilenameFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

import static com.google.common.base.Optional.absent;
import static com.google.common.base.Optional.of;
import static com.google.common.collect.Lists.newArrayList;
import static de.linesofcode.jonas.videoconverter.BooleanAwareProperties.Properties.DELETE_SOURCE_FILE;
import static de.linesofcode.jonas.videoconverter.BooleanAwareProperties.Properties.DRY_RUN;
import static de.linesofcode.jonas.videoconverter.BooleanAwareProperties.Properties.FFMPEG;
import static de.linesofcode.jonas.videoconverter.BooleanAwareProperties.Properties.FILE_PROJECT_DELIMITER;
import static de.linesofcode.jonas.videoconverter.BooleanAwareProperties.Properties.INPUT_PATH;
import static de.linesofcode.jonas.videoconverter.BooleanAwareProperties.Properties.ORIGINAL_FILE_SUFFIX;
import static de.linesofcode.jonas.videoconverter.BooleanAwareProperties.Properties.OUTPUT_FILE_SUFFIX;
import static de.linesofcode.jonas.videoconverter.BooleanAwareProperties.Properties.PROJECT_DIRECTORY;


public final class VideoConverter {

	private static final Logger LOG = LoggerFactory.getLogger(VideoConverter.class);

	private final BooleanAwareProperties PROPERTIES;

	public VideoConverter() {
		LOG.trace("Setting up video converter...");
		PROPERTIES = new BooleanAwareProperties("VideoConverter.properties");
		LOG.trace("Finished.");
	}

	public void convert() {
		verifySourceDirectoryExistsOrFail();

		LOG.info("Starting conversion process...");

		final String inputPath = PROPERTIES.getProperty(INPUT_PATH);
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

			final String command = generateFFMPEGCommand(file, outputFile);
			try {
				runFFMPEG(command);
			} catch (IOException | InterruptedException e) {
				LOG.warn("FFMPEG failed with error [{}]. The current file will be ignored.", e.getMessage(), e);
				continue;
			}

			final boolean shouldDeleteSourceFile = PROPERTIES.getBooleanProperty(DELETE_SOURCE_FILE);
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

	private String generateDestinationFileName(final File input) {
		LOG.trace("Generating output file name for original file.");

		final String rawName = extractRawName(input);

		LOG.debug("Raw filename is [{}].", rawName);

		final String suffix = PROPERTIES.getProperty(ORIGINAL_FILE_SUFFIX);
		LOG.debug("Original file suffix is [{}].", suffix);

		final String fileName = rawName + " " + suffix + ".avi";
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

	private void runFFMPEG(final String command) throws IOException, InterruptedException {

		final boolean isDryRun = PROPERTIES.getBooleanProperty(DRY_RUN);

		if (isDryRun) {
			LOG.info("Dry run. Command would have been [{}].", command);
			return;
		}

		Process process = null;
		try {
			LOG.info("Starting ffmpeg");
			process = new ProcessBuilder(command).redirectErrorStream(true).start();
			BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

			String line = "";
			while ((line = reader.readLine())!= null) {
				LOG.info(line);
			}

			process.waitFor();

		} finally {
			if (process != null) {
				process.destroy();
			}
		}
	}

	private String generateFFMPEGCommand(final File input, final File outputFile) {
		LOG.trace("Generating ffmpeg command...");

		final StringBuilder builder = new StringBuilder();

		// ffmpeg -y -i "!FILE!" -c:v libx264 -preset veryslow -map 0:0 -map 0:1 -map 0:2 -c:a aac -strict experimental -b:a 192k -ac 2 -qp 0 -threads 4 "!OUTPUT!"
		builder.append("\"" + PROPERTIES.getProperty(FFMPEG) + "\"");
		builder.append(" -y -i ");
		builder.append("\"" + input.getAbsolutePath() + "\"");   // input file
		builder.append(" -map 0:0 -map 0:1 -map 0:2"); // mapping
		builder.append(" -c:v libx264 -preset veryslow -qp 0");       // video encoding
		builder.append(" -c:a aac -strict experimental -b:a 192k -ac 2"); // audio encoding
		builder.append(" -threads 4"); // optimization
		builder.append(" ");
		builder.append("\"" + outputFile.getAbsolutePath() + "\"");

		final String command = builder.toString();
		LOG.debug("FFMPEG command will be [{}]", command);

		LOG.trace("Finished.");
		return command;
	}

	private File generateOutputFile(final File file, Optional<File> projectDirectory) {
		LOG.trace("Generating output file name for input [{}]...", file.getAbsolutePath());

		final String rawName = extractRawName(file);

		LOG.debug("Raw filename is [{}].", rawName);

		final String suffix = PROPERTIES.getProperty(OUTPUT_FILE_SUFFIX);
                LOG.debug("Output file suffix is [{}].", suffix);

		final String fileName = rawName + suffix + ".mp4";
		LOG.debug("Generated filename is [{}]", fileName);

		LOG.trace("Finished.");
		return new File(projectDirectory.get(), fileName);
	}

	private Optional<File> createProjectDirectoryOrFail(final String projectName){
		LOG.trace("Creating project directory...");

		final String path = PROPERTIES.getProperty(PROJECT_DIRECTORY);
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

	private void verifySourceDirectoryExistsOrFail() {
		LOG.debug("Verifying source directory...");
		final String path = PROPERTIES.getProperty(INPUT_PATH);
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
