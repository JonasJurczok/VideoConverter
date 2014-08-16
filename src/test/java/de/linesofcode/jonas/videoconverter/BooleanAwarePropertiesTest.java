package de.linesofcode.jonas.videoconverter;

import org.junit.Test;

import static de.linesofcode.jonas.videoconverter.BooleanAwareProperties.Properties.DELETE_INPUT_FILE;
import static de.linesofcode.jonas.videoconverter.BooleanAwareProperties.Properties.DRY_RUN;
import static de.linesofcode.jonas.videoconverter.BooleanAwareProperties.Properties.FFMPEG;
import static de.linesofcode.jonas.videoconverter.BooleanAwareProperties.Properties.FILE_PROJECT_DELIMITER;
import static de.linesofcode.jonas.videoconverter.BooleanAwareProperties.Properties.OUTPUT_DIRECTORY;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.core.Is.is;

public class BooleanAwarePropertiesTest {

	private static final String PROPERTIES_PATH = "src/test/resources/VideoConverterTest.properties";

	// also tests loading without a profile name.
	@Test
	public void loadFromFile() {
		final BooleanAwareProperties properties = createProperties();

		assertThat(properties.keySet().size(), is(greaterThan(0)));
	}

	@Test
	public void getBooleanHappyCase() {
		final BooleanAwareProperties properties = createProperties();
		final boolean deleteSource = properties.getBooleanProperty(DELETE_INPUT_FILE);

		assertThat(deleteSource, is(true));
	}

	@Test
	public void getBooleanNonExistentResolvesToFalse() {
		final BooleanAwareProperties properties = createProperties();
		properties.getBooleanProperty(DRY_RUN);
	}

	@Test
	public void getBooleanTextValueResolvesToFalse() {
		final BooleanAwareProperties properties = createProperties();
		final boolean deleteSource = properties.getBooleanProperty(FFMPEG);

		assertThat(deleteSource, is(false));
	}

	@Test
	public void loadPropertiesWithProfileName() {
		final BooleanAwareProperties properties = createProperties("test");

		assertThat(properties.keySet().size(), is(greaterThan(0)));
	}

	@Test
	public void getPropertyWithProfile() {
		final BooleanAwareProperties properties = createProperties("test");

		final String projectDelimiter = properties.getProperty(FILE_PROJECT_DELIMITER);

		assertThat(projectDelimiter, is("test"));
	}

	@Test
	public void getPropertyFromDefaultValue() {
		final BooleanAwareProperties properties = createProperties("test");

		final String projectDelimiter = properties.getProperty(OUTPUT_DIRECTORY);

		assertThat(projectDelimiter, is("testdirectory"));
	}

	private BooleanAwareProperties createProperties(final String profileName) {
		return new BooleanAwareProperties(PROPERTIES_PATH, profileName);
	}

	private BooleanAwareProperties createProperties() {
		return new BooleanAwareProperties(PROPERTIES_PATH);
	}
}
