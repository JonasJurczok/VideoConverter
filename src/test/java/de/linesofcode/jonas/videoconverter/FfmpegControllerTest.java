package de.linesofcode.jonas.videoconverter;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.math.BigDecimal;

public class FfmpegControllerTest {

	private FfmpegController controller;

	@Before
	public void createController() {
		controller = new FfmpegController(new BooleanAwareProperties("src/test/resources/ffmpegControllerTest.properties"));
	}

	@Test
	public void getDuration() {
		final File input = new File("src/test/resources/test.mp4");

		final BigDecimal duration = controller.getDuration(input);

		MatcherAssert.assertThat(duration, Matchers.closeTo(new BigDecimal(5.0), new BigDecimal(0.01)));
	}
}
