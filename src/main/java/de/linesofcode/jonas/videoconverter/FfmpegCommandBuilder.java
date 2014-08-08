package de.linesofcode.jonas.videoconverter;

import java.io.File;

import static de.linesofcode.jonas.videoconverter.BooleanAwareProperties.Properties.FFMPEG;

public class FfmpegCommandBuilder {

	private final BooleanAwareProperties properties;

	private final StringBuilder builder = new StringBuilder();

	public FfmpegCommandBuilder(final BooleanAwareProperties properties) {
		this.properties = properties;
	}

	public FfmpegCommandBuilder withFFMPEG() {
		//TODO: allow ffmpeg path to not end in \
		builder.append("\"").append(properties.getProperty(FFMPEG)).append("ffmpeg.exe\"");
		return this;
	}

	public FfmpegCommandBuilder withFFMProbe() {
		builder.append("\"").append(properties.getProperty(FFMPEG)).append("ffprobe.exe\"");
		return this;
	}

	public FfmpegCommandBuilder withInput(final File input) {
		builder.append(" -i ").append("\"").append(input.getAbsolutePath()).append("\"");

		return this;
	}

	public FfmpegCommandBuilder withShowFormat() {
		builder.append(" -show_format");

		return this;
	}

	public String build() {
		return builder.toString();
	}
}
