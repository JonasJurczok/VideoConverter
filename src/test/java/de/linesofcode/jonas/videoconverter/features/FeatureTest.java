package de.linesofcode.jonas.videoconverter.features;

import cucumber.api.CucumberOptions;
import cucumber.api.junit.Cucumber;
import org.junit.runner.RunWith;

@RunWith(Cucumber.class)
@CucumberOptions(format = "pretty", tags = {"~@ignore"}, features = "src/test/resources/features")
public class FeatureTest {
}
