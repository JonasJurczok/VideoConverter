package de.linesofcode.jonas.videoconverter;

import com.sun.corba.se.spi.orbutil.fsm.Input;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import static de.linesofcode.jonas.videoconverter.BooleanAwareProperties.Properties.DELETE_SOURCE_FILE;

public class BooleanAwareProperties extends java.util.Properties {

	private static final Logger LOG = LoggerFactory.getLogger(BooleanAwareProperties.class);

	public BooleanAwareProperties() {
	}

	public BooleanAwareProperties(final String fileName) {
		loadProperties(fileName);

		logCurrentConfiguration();
	}

	private void logCurrentConfiguration() {
		LOG.info("Current configuration is:");

		for (final Object key : keySet()) {
			LOG.info("[{}] = [{}]", key, getProperty(key.toString()));
		}
	}

	private void loadProperties(String fileName) {
		LOG.debug("Loading properties from file...");

		try (final InputStream inputStream = new FileInputStream(fileName)) {
			LOG.info("VideoConverter.properties found. Reading values.");
			load(inputStream);
		} catch (IOException e) {
			throw new RuntimeException("Loading the properties failed!", e);
		}

		LOG.debug("Finished.");
	}

	public boolean getBooleanProperty(final Properties key) {
		final String stored = getProperty(key);
		final boolean isNull = stored == null;
		final boolean isEmpty = isNull || stored.isEmpty();
		final boolean isFalse = "false".equalsIgnoreCase(stored);
		return !isNull && !isEmpty && !isFalse;
	}

	public String getProperty(Properties key) {
		final String property = getProperty(key.keyName());

		if (property == null || property.isEmpty()) {
			throw new NullPointerException("Key [" + key.keyName() + "] is not configured.");
		}

		return property;
	}


	public enum Properties {

		DELETE_SOURCE_FILE {
			@Override
			public String keyName() {
				return "deleteSourceFile";
			}
		},
		INPUT_PATH {
			@Override
			public String keyName() {
				return "inputPath";
			}
		},
		FILE_PROJECT_DELIMITER {
			@Override
			public String keyName() {
				return "projectDelimiter";
			}
		},
		PROJECT_DIRECTORY {
			@Override
			public String keyName() {
				return "projectDirectory";
			}
		},
		OUTPUT_FILE_SUFFIX {
			@Override
			public String keyName() {
				return "outputFileSuffix";
			}
		},
		ORIGINAL_FILE_SUFFIX {
			@Override
			public String keyName() {
				return "originalFileSuffix";
			}
		},
		DRY_RUN {
			@Override
			public String keyName() {
				return "dryRun";
			}
		},
		FFMPEG {
			@Override
			public String keyName() {
				return "ffmpeg";
			}
		};

		abstract public String keyName();
	}

}

