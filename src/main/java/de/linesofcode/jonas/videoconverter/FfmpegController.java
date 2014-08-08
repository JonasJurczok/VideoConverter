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

			process.waitFor();
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
}
