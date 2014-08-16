package de.linesofcode.jonas.videoconverter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static de.linesofcode.jonas.videoconverter.BooleanAwareProperties.Properties.FADING_DURATION;
import static de.linesofcode.jonas.videoconverter.BooleanAwareProperties.Properties.FFMPEG;
import static de.linesofcode.jonas.videoconverter.BooleanAwareProperties.Properties.INTRO_PATH;
import static de.linesofcode.jonas.videoconverter.BooleanAwareProperties.Properties.USE_FADING;
import static de.linesofcode.jonas.videoconverter.BooleanAwareProperties.Properties.USE_INTRO;
import static java.lang.Math.round;
import static java.nio.file.Files.move;
import static java.nio.file.Paths.get;

public class FfmpegController {
	private static final Logger LOG = LoggerFactory.getLogger(FfmpegController.class);

	private static final String DURATION_PREFIX = "duration=";

	private final BooleanAwareProperties properties;

	public FfmpegController(BooleanAwareProperties properties) {
		this.properties = properties;
	}

	public void executeCommand(final String command, boolean redirectErrors, boolean logOutput) {

		if (command == null || command.isEmpty()) {
			LOG.warn("No command given to FFMPEGController.executeCommand!");
			return;
		}

		LOG.info("Executing command [{}].", command);

		Process process = null;
		try {
			process = new ProcessBuilder(command).redirectErrorStream(redirectErrors).start();

			if (logOutput) {
				BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

				String line = "";
				while ((line = reader.readLine()) != null) {
					LOG.info(line);
				}
			}

			final int result = process.waitFor();
			if (result != 0) {
				throw new RuntimeException("FFMPEG process failed.");
			}

		} catch (InterruptedException | IOException e) {
			throw new RuntimeException(e);
		} finally {
			if (process != null) {
				process.destroy();
			}
		}
	}

	public BigDecimal getDuration(final File input) {

		if (!input.exists()) {
			throw new IllegalArgumentException("Inputfile [" + input.getAbsolutePath() + "] does not exist!");
		}

		if (input.isDirectory()) {
			throw new IllegalArgumentException("Input [" + input.getAbsolutePath() + "] is a directory!");
		}

		final FfmpegCommandBuilder builder = new FfmpegCommandBuilder(properties);

		final String command = builder.withFFMProbe().withShowFormat().withInput(input).build();

		LOG.debug("Issuing ffprobe command [{}]", command);

		Process process = null;
		try {
			process = new ProcessBuilder(command).redirectErrorStream(true).start();
			BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

			String line = "";
			while ((line = reader.readLine())!= null) {
				if (line.startsWith(DURATION_PREFIX)) {
					final String duration = line.substring(DURATION_PREFIX.length());

					final BigDecimal result = new BigDecimal(duration).round(new MathContext(2, RoundingMode.HALF_UP));
					LOG.debug("Found duration [{}]", result);
					return result;
				}
			}

			process.waitFor();
		} catch (InterruptedException | IOException e) {
		        throw new RuntimeException(e);
		} finally {
			if (process != null) {
				process.destroy();
			}
		}

		throw new IllegalStateException("Could not determine a duration for the input file!");
	}

	public void process(final File input, final File output) {
		if (properties.getBooleanProperty(USE_FADING)) {
			processWithFading(input, output);
		} else {
			processSimple(input, output);
		}
	}

	private void processSimple(final File input, final File output) {
		LOG.trace("Generating ffmpeg command...");

		final StringBuilder builder = new StringBuilder();

		builder.append("\"").append(properties.getProperty(FFMPEG)).append("ffmpeg.exe\"");
		builder.append(" -y -i ");
		builder.append("\"").append(input.getAbsolutePath()).append("\"");   // input file
		builder.append(" -map 0:0 -map 0:1 -map 0:2"); // mapping
		builder.append(" -c:v libx264 -crf 19 -preset slow");       // video encoding
		builder.append(" -c:a aac -strict experimental -b:a 192k -ac 2"); // audio encoding
		builder.append(" -threads 4"); // optimization
		builder.append(" ");
		builder.append("\"").append(output.getAbsolutePath()).append("\"");

		final String command = builder.toString();
		LOG.debug("FFMPEG command will be [{}]", command);

		executeCommand(command, true, true);

		LOG.trace("Finished.");
	}

	private void processWithFading(final File input, final File output) {

		LOG.info("Processing with fading.");

		final File intermediateOutput = new File(output.getParent(), "intermediate.mp4");
		final String fadingCommand = buildFadingCommand(input, intermediateOutput);
		executeCommand(fadingCommand, true, true);

		if (properties.getBooleanProperty(USE_INTRO)) {
			final String addIntroCommand = buildAddIntroCommand(output, intermediateOutput);
			executeCommand(addIntroCommand, true, true);
			intermediateOutput.delete();
		} else {
			try {
				move(get(intermediateOutput.getAbsolutePath()), get(output.getAbsolutePath()));
			} catch (IOException e) {
				throw new RuntimeException("Moving files failed.", e);
			}
		}
	}

	private String buildAddIntroCommand(File output, File intermediateOutput) {
		final String intro = properties.getProperty(INTRO_PATH);
		final StringBuilder addIntroCommandBuilder = new StringBuilder();
		addIntroCommandBuilder.append("\"").append(properties.getProperty(FFMPEG)).append("ffmpeg.exe\"");
		addIntroCommandBuilder.append(" -i \"");
		addIntroCommandBuilder.append(intro);
		addIntroCommandBuilder.append("\"");
		addIntroCommandBuilder.append(" -i \"");
		addIntroCommandBuilder.append(intermediateOutput.getAbsolutePath());
		addIntroCommandBuilder.append("\"");
		addIntroCommandBuilder.append(" -filter_complex \"[0:1] [0:0] [1:1] [1:0] concat=n=2:v=1:a=1 [v] [a]\" -map \"[v]\" -map \"[a]\"");
		addIntroCommandBuilder.append(" \"");
		addIntroCommandBuilder.append(output.getAbsolutePath());
		addIntroCommandBuilder.append("\"");
		return addIntroCommandBuilder.toString();
	}

	private String buildFadingCommand(File input, File intermediateOutput) {
		final BigDecimal duration = getDuration(input);
		final Integer fadeDuration = Integer.valueOf(properties.getProperty(FADING_DURATION));

		final StringBuilder fadeBuilder = new StringBuilder();
		fadeBuilder.append("\"" + properties.getProperty(FFMPEG) + "ffmpeg.exe\"");
		fadeBuilder.append(" -y -i ");
		fadeBuilder.append("\"" + input.getAbsolutePath() + "\"");   // input file
		fadeBuilder.append(" -map 0:0 -map 0:1 -map 0:2"); // mapping
		fadeBuilder.append(" -c:v libx264 -crf 19 -preset slow -r 30 ");       // video encoding
		fadeBuilder.append(" -c:a aac -strict experimental -b:a 192k -filter_complex \"[0:1][0:2]amix\" -ac 2"); // audio encoding and merging of audio streams
		fadeBuilder.append(" -threads 4"); // optimization
		fadeBuilder.append(" -vf \"fade=in:st=0:d=" + fadeDuration);
		fadeBuilder.append(" , fade=out:st=" + (round(duration.doubleValue()) - fadeDuration) + ":d=" + fadeDuration + "\"");
		fadeBuilder.append(" \"" + intermediateOutput.getAbsolutePath() + "\"");
		return fadeBuilder.toString();
	}
}
