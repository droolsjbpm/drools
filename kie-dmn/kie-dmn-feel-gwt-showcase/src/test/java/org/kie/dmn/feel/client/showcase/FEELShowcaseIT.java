/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kie.dmn.feel.client.showcase;

import java.io.File;
import java.time.Duration;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.support.ui.WebDriverWait;

import static org.junit.Assert.assertEquals;

@Ignore
public class FEELShowcaseIT {

    private static final String INDEX_HTML = "target/kie-dmn-feel-gwt-showcase/index.html";

    private static final String INDEX_HTML_PATH = "file:///" + new File(INDEX_HTML).getAbsolutePath();

    private static final boolean HEADLESS = true;

    private WebDriver driver;

    @BeforeClass
    public static void setupClass() {
        WebDriverManager.firefoxdriver().setup();
    }

    @Before
    public void openDMNDesigner() {
        driver = new FirefoxDriver(getFirefoxOptions());
        driver.get(INDEX_HTML_PATH);
    }

    @After
    public void quitDriver() {
        driver.quit();
    }

    @Test
    public void testEvaluation() {
        waitOperation().withMessage("Wait until FEEL demo appears").until(e -> e.findElement(By.cssSelector("#root")));

        assertEvaluation("2 + 4", "6");
        assertEvaluation("\"Hello\" + \" world!\"", "Hello world!");
        assertEvaluation("[1, 2, 3]", "[1, 2, 3]");
        assertEvaluation("[\"Sao Paulo\", \"Valladolid\", \"Campinas\"][3]", "Campinas");
        assertEvaluation("date(1992, 02, 01).year", "1992");
        assertEvaluation("some i in [1, 2, 3, 4, 5] satisfies i > 4", "true");
        assertEvaluation("some i in [1, 2, 3, 4, 5] satisfies i > 5", "false");
        assertEvaluation("1 in [1..10]", "true");
        assertEvaluation("1 in (1..10]", "false");
        assertEvaluation("string length(\"DMN\")", "3");
        assertEvaluation("substring(\"Learn DMN in 15 minutes\", 6, 3)", "DMN");
        assertEvaluation("lower case(\"LEARN-DMN-IN-15-MINUTES.COM\")", "learn-dmn-in-15-minutes.com");
        assertEvaluation("upper case(\"learn-dmn-in-15-minutes.com\")", "LEARN-DMN-IN-15-MINUTES.COM");
        assertEvaluation("sum([1, 2, 3, 4, 5])", "15");
        assertEvaluation("if 20 > 0 then \"YES\" else \"NO\"", "YES");
        assertEvaluation("if (20 - (10 * 2)) > 0 then \"YES\" else \"NO\"", "NO");
    }

    private void assertEvaluation(final String feelExpression,
                                  final String expectedEvaluation) {

        final WebElement feelExpressionTextBox = driver.findElement(By.cssSelector("[data-field='text']"));
        final WebElement feelEvaluation = driver.findElement(By.cssSelector("[data-field='evaluation']"));

        feelExpressionTextBox.clear();
        feelExpressionTextBox.sendKeys(feelExpression);

        assertEquals(expectedEvaluation, feelEvaluation.getAttribute("value"));
    }

    private FirefoxOptions getFirefoxOptions() {
        final FirefoxOptions firefoxOptions = new FirefoxOptions();
        firefoxOptions.setHeadless(HEADLESS);
        firefoxOptions.setProfile(getFirefoxProfile());
        return firefoxOptions;
    }

    private FirefoxProfile getFirefoxProfile() {
        final FirefoxProfile profile = new FirefoxProfile();
        profile.setPreference("webdriver.log.init", true);
        profile.setPreference("webdriver.log.browser.ignore", true);
        profile.setPreference("webdriver.log.driver.ignore", true);
        profile.setPreference("webdriver.log.profiler.ignore", true);
        return profile;
    }

    private WebDriverWait waitOperation() {
        return new WebDriverWait(driver, Duration.ofSeconds(10).getSeconds());
    }
}
