package com.example.simactivation;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;
import org.openqa.selenium.By;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;

class SeleniumJourneyIT {
    private WebDriver driver;
    private WebDriverWait wait;

    @RegisterExtension
    private final TestExecutionExceptionHandler screenshotOnFailure = new TestExecutionExceptionHandler() {
        @Override
        public void handleTestExecutionException(ExtensionContext context, Throwable cause) throws Throwable {
            if (driver != null) {
                try {
                    captureFailure();
                } catch (Exception screenshotFailure) {
                    cause.addSuppressed(screenshotFailure);
                }
            }
            throw cause;
        }
    };

    @BeforeEach
    void openBrowser() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new", "--no-sandbox", "--disable-dev-shm-usage");
        driver = new ChromeDriver(options);
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        driver.get(System.getProperty("app.baseUrl", "http://localhost:8080"));
        wait.until(webDriver -> webDriver.getTitle().contains("SIM Activation"));
    }

    @AfterEach
    void closeBrowser() {
        if (driver != null) {
            driver.quit();
        }
    }

    @Test
    void createsAndReadsDraftRecord() {
        String customer = "Create Journey " + System.nanoTime();
        createRecord(customer);
        wait.until(webDriver -> webDriver.getPageSource().contains(customer));
        assertTrue(driver.getPageSource().contains("DRAFT"));
    }

    @Test
    void searchesByCustomerName() {
        String customer = "Search Journey " + System.nanoTime();
        createRecord(customer);
        driver.findElement(By.id("search-query")).sendKeys(customer);
        driver.findElement(By.cssSelector("#search-form button[type=submit]")).click();
        wait.until(webDriver -> webDriver.findElement(By.id("records")).getText().contains(customer));
        assertTrue(driver.findElement(By.id("records")).getText().contains(customer));
    }

    @Test
    void updatesAnExistingDraft() {
        String customer = "Original " + System.nanoTime();
        createRecord(customer);
        rowFor(customer).findElement(By.cssSelector("[data-edit]")).click();
        driver.findElement(By.id("customer-name")).clear();
        driver.findElement(By.id("customer-name")).sendKeys("Updated " + customer);
        driver.findElement(By.cssSelector("#activation-form button[type=submit]")).click();
        wait.until(webDriver -> webDriver.getPageSource().contains("Updated " + customer));
        assertTrue(driver.getPageSource().contains("Updated " + customer));
    }

    @Test
    void enforcesRoleOrderedStatusWorkflow() {
        String customer = "Workflow " + System.nanoTime();
        createRecord(customer);
        transitionTo(customer, "SUBMITTED", "AGENT");
        transitionTo(customer, "IN_REVIEW", "SUPERVISOR");

        driver.findElement(By.id("actor-role")).sendKeys("SUPERVISOR");
        rowFor(customer).findElement(By.cssSelector("[data-next='ACTIVATED']")).click();
        wait.until(webDriver -> webDriver.findElement(By.id("message")).getText().contains("Role is not allowed"));
        assertTrue(rowFor(customer).getText().contains("IN_REVIEW"));

        transitionTo(customer, "ACTIVATED", "ADMIN");
        assertTrue(rowFor(customer).getText().contains("ACTIVATED"));
        assertTrue(driver.findElement(By.id("dashboard")).getText().contains("ACTIVATED"));
    }

    private void createRecord(String customer) {
        String numeric = Long.toUnsignedString(System.nanoTime());
        String msisdn = "9" + numeric.substring(Math.max(0, numeric.length() - 9));
        while (msisdn.length() < 10) {
            msisdn += "1";
        }
        String iccid = "89" + String.format("%018d", Math.abs(System.nanoTime()) % 1_000_000_000_000_000_000L);
        driver.findElement(By.id("customer-name")).sendKeys(customer);
        driver.findElement(By.id("msisdn")).sendKeys(msisdn);
        driver.findElement(By.id("iccid")).sendKeys(iccid);
        driver.findElement(By.cssSelector("#activation-form button[type=submit]")).click();
        wait.until(webDriver -> webDriver.findElement(By.id("records")).getText().contains(customer));
    }

    private void transitionTo(String customer, String status, String role) {
        driver.findElement(By.id("actor-role")).sendKeys(role);
        rowFor(customer).findElement(By.cssSelector("[data-next='" + status + "']")).click();
        wait.until(webDriver -> rowFor(customer).getText().contains(status));
    }

    private org.openqa.selenium.WebElement rowFor(String customer) {
        return driver.findElement(By.xpath("//tr[td[2][contains(., '" + customer + "')]]"));
    }

    private void captureFailure() {
        try {
            Path screenshots = Path.of("target", "selenium-failures");
            Files.createDirectories(screenshots);
            Files.write(screenshots.resolve("journey-failure-" + System.nanoTime() + ".png"),
                    ((org.openqa.selenium.TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES));
        } catch (Exception screenshotFailure) {
            throw new IllegalStateException("Could not capture Selenium failure screenshot", screenshotFailure);
        }
    }
}
