package de.linesofcode.jonas.videoconverter.features;

import cucumber.api.DataTable;
import cucumber.api.java.en.Given;
import de.linesofcode.jonas.videoconverter.BooleanAwareProperties;
import de.linesofcode.jonas.videoconverter.VideoConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class StepDefinitions {

	private static final Logger LOG = LoggerFactory.getLogger(StepDefinitions.class);

	private VideoConverter converter;

	@Given("^a VideoConverter with the following configuration:$")
	public void a_VideoConverter_with_the_following_configuration(final DataTable configuration) throws Throwable {

		final BooleanAwareProperties properties = new BooleanAwareProperties();

		for (final List<String> row : configuration.raw()) {
			final String key = row.get(0);
			final String value = row.get(1);

			LOG.info("Key [{}], Value [{}]", key, value);
			properties.setProperty(key, value);
		}

		converter = new VideoConverter(properties);
	}
}
