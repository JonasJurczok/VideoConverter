package de.linesofcode.jonas.videoconverter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class BooleanAwareProperties extends java.util.Properties {

	private static final Logger LOG = LoggerFactory.getLogger(BooleanAwareProperties.class);

	private String profilePrefix;

	public BooleanAwareProperties() {

	}

	public BooleanAwareProperties(final String fileName) {
		this(fileName, null);
	}

	public BooleanAwareProperties(final String fileName, final String profileName) {
		loadProperties(fileName);
		logCurrentConfiguration();

		convertToProfilePrefix(profileName);
	}

	private void convertToProfilePrefix(String profileName) {
		if (profileName == null || profileName.isEmpty()) {
			LOG.info("No profile name given. Using default values only.");
			profilePrefix = "";
		} else {
			LOG.info("Setting active profile to [{}]", profileName);
			if (profileName.endsWith(".")) {
				profilePrefix = profileName;
			} else {
				profilePrefix = profileName + ".";
			}
		}
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
			LOG.info("File [{}] found. Reading values.", fileName);
			load(inputStream);
		} catch (IOException e) {
			throw new RuntimeException("Loading the properties failed!", e);
		}

		LOG.debug("Finished.");
	}

	public boolean getBooleanProperty(final Properties key) {
		try {
			final String stored = getProperty(key);
			final boolean isNull = stored == null;
			final boolean isEmpty = isNull || stored.isEmpty();
			final boolean isTrue = "true".equalsIgnoreCase(stored);
			return !isNull && !isEmpty && isTrue;
		} catch (NullPointerException e) {
			LOG.info("Key [{}] not configured. Defaulting to [{}].", key.keyName(), false);
			return false;
		}
	}

	public String getProperty(Properties key) {
		//TODO: only fetch with profile if there actually is a profile configured.
		LOG.debug("Trying to fetch property for key [{}] with profile [{}]", key.keyName(), profilePrefix);
		String property = getProperty(profilePrefix + key.keyName());

		if (property == null || property.isEmpty()) {
			LOG.debug("Fetching with profile failed. Resolving value without profile.");
			property = getProperty(key.keyName());
		}

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
		INPUT_DIRECTORY {
			@Override
			public String keyName() {
				return "inputDir";
			}
		},
		FILE_PROJECT_DELIMITER {
			@Override
			public String keyName() {
				return "projectDelimiter";
			}
		},
		OUTPUT_DIRECTORY {
			@Override
			public String keyName() {
				return "outputDir";
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
		},
		COPY_ONLY {
			@Override
			public String keyName() {
				return "copyOnly";
			}
		};

		abstract public String keyName();
	}

	public String getProfile() {
		return profilePrefix.substring(0, profilePrefix.length() - 1);
	}

	public void setProfile(final String profile) {
		convertToProfilePrefix(profile);
	}
}

