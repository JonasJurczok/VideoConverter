package de.linesofcode.jonas.videoconverter;

import org.junit.Before;
import org.junit.Test;

import java.io.File;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.StringContains.containsString;
import static org.hamcrest.core.StringStartsWith.startsWith;

public class FfmpegCommandBuilderTest {

	private FfmpegCommandBuilder builder;
	private static final BooleanAwareProperties PROPERTIES = new BooleanAwareProperties("src/test/resources/CommandBuilderTest.properties");

	@Before
	public void setUp() {
		builder = new FfmpegCommandBuilder(PROPERTIES);
	}

	@Test
	public void justffmpeg() {
		final String command = builder.withFFMPEG().build();
		assertThat(command, startsWith("\"path/to/ffmpeg/ffmpeg.exe\""));
	}

	@Test
	public void justffmprobe() {
		final String command = builder.withFFMProbe().build();
		assertThat(command, startsWith("\"path/to/ffmpeg/ffprobe.exe\""));
	}

	@Test
	public void withInput() {
		final File input = new File("input.avi");
		final String command = builder.withInput(input).build();

		assertThat(command, containsString("-i \"" + input.getAbsolutePath() + "\""));
	}

	@Test
	public void withShowFormat() {
		final String command = builder.withShowFormat().build();

		assertThat(command, containsString("-show_format"));

	}
}
