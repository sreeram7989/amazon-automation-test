import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.*;
import io.github.bonigarcia.wdm.WebDriverManager;

import java.time.Duration;
import java.util.List;
import java.util.Properties;
import java.io.FileInputStream;

public class AmazonTest {

    public static void main(String[] args) {

        WebDriverManager.chromedriver().setup();
        WebDriver driver = new ChromeDriver();
        driver.manage().window().maximize();

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));

        try {
            // Load config
            Properties prop = new Properties();
            prop.load(new FileInputStream("config.properties"));

            String url = prop.getProperty("url");
            String product = prop.getProperty("product");
            String expectedProduct = prop.getProperty("expectedProduct");
            String quantity = prop.getProperty("quantity");

            // 1. Open Amazon
            driver.get(url);
            wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("body")));

            // 2. Search product
            WebElement searchBox = wait.until(
                    ExpectedConditions.presenceOfElementLocated(By.id("twotabsearchtextbox"))
            );
            searchBox.sendKeys(product);
            driver.findElement(By.id("nav-search-submit-button")).click();

            // 3. Verify results
            wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.xpath("//div[@data-component-type='s-search-result']")
            ));
            System.out.println("Search results displayed");

            // 4. Select product dynamically
            List<WebElement> products = driver.findElements(
                    By.xpath("//div[@data-component-type='s-search-result']")
            );

            boolean found = false;
            String parent = driver.getWindowHandle();

            for (WebElement p : products) {
                try {
                    if (p.getText().contains(expectedProduct)) {
                        p.findElement(By.tagName("a")).click();
                        found = true;
                        break;
                    }
                } catch (Exception ignored) {}
            }

            if (!found) throw new Exception("Product not found");

            // Switch tab if needed
            for (String win : driver.getWindowHandles()) {
                if (!win.equals(parent)) {
                    driver.switchTo().window(win);
                }
            }

            // 5. Verify product page
            wait.until(ExpectedConditions.presenceOfElementLocated(By.id("add-to-cart-button")));
            System.out.println("Product page opened");

            // 6. Select quantity
            try {
                Select qty = new Select(driver.findElement(By.id("quantity")));
                qty.selectByValue(quantity);
                System.out.println("Quantity selected as " + quantity);
            } catch (Exception e) {
                System.out.println("Quantity dropdown not available");
            }

            // 7. Add to cart (stable click)
            WebElement addToCart = wait.until(
                    ExpectedConditions.presenceOfElementLocated(By.id("add-to-cart-button"))
            );

            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", addToCart);
            Thread.sleep(2000);
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", addToCart);

            // 8. Verify subtotal
            wait.until(ExpectedConditions.or(
                    ExpectedConditions.presenceOfElementLocated(By.id("nav-cart-count")),
                    ExpectedConditions.presenceOfElementLocated(By.id("sw-subtotal"))
            ));
            System.out.println("Cart subtotal displayed");

            // 9. Go to cart
            driver.findElement(By.id("nav-cart")).click();

            // 10. Verify cart page
            wait.until(ExpectedConditions.titleContains("Shopping Cart"));
            System.out.println("Shopping cart page opened");

            // 11. Validate using text (robust)
            WebElement cartItem = wait.until(ExpectedConditions.visibilityOfElementLocated(
                    By.xpath("//div[contains(@class,'sc-list-item')]")
            ));

            String cartText = cartItem.getText();

            if (cartText.contains(expectedProduct)) {
                System.out.println("Product verified");
            } else {
                throw new Exception("Product not found in cart");
            }

            if (cartText.contains(quantity)) {
                System.out.println("Quantity verified as " + quantity + " ");
            } else {
                throw new Exception("Quantity mismatch");
            }

            System.out.println("TEST PASSED");

        } catch (Exception e) {
            System.out.println("TEST FAILED");
            e.printStackTrace();
        } finally {
            driver.quit();
        }
    }
}