# Appium Java Automation Framework

[![Java](https://img.shields.io/badge/Java-17-blue.svg)](https://www.oracle.com/java/)
[![Appium](https://img.shields.io/badge/Appium-2.x-green.svg)](https://appium.io/)
[![TestNG](https://img.shields.io/badge/TestNG-7.9.0-red.svg)](https://testng.org/)
[![Maven](https://img.shields.io/badge/Maven-3.9+-orange.svg)](https://maven.apache.org/)

## 📋 Overview

Enterprise-grade mobile automation framework built with Appium 2.x, Java 17, and TestNG. This framework provides a robust, scalable, and maintainable solution for automating Android and iOS mobile applications.

## 🏗️ Architecture

```
src/
├── main/
│   ├── java/com/mobile/automation/
│   │   ├── config/          # Configuration management
│   │   ├── core/            # Core framework components
│   │   ├── pages/           # Page Object Model classes
│   │   ├── utils/           # Utility classes
│   │   └── listeners/       # TestNG listeners
│   └── resources/
│       ├── config/          # Environment configuration files
│       └── testdata/        # Test data files
└── test/
    ├── java/com/mobile/automation/
    │   └── tests/           # Test classes
    └── resources/
        ├── testng.xml       # TestNG suite files
        └── log4j2.xml       # Logging configuration
```

## ✨ Key Features

- ✅ **ThreadLocal Driver Management** - Thread-safe driver instances for parallel execution
- ✅ **Cross-Platform Support** - Android (UiAutomator2) and iOS (XCUITest)
- ✅ **Page Object Model** - Clean separation of test logic and UI interactions
- ✅ **Explicit Waits** - No Thread.sleep(), only explicit and fluent waits
- ✅ **Smart Retry Logic** - Retries only infrastructure failures, not assertion failures
- ✅ **Comprehensive Reporting** - Allure reports with screenshots and metadata
- ✅ **Environment Configuration** - YAML-based configuration with environment variable support
- ✅ **Gesture Utilities** - Reusable methods for tap, swipe, scroll, zoom, drag-drop
- ✅ **CI/CD Ready** - Jenkins, GitHub Actions, and Docker support
- ✅ **Logging** - Log4j2 with per-test log files
- ✅ **Screenshot on Failure** - Automatic screenshot capture on test failures

## 🔧 Prerequisites

### Required Software

- **Java 17+** - [Download](https://adoptium.net/)
- **Maven 3.9+** - [Download](https://maven.apache.org/download.cgi)
- **Node.js 18+** - [Download](https://nodejs.org/)
- **Appium 2.x** - Install via npm: `npm install -g appium@next`

### Platform-Specific Requirements

#### Android
- Android SDK (API 28+)
- Android Studio
- Appium UiAutomator2 Driver: `appium driver install uiautomator2`
- Android Emulator or Real Device

#### iOS
- Xcode (macOS only)
- Xcode Command Line Tools
- Appium XCUITest Driver: `appium driver install xcuitest`
- iOS Simulator or Real Device
- carthage: `brew install carthage`

## 🚀 Setup Instructions

### 1. Clone Repository

```bash
git clone <repository-url>
cd apium-java
```

### 2. Install Appium

```bash
npm install -g appium@next
appium driver install uiautomator2
appium driver install xcuitest
```

Verify installation:
```bash
appium -v
appium driver list
```

### 3. Install Maven Dependencies

```bash
mvn clean install -DskipTests
```

### 4. Setup Environment Variables

Create a `.env` file or set system environment variables:

```bash
# Device Configuration
export DEVICE_NAME="Pixel_7_Pro"
export DEVICE_UDID="emulator-5554"
export PLATFORM_VERSION="14.0"

# App Configuration
export APP_PATH="./apps/android-app.apk"
export APP_PACKAGE="com.example.app"
export APP_ACTIVITY="com.example.app.MainActivity"

# Test Users (for security)
export TEST_USER_1="user1@example.com"
export TEST_PASSWORD_1="securePassword123"

# Appium Server
export APPIUM_HOST="127.0.0.1"
export APPIUM_PORT="4723"
```

### 5. Place App Files

Create an `apps` directory and place your APK/IPA files:

```bash
mkdir -p apps
# Copy your app files to apps/ directory
```

## 🎯 Running Tests

### Local Execution

#### Start Appium Server

```bash
appium --address 127.0.0.1 --port 4723
```

#### Run Tests

**Run all tests (default: Android QA):**
```bash
mvn test
```

**Run with specific platform and environment:**
```bash
mvn test -Dplatform=android -Denv=qa
```

**Run smoke tests:**
```bash
mvn test -DsuiteXmlFile=src/test/resources/testng.xml
```

**Run regression tests:**
```bash
mvn test -DsuiteXmlFile=src/test/resources/testng-regression.xml
```

**Run with parallel execution:**
```bash
mvn test -DthreadCount=3
```

**Run specific test class:**
```bash
mvn test -Dtest=LoginTest
```

**Run specific test method:**
```bash
mvn test -Dtest=LoginTest#testSuccessfulLogin
```

### iOS Execution

```bash
mvn test -Dplatform=ios -Denv=qa
```

### Docker Execution

#### Start Appium in Docker

```bash
cd docker
docker-compose up -d
```

#### Run tests against Docker

```bash
mvn test -Dserver.type=docker
```

#### Stop Docker containers

```bash
docker-compose down
```

## 📊 Reporting

### Generate Allure Report

```bash
# Generate report
mvn allure:report

# Open report in browser
mvn allure:serve
```

### View Reports

- **Allure Report**: `target/allure-reports/index.html`
- **TestNG Report**: `target/surefire-reports/index.html`
- **Logs**: `target/logs/`
- **Screenshots**: `target/screenshots/`
- **Videos**: `target/videos/`

## 📁 Configuration

### Environment Configuration

Configuration files are located in `src/main/resources/config/`:

- `qa-android.yaml` - QA Android configuration
- `qa-ios.yaml` - QA iOS configuration
- `dev-android.yaml` - Dev Android configuration

#### Example Configuration

```yaml
environment: qa

appium:
  host: ${APPIUM_HOST:127.0.0.1}
  port: ${APPIUM_PORT:4723}

platform:
  name: Android
  version: ${PLATFORM_VERSION:14.0}
  automationName: UiAutomator2

device:
  name: ${DEVICE_NAME:Android Emulator}
  udid: ${DEVICE_UDID:}

app:
  path: ${APP_PATH:./apps/android-app.apk}
  package: ${APP_PACKAGE:com.example.app}
  activity: ${APP_ACTIVITY:com.example.app.MainActivity}
```

### Test Data

Test data is stored in `src/main/resources/testdata/testdata.json`:

```json
{
  "testUsers": [
    {
      "username": "${TEST_USER_1:user1@example.com}",
      "password": "${TEST_PASSWORD_1:}",
      "role": "customer"
    }
  ],
  "testData": {
    "validEmail": "test@example.com",
    "searchKeyword": "smartphone"
  }
}
```

## 🧪 Writing Tests

### Example Test Class

```java
@Epic("Authentication")
@Feature("Login")
public class LoginTest extends BaseTest {
    
    @Test(priority = 1, description = "Verify successful login")
    @Severity(SeverityLevel.BLOCKER)
    public void testSuccessfulLogin() {
        LoginPage loginPage = new LoginPage(driver);
        HomePage homePage = new HomePage(driver);
        
        loginPage.login("user@example.com", "password");
        
        Assert.assertTrue(homePage.isHomePageDisplayed(), 
            "Home page should be displayed");
    }
}
```

### Example Page Object

```java
public class LoginPage extends BasePage {
    
    @AndroidFindBy(id = "com.example.app:id/email")
    @iOSXCUITFindBy(accessibility = "emailTextField")
    private WebElement emailField;
    
    public LoginPage(AppiumDriver driver) {
        super(driver);
    }
    
    @Step("Login with email: {email}")
    public void login(String email, String password) {
        enterText(emailField, email);
        enterText(passwordField, password);
        click(loginButton);
    }
}
```

## 🔄 CI/CD Integration

### Jenkins

1. Create a new Pipeline job
2. Point to `Jenkinsfile` in repository
3. Configure parameters:
   - PLATFORM: android/ios
   - ENVIRONMENT: qa/dev/stage
   - SUITE: smoke/regression
   - THREAD_COUNT: Number of parallel threads

### GitHub Actions

Workflow is configured in `.github/workflows/test-automation.yml`

Trigger manually:
```bash
gh workflow run test-automation.yml \
  -f platform=android \
  -f environment=qa \
  -f suite=smoke
```

### Cloud Device Farms

#### BrowserStack

Update configuration:
```yaml
appium:
  host: hub-cloud.browserstack.com
  port: 443

capabilities:
  browserstack.user: ${BS_USERNAME}
  browserstack.key: ${BS_ACCESS_KEY}
```

#### Sauce Labs

Update configuration:
```yaml
appium:
  host: ondemand.saucelabs.com
  port: 443

capabilities:
  sauce:options:
    username: ${SAUCE_USERNAME}
    accessKey: ${SAUCE_ACCESS_KEY}
```

## 🛠️ Best Practices

### ✅ DO's

- Use Page Object Model for all UI interactions
- Use explicit waits for element synchronization
- Use ThreadLocal for driver management
- Use Allure annotations for better reporting
- Use environment variables for sensitive data
- Write independent and isolated tests
- Use descriptive test names and assertions
- Implement proper exception handling

### ❌ DON'Ts

- DON'T use static drivers
- DON'T use Thread.sleep()
- DON'T hardcode credentials or locators
- DON'T create drivers in test classes
- DON'T put assertions in page classes
- DON'T retry on assertion failures
- DON'T mix test data with test logic

## 🐛 Troubleshooting

### Common Issues

**Issue: Appium server not starting**
```bash
# Kill existing processes
pkill -f appium
# Start fresh
appium --address 127.0.0.1 --port 4723
```

**Issue: Element not found**
- Check if correct locator strategy is used
- Verify element is present in the app hierarchy
- Add appropriate waits before interaction

**Issue: Tests failing with NoSuchSessionException**
- Ensure Appium server is running
- Check if device/emulator is connected
- Verify capabilities are correct

**Issue: Parallel execution issues**
- Ensure each thread has its own device/emulator
- Check ThreadLocal driver implementation
- Verify test independence

## 📚 Additional Resources

- [Appium Documentation](https://appium.io/docs/en/latest/)
- [Selenium Documentation](https://www.selenium.dev/documentation/)
- [TestNG Documentation](https://testng.org/doc/documentation-main.html)
- [Allure Documentation](https://docs.qameta.io/allure/)

## 👥 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📄 License

This project is licensed under the MIT License.

## 📧 Contact

For questions and support, please contact the Mobile Automation Team.

---

**Built with ❤️ by the Mobile Automation Team**
