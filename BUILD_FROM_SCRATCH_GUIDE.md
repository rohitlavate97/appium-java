# 🚀 Building Appium 2.x Framework from Scratch - Complete Guide

## Table of Contents
1. [Prerequisites Installation](#prerequisites-installation)
2. [Project Initialization](#project-initialization)
3. [Maven Configuration](#maven-configuration)
4. [Framework Architecture Setup](#framework-architecture-setup)
5. [Configuration Management](#configuration-management)
6. [Core Components Implementation](#core-components-implementation)
7. [Utility Classes Development](#utility-classes-development)
8. [Page Object Model](#page-object-model)
9. [Test Classes Creation](#test-classes-creation)
10. [Listeners Implementation](#listeners-implementation)
11. [Logging Setup](#logging-setup)
12. [Testing and Validation](#testing-and-validation)
13. [Allure Reporting](#allure-reporting)
14. [CI/CD Integration](#cicd-integration)
15. [Final Validation and Best Practices](#final-validation-and-best-practices)

---

## Phase 1: Prerequisites Installation

### Step 1.1: Install Java Development Kit (JDK) 17

**Windows:**
```powershell
# Download from https://adoptium.net/
# Or use Chocolatey
choco install temurin17

# Verify installation
java -version
javac -version
```

**Linux/macOS:**
```bash
# macOS (Homebrew)
brew install openjdk@17

# Ubuntu/Debian
sudo apt update
sudo apt install openjdk-17-jdk

# Verify
java -version
```

**Set JAVA_HOME:**
```powershell
# Windows - Add to System Environment Variables
JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.0.x

# Linux/macOS - Add to ~/.bashrc or ~/.zshrc
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
export PATH=$JAVA_HOME/bin:$PATH
```

### Step 1.2: Install Apache Maven

**Windows:**
```powershell
# Download from https://maven.apache.org/download.cgi
# Extract to C:\Program Files\Apache\Maven

# Add to PATH
$env:PATH += ";C:\Program Files\Apache\Maven\bin"

# Verify
mvn -version
```

**Linux/macOS:**
```bash
# macOS
brew install maven

# Ubuntu/Debian
sudo apt install maven

# Verify
mvn -version
```

### Step 1.3: Install Node.js and npm

**Windows:**
```powershell
# Download from https://nodejs.org/ (LTS version)
# Or use Chocolatey
choco install nodejs-lts

# Verify
node --version
npm --version
```

**Linux/macOS:**
```bash
# Using nvm (recommended)
curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.39.0/install.sh | bash
nvm install --lts
nvm use --lts

# Or direct installation
# macOS
brew install node

# Ubuntu/Debian
sudo apt install nodejs npm

# Verify
node --version
npm --version
```

### Step 1.4: Install Appium 2.x

```bash
# Install Appium globally
npm install -g appium@next

# Verify installation
appium --version

# Install drivers
appium driver install uiautomator2
appium driver install xcuitest  # macOS only for iOS

# List installed drivers
appium driver list --installed

# Install Appium Doctor (optional but recommended)
npm install -g appium-doctor

# Check setup
appium-doctor --android
appium-doctor --ios  # macOS only
```

### Step 1.5: Android SDK Setup (for Android testing)

**Install Android Studio:**
1. Download from https://developer.android.com/studio
2. Install Android Studio
3. Open Android Studio → SDK Manager
4. Install required SDK packages:
   - Android SDK Platform (API 28+)
   - Android SDK Platform-Tools
   - Android SDK Build-Tools
   - Android Emulator
   - Intel x86 Emulator Accelerator (HAXM) - Windows/macOS

**Set Environment Variables:**
```powershell
# Windows
ANDROID_HOME=C:\Users\<YourUser>\AppData\Local\Android\Sdk
ANDROID_SDK_ROOT=%ANDROID_HOME%
PATH=%PATH%;%ANDROID_HOME%\platform-tools;%ANDROID_HOME%\tools;%ANDROID_HOME%\emulator

# Linux/macOS - Add to ~/.bashrc or ~/.zshrc
export ANDROID_HOME=$HOME/Android/Sdk
export ANDROID_SDK_ROOT=$ANDROID_HOME
export PATH=$PATH:$ANDROID_HOME/platform-tools:$ANDROID_HOME/tools:$ANDROID_HOME/emulator
```

**Verify ADB:**
```bash
adb --version
adb devices
```

**Create Android Emulator:**
```bash
# List available system images
sdkmanager --list | grep system-images

# Download system image
sdkmanager "system-images;android-34;google_apis;x86_64"

# Create AVD
avdmanager create avd -n Pixel_7_Pro_API_34 -k "system-images;android-34;google_apis;x86_64" -d "pixel_7_pro"

# List AVDs
emulator -list-avds

# Start emulator
emulator -avd Pixel_7_Pro_API_34
```

### Step 1.6: iOS Setup (macOS only)

```bash
# Install Xcode from App Store

# Install Xcode Command Line Tools
xcode-select --install

# Install Carthage
brew install carthage

# Verify
xcodebuild -version
xcrun simctl list devices

# Install ios-deploy (for real devices)
npm install -g ios-deploy

# Setup WebDriverAgent (run once)
appium driver run xcuitest build-wda
```

---

## Phase 2: Project Initialization

### Step 2.1: Create Project Structure

```bash
# Create project root directory
mkdir appium-framework
cd appium-framework

# Create Maven-standard directory structure
mkdir -p src/main/java/com/mobile/automation/{config,core,pages,utils,listeners}
mkdir -p src/main/resources/{config,testdata}
mkdir -p src/test/java/com/mobile/automation/tests
mkdir -p src/test/resources
mkdir -p docker
mkdir -p .github/workflows
```

**Verify structure:**
```bash
# Windows
tree /F

# Linux/macOS
tree
```

### Step 2.2: Initialize Git Repository

```bash
# Initialize Git
git init

# Create .gitignore
cat > .gitignore << 'EOF'
# Compiled class files
*.class
target/
*.jar
*.war

# IDE
.idea/
*.iml
.vscode/
.settings/

# Logs
*.log
logs/

# Test outputs
test-output/
allure-results/
screenshots/
videos/

# Environment
.env
apps/*.apk
apps/*.ipa

# macOS
.DS_Store
EOF

# Initial commit
git add .
git commit -m "Initial project structure"
```

---

## Phase 3: Maven Configuration

### Step 3.1: Create pom.xml

```bash
# In project root directory
touch pom.xml
```

Add the following content to `pom.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <!-- Project Coordinates -->
    <groupId>com.mobile.automation</groupId>
    <artifactId>appium-framework</artifactId>
    <version>1.0.0</version>
    <packaging>jar</packaging>

    <name>Appium Framework</name>
    <description>Enterprise-grade Appium automation framework</description>

    <!-- Properties -->
    <properties>
        <maven.compiler.source>17</maven.compiler.source>
        <maven.compiler.target>17</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        
        <!-- Dependency Versions -->
        <appium.version>9.2.2</appium.version>
        <selenium.version>4.18.1</selenium.version>
        <testng.version>7.9.0</testng.version>
        <allure.version>2.25.0</allure.version>
        <log4j.version>2.22.1</log4j.version>
        <jackson.version>2.16.1</jackson.version>
    </properties>

    <dependencies>
        <!-- Appium -->
        <dependency>
            <groupId>io.appium</groupId>
            <artifactId>java-client</artifactId>
            <version>${appium.version}</version>
        </dependency>

        <!-- Selenium -->
        <dependency>
            <groupId>org.seleniumhq.selenium</groupId>
            <artifactId>selenium-java</artifactId>
            <version>${selenium.version}</version>
        </dependency>

        <!-- TestNG -->
        <dependency>
            <groupId>org.testng</groupId>
            <artifactId>testng</artifactId>
            <version>${testng.version}</version>
        </dependency>

        <!-- Allure TestNG -->
        <dependency>
            <groupId>io.qameta.allure</groupId>
            <artifactId>allure-testng</artifactId>
            <version>${allure.version}</version>
        </dependency>

        <!-- Log4j2 -->
        <dependency>
            <groupId>org.apache.logging.log4j</groupId>
            <artifactId>log4j-core</artifactId>
            <version>${log4j.version}</version>
        </dependency>
        <dependency>
            <groupId>org.apache.logging.log4j</groupId>
            <artifactId>log4j-api</artifactId>
            <version>${log4j.version}</version>
        </dependency>

        <!-- Jackson for YAML/JSON -->
        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-databind</artifactId>
            <version>${jackson.version}</version>
        </dependency>
        <dependency>
            <groupId>com.fasterxml.jackson.dataformat</groupId>
            <artifactId>jackson-dataformat-yaml</artifactId>
            <version>${jackson.version}</version>
        </dependency>

        <!-- Commons IO -->
        <dependency>
            <groupId>commons-io</groupId>
            <artifactId>commons-io</artifactId>
            <version>2.15.1</version>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <!-- Compiler Plugin -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.12.1</version>
                <configuration>
                    <source>17</source>
                    <target>17</target>
                </configuration>
            </plugin>

            <!-- Surefire Plugin -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-surefire-plugin</artifactId>
                <version>3.2.5</version>
                <configuration>
                    <suiteXmlFiles>
                        <suiteXmlFile>src/test/resources/testng.xml</suiteXmlFile>
                    </suiteXmlFiles>
                </configuration>
            </plugin>

            <!-- Allure Plugin -->
            <plugin>
                <groupId>io.qameta.allure</groupId>
                <artifactId>allure-maven</artifactId>
                <version>2.12.0</version>
            </plugin>
        </plugins>
    </build>
</project>
```

### Step 3.2: Verify Maven Setup

```bash
# Clean and compile
mvn clean compile

# This should download all dependencies
# Verify target/ directory is created
```

---

## Phase 4: Configuration Management

### Step 4.1: Create YAML Configuration Files

**Create: `src/main/resources/config/qa-android.yaml`**

```yaml
environment: qa

appium:
  host: ${APPIUM_HOST:127.0.0.1}
  port: ${APPIUM_PORT:4723}
  timeout: 30

platform:
  name: Android
  version: ${PLATFORM_VERSION:14.0}
  automationName: UiAutomator2

device:
  name: ${DEVICE_NAME:Android Emulator}
  udid: ${DEVICE_UDID:}
  isRealDevice: false

app:
  path: ${APP_PATH:./apps/app.apk}
  package: ${APP_PACKAGE:com.example.app}
  activity: ${APP_ACTIVITY:com.example.app.MainActivity}
  autoGrantPermissions: true
  fullReset: false
  noReset: true
  newCommandTimeout: 300

capabilities:
  uiautomator2ServerLaunchTimeout: 60000
  adbExecTimeout: 60000

wait:
  implicit: 0
  explicit: 20
  polling: 500

execution:
  screenshotOnFailure: true
  videoRecording: false
  retryCount: 1
```

**Create: `src/main/resources/config/qa-ios.yaml`**

```yaml
environment: qa

appium:
  host: ${APPIUM_HOST:127.0.0.1}
  port: ${APPIUM_PORT:4723}
  timeout: 30

platform:
  name: iOS
  version: ${PLATFORM_VERSION:17.2}
  automationName: XCUITest

device:
  name: ${DEVICE_NAME:iPhone 15}
  udid: ${DEVICE_UDID:}
  isRealDevice: false

app:
  path: ${APP_PATH:./apps/app.app}
  bundleId: ${APP_BUNDLE_ID:com.example.app}
  autoGrantPermissions: true
  fullReset: false
  noReset: true
  newCommandTimeout: 300

capabilities:
  wdaLaunchTimeout: 120000
  useNewWDA: false

wait:
  implicit: 0
  explicit: 20
  polling: 500

execution:
  screenshotOnFailure: true
  videoRecording: false
  retryCount: 1
```

### Step 4.2: Create Test Data File

**Create: `src/main/resources/testdata/testdata.json`**

```json
{
  "testUsers": [
    {
      "username": "${TEST_USER_1:user@example.com}",
      "password": "${TEST_PASSWORD_1:password123}",
      "role": "customer"
    }
  ],
  "testData": {
    "validEmail": "test@example.com",
    "searchKeyword": "smartphone"
  }
}
```

### Step 4.3: Create ConfigManager Class

**Create: `src/main/java/com/mobile/automation/config/ConfigManager.java`**

```java
package com.mobile.automation.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class ConfigManager {
    
    private static ConfigManager instance;
    private Map<String, Object> config;
    
    private ConfigManager() {
        loadConfiguration();
    }
    
    public static synchronized ConfigManager getInstance() {
        if (instance == null) {
            instance = new ConfigManager();
        }
        return instance;
    }
    
    private void loadConfiguration() {
        String environment = System.getProperty("env", "qa");
        String platform = System.getProperty("platform", "android");
        String configFileName = environment + "-" + platform + ".yaml";
        
        try {
            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            InputStream inputStream = getClass().getClassLoader()
                    .getResourceAsStream("config/" + configFileName);
            
            config = mapper.readValue(inputStream, Map.class);
            config = resolveEnvironmentVariables(config);
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to load configuration", e);
        }
    }
    
    @SuppressWarnings("unchecked")
    private Map<String, Object> resolveEnvironmentVariables(Map<String, Object> map) {
        Map<String, Object> resolved = new HashMap<>();
        
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            Object value = entry.getValue();
            
            if (value instanceof String) {
                resolved.put(entry.getKey(), resolveEnvironmentVariable((String) value));
            } else if (value instanceof Map) {
                resolved.put(entry.getKey(), resolveEnvironmentVariables((Map<String, Object>) value));
            } else {
                resolved.put(entry.getKey(), value);
            }
        }
        
        return resolved;
    }
    
    private String resolveEnvironmentVariable(String value) {
        if (value == null || !value.contains("${")) {
            return value;
        }
        
        String result = value;
        int startIndex = value.indexOf("${");
        
        while (startIndex != -1) {
            int endIndex = value.indexOf("}", startIndex);
            if (endIndex == -1) break;
            
            String placeholder = value.substring(startIndex + 2, endIndex);
            String[] parts = placeholder.split(":", 2);
            String varName = parts[0];
            String defaultValue = parts.length > 1 ? parts[1] : "";
            
            String envValue = System.getenv(varName);
            if (envValue == null) {
                envValue = System.getProperty(varName, defaultValue);
            }
            
            result = result.replace("${" + placeholder + "}", envValue);
            startIndex = result.indexOf("${", endIndex);
        }
        
        return result;
    }
    
    @SuppressWarnings("unchecked")
    public <T> T get(String keyPath) {
        String[] keys = keyPath.split("\\.");
        Object value = config;
        
        for (String key : keys) {
            if (value instanceof Map) {
                value = ((Map<String, Object>) value).get(key);
            } else {
                return null;
            }
        }
        
        return (T) value;
    }
    
    public String getString(String keyPath) {
        Object value = get(keyPath);
        return value != null ? value.toString() : null;
    }
    
    public Integer getInt(String keyPath) {
        Object value = get(keyPath);
        if (value instanceof Integer) {
            return (Integer) value;
        } else if (value instanceof String) {
            return Integer.parseInt((String) value);
        }
        return null;
    }
    
    public Boolean getBoolean(String keyPath) {
        Object value = get(keyPath);
        if (value instanceof Boolean) {
            return (Boolean) value;
        } else if (value instanceof String) {
            return Boolean.parseBoolean((String) value);
        }
        return null;
    }
}
```

**Test the ConfigManager:**

```bash
# Compile the project
mvn clean compile

# This should compile without errors
```

---

## Phase 5: Core Components Implementation

### Step 5.1: Create CapabilitiesFactory

**Create: `src/main/java/com/mobile/automation/core/CapabilitiesFactory.java`**

```java
package com.mobile.automation.core;

import com.mobile.automation.config.ConfigManager;
import io.appium.java_client.android.options.UiAutomator2Options;
import io.appium.java_client.ios.options.XCUITestOptions;
import org.openqa.selenium.MutableCapabilities;

import java.io.File;
import java.time.Duration;
import java.util.Map;

public class CapabilitiesFactory {
    
    private final ConfigManager config;
    
    public CapabilitiesFactory() {
        this.config = ConfigManager.getInstance();
    }
    
    public MutableCapabilities createCapabilities() {
        String platformName = config.getString("platform.name");
        
        if ("Android".equalsIgnoreCase(platformName)) {
            return createAndroidCapabilities();
        } else if ("iOS".equalsIgnoreCase(platformName)) {
            return createIOSCapabilities();
        } else {
            throw new IllegalArgumentException("Unsupported platform: " + platformName);
        }
    }
    
    private UiAutomator2Options createAndroidCapabilities() {
        UiAutomator2Options options = new UiAutomator2Options();
        
        options.setPlatformName(config.getString("platform.name"));
        options.setPlatformVersion(config.getString("platform.version"));
        options.setAutomationName(config.getString("platform.automationName"));
        options.setDeviceName(config.getString("device.name"));
        
        String appPath = config.getString("app.path");
        if (appPath != null && !appPath.isEmpty()) {
            File appFile = new File(appPath);
            if (appFile.exists()) {
                options.setApp(appFile.getAbsolutePath());
            }
        }
        
        options.setAppPackage(config.getString("app.package"));
        options.setAppActivity(config.getString("app.activity"));
        options.setAutoGrantPermissions(config.getBoolean("app.autoGrantPermissions"));
        options.setNoReset(config.getBoolean("app.noReset"));
        
        return options;
    }
    
    private XCUITestOptions createIOSCapabilities() {
        XCUITestOptions options = new XCUITestOptions();
        
        options.setPlatformName(config.getString("platform.name"));
        options.setPlatformVersion(config.getString("platform.version"));
        options.setAutomationName(config.getString("platform.automationName"));
        options.setDeviceName(config.getString("device.name"));
        
        String appPath = config.getString("app.path");
        if (appPath != null && !appPath.isEmpty()) {
            File appFile = new File(appPath);
            if (appFile.exists()) {
                options.setApp(appFile.getAbsolutePath());
            }
        }
        
        options.setBundleId(config.getString("app.bundleId"));
        options.setAutoAcceptAlerts(config.getBoolean("app.autoGrantPermissions"));
        options.setNoReset(config.getBoolean("app.noReset"));
        
        return options;
    }
}
```

### Step 5.2: Create DriverFactory (ThreadLocal)

**Create: `src/main/java/com/mobile/automation/core/DriverFactory.java`**

```java
package com.mobile.automation.core;

import com.mobile.automation.config.ConfigManager;
import io.appium.java_client.AppiumDriver;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.ios.IOSDriver;
import org.openqa.selenium.MutableCapabilities;

import java.net.URL;
import java.time.Duration;

public class DriverFactory {
    
    // ThreadLocal driver - NO STATIC DRIVER!
    private static final ThreadLocal<AppiumDriver> driver = new ThreadLocal<>();
    private static final ConfigManager config = ConfigManager.getInstance();
    
    private DriverFactory() {
    }
    
    public static AppiumDriver getDriver() {
        if (driver.get() == null) {
            createDriver();
        }
        return driver.get();
    }
    
    private static void createDriver() {
        String platformName = config.getString("platform.name");
        
        try {
            URL serverUrl = new URL(String.format("http://%s:%d", 
                    config.getString("appium.host"),
                    config.getInt("appium.port")));
            
            MutableCapabilities capabilities = new CapabilitiesFactory().createCapabilities();
            
            AppiumDriver appiumDriver;
            
            if ("Android".equalsIgnoreCase(platformName)) {
                appiumDriver = new AndroidDriver(serverUrl, capabilities);
            } else if ("iOS".equalsIgnoreCase(platformName)) {
                appiumDriver = new IOSDriver(serverUrl, capabilities);
            } else {
                throw new IllegalArgumentException("Unsupported platform: " + platformName);
            }
            
            // Configure timeouts
            appiumDriver.manage().timeouts()
                    .implicitlyWait(Duration.ofSeconds(config.getInt("wait.implicit")));
            
            driver.set(appiumDriver);
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to create driver", e);
        }
    }
    
    public static void quitDriver() {
        if (driver.get() != null) {
            try {
                driver.get().quit();
            } catch (Exception e) {
                // Log error
            } finally {
                driver.remove();
            }
        }
    }
    
    public static boolean isDriverInitialized() {
        return driver.get() != null;
    }
}
```

### Step 5.3: Create BaseTest

**Create: `src/main/java/com/mobile/automation/core/BaseTest.java`**

```java
package com.mobile.automation.core;

import io.appium.java_client.AppiumDriver;
import org.testng.ITestResult;
import org.testng.annotations.*;

public class BaseTest {
    
    protected AppiumDriver driver;
    
    @BeforeMethod(alwaysRun = true)
    public void setUp(ITestResult result) {
        System.out.println("Starting test: " + result.getMethod().getMethodName());
        driver = DriverFactory.getDriver();
    }
    
    @AfterMethod(alwaysRun = true)
    public void tearDown(ITestResult result) {
        if (result.getStatus() == ITestResult.FAILURE) {
            System.out.println("Test FAILED: " + result.getMethod().getMethodName());
            // Capture screenshot here
        }
        
        DriverFactory.quitDriver();
        System.out.println("Completed test: " + result.getMethod().getMethodName());
    }
    
    protected AppiumDriver getDriver() {
        return driver;
    }
}
```

**Test Compilation:**

```bash
mvn clean compile

# Should compile successfully
```

---

## Phase 6: Utility Classes Development

### Step 6.1: Create WaitUtil

**Create: `src/main/java/com/mobile/automation/utils/WaitUtil.java`**

```java
package com.mobile.automation.utils;

import com.mobile.automation.config.ConfigManager;
import io.appium.java_client.AppiumDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

public class WaitUtil {
    
    private static final ConfigManager config = ConfigManager.getInstance();
    
    private WaitUtil() {
    }
    
    private static int getExplicitWaitTimeout() {
        return config.getInt("wait.explicit");
    }
    
    // NO Thread.sleep() - Only explicit waits!
    public static WebElement waitForVisibility(AppiumDriver driver, WebElement element) {
        WebDriverWait wait = new WebDriverWait(driver, 
                Duration.ofSeconds(getExplicitWaitTimeout()));
        return wait.until(ExpectedConditions.visibilityOf(element));
    }
    
    public static WebElement waitForClickability(AppiumDriver driver, WebElement element) {
        WebDriverWait wait = new WebDriverWait(driver, 
                Duration.ofSeconds(getExplicitWaitTimeout()));
        return wait.until(ExpectedConditions.elementToBeClickable(element));
    }
    
    public static boolean waitForInvisibility(AppiumDriver driver, WebElement element) {
        WebDriverWait wait = new WebDriverWait(driver, 
                Duration.ofSeconds(getExplicitWaitTimeout()));
        return wait.until(ExpectedConditions.invisibilityOf(element));
    }
}
```

### Step 6.2: Create GestureUtil

**Create: `src/main/java/com/mobile/automation/utils/GestureUtil.java`**

```java
package com.mobile.automation.utils;

import io.appium.java_client.AppiumDriver;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.Point;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.PointerInput;
import org.openqa.selenium.interactions.Sequence;

import java.time.Duration;
import java.util.Collections;

public class GestureUtil {
    
    private GestureUtil() {
    }
    
    public static void tap(AppiumDriver driver, WebElement element) {
        WaitUtil.waitForClickability(driver, element);
        element.click();
    }
    
    public static void swipe(AppiumDriver driver, int startX, int startY, 
                            int endX, int endY, int durationMs) {
        PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
        Sequence swipe = new Sequence(finger, 1);
        
        swipe.addAction(finger.createPointerMove(Duration.ZERO, 
                PointerInput.Origin.viewport(), startX, startY));
        swipe.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
        swipe.addAction(finger.createPointerMove(Duration.ofMillis(durationMs), 
                PointerInput.Origin.viewport(), endX, endY));
        swipe.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
        
        driver.perform(Collections.singletonList(swipe));
    }
    
    public static void swipeUp(AppiumDriver driver) {
        Dimension size = driver.manage().window().getSize();
        int startX = size.getWidth() / 2;
        int startY = (int) (size.getHeight() * 0.8);
        int endY = (int) (size.getHeight() * 0.2);
        
        swipe(driver, startX, startY, startX, endY, 800);
    }
    
    public static void swipeDown(AppiumDriver driver) {
        Dimension size = driver.manage().window().getSize();
        int startX = size.getWidth() / 2;
        int startY = (int) (size.getHeight() * 0.2);
        int endY = (int) (size.getHeight() * 0.8);
        
        swipe(driver, startX, startY, startX, endY, 800);
    }
}
```

### Step 6.3: Create ScreenshotUtil

**Create: `src/main/java/com/mobile/automation/utils/ScreenshotUtil.java`**

```java
package com.mobile.automation.utils;

import io.appium.java_client.AppiumDriver;
import org.apache.commons.io.FileUtils;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ScreenshotUtil {
    
    private static final String SCREENSHOT_DIR = "target/screenshots";
    
    static {
        try {
            Files.createDirectories(Paths.get(SCREENSHOT_DIR));
        } catch (IOException e) {
            // Handle error
        }
    }
    
    private ScreenshotUtil() {
    }
    
    public static byte[] captureScreenshot(AppiumDriver driver) {
        try {
            return ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
        } catch (Exception e) {
            return null;
        }
    }
    
    public static String captureScreenshotToFile(AppiumDriver driver, String testName) {
        try {
            String timestamp = LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String fileName = testName + "_" + timestamp + ".png";
            String filePath = SCREENSHOT_DIR + File.separator + fileName;
            
            File screenshotFile = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            FileUtils.copyFile(screenshotFile, new File(filePath));
            
            return filePath;
        } catch (IOException e) {
            return null;
        }
    }
}
```

### Step 6.4: Create RetryAnalyzer

**Create: `src/main/java/com/mobile/automation/utils/RetryAnalyzer.java`**

```java
package com.mobile.automation.utils;

import org.testng.IRetryAnalyzer;
import org.testng.ITestResult;

public class RetryAnalyzer implements IRetryAnalyzer {
    
    private int retryCount = 0;
    private static final int MAX_RETRY_COUNT = 1;
    
    @Override
    public boolean retry(ITestResult result) {
        if (retryCount < MAX_RETRY_COUNT) {
            Throwable throwable = result.getThrowable();
            
            // Only retry infrastructure failures, NOT assertion failures
            if (isInfrastructureFailure(throwable)) {
                retryCount++;
                System.out.println("Retrying test: " + result.getName() + 
                        " - Attempt " + retryCount);
                return true;
            }
        }
        return false;
    }
    
    private boolean isInfrastructureFailure(Throwable throwable) {
        if (throwable == null) {
            return false;
        }
        
        String className = throwable.getClass().getName();
        
        // Infrastructure exceptions - RETRY
        if (className.contains("TimeoutException") ||
            className.contains("WebDriverException") ||
            className.contains("NoSuchSessionException")) {
            return true;
        }
        
        // Assertion failures - DO NOT RETRY
        if (className.contains("AssertionError") ||
            className.contains("AssertionFailedError")) {
            return false;
        }
        
        return false;
    }
}
```

**Compile utilities:**

```bash
mvn clean compile
```

---

## Phase 7: Page Object Model

### Step 7.1: Create BasePage

**Create: `src/main/java/com/mobile/automation/pages/BasePage.java`**

```java
package com.mobile.automation.pages;

import com.mobile.automation.utils.GestureUtil;
import com.mobile.automation.utils.WaitUtil;
import io.appium.java_client.AppiumDriver;
import io.appium.java_client.pagefactory.AppiumFieldDecorator;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.PageFactory;

import java.time.Duration;

public class BasePage {
    
    protected AppiumDriver driver;
    
    public BasePage(AppiumDriver driver) {
        this.driver = driver;
        PageFactory.initElements(new AppiumFieldDecorator(driver, Duration.ofSeconds(10)), this);
    }
    
    // NO ASSERTIONS in page classes!
    
    protected void click(WebElement element) {
        WaitUtil.waitForClickability(driver, element);
        element.click();
    }
    
    protected void enterText(WebElement element, String text) {
        WaitUtil.waitForVisibility(driver, element);
        element.clear();
        element.sendKeys(text);
    }
    
    protected String getText(WebElement element) {
        WaitUtil.waitForVisibility(driver, element);
        return element.getText();
    }
    
    protected boolean isElementDisplayed(WebElement element) {
        try {
            WaitUtil.waitForVisibility(driver, element);
            return element.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }
    
    protected void tap(WebElement element) {
        GestureUtil.tap(driver, element);
    }
    
    protected void swipeUp() {
        GestureUtil.swipeUp(driver);
    }
}
```

### Step 7.2: Create Sample LoginPage

**Create: `src/main/java/com/mobile/automation/pages/LoginPage.java`**

```java
package com.mobile.automation.pages;

import io.appium.java_client.AppiumDriver;
import io.appium.java_client.pagefactory.AndroidFindBy;
import io.appium.java_client.pagefactory.iOSXCUITFindBy;
import org.openqa.selenium.WebElement;

public class LoginPage extends BasePage {
    
    // Platform-specific locators
    @AndroidFindBy(id = "com.example.app:id/email")
    @iOSXCUITFindBy(accessibility = "emailTextField")
    private WebElement emailField;
    
    @AndroidFindBy(id = "com.example.app:id/password")
    @iOSXCUITFindBy(accessibility = "passwordTextField")
    private WebElement passwordField;
    
    @AndroidFindBy(id = "com.example.app:id/loginButton")
    @iOSXCUITFindBy(accessibility = "loginButton")
    private WebElement loginButton;
    
    public LoginPage(AppiumDriver driver) {
        super(driver);
    }
    
    public LoginPage enterEmail(String email) {
        enterText(emailField, email);
        return this;
    }
    
    public LoginPage enterPassword(String password) {
        enterText(passwordField, password);
        return this;
    }
    
    public void clickLogin() {
        click(loginButton);
    }
    
    public void login(String email, String password) {
        enterEmail(email);
        enterPassword(password);
        clickLogin();
    }
    
    public boolean isLoginPageDisplayed() {
        return isElementDisplayed(emailField) && 
               isElementDisplayed(passwordField);
    }
}
```

**Compile pages:**

```bash
mvn clean compile
```

---

## Phase 8: Test Classes Creation

### Step 8.1: Create Sample Test

**Create: `src/test/java/com/mobile/automation/tests/LoginTest.java`**

```java
package com.mobile.automation.tests;

import com.mobile.automation.core.BaseTest;
import com.mobile.automation.pages.LoginPage;
import org.testng.Assert;
import org.testng.annotations.Test;

public class LoginTest extends BaseTest {
    
    @Test(priority = 1, description = "Verify successful login")
    public void testSuccessfulLogin() {
        System.out.println("Executing: testSuccessfulLogin");
        
        LoginPage loginPage = new LoginPage(driver);
        
        // Verify login page is displayed
        Assert.assertTrue(loginPage.isLoginPageDisplayed(), 
                "Login page should be displayed");
        
        // Perform login
        loginPage.login("user@example.com", "password123");
        
        // Add verification for successful login
        // Assert.assertTrue(homePage.isDisplayed());
    }
    
    @Test(priority = 2, description = "Verify login with invalid credentials")
    public void testInvalidLogin() {
        System.out.println("Executing: testInvalidLogin");
        
        LoginPage loginPage = new LoginPage(driver);
        loginPage.login("invalid@email.com", "wrongpassword");
        
        // Add verification for error message
    }
}
```

**Compile tests:**

```bash
mvn clean test-compile
```

---

## Phase 9: TestNG Configuration

### Step 9.1: Create TestNG XML

**Create: `src/test/resources/testng.xml`**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE suite SYSTEM "https://testng.org/testng-1.0.dtd">

<suite name="Smoke Test Suite" parallel="false" thread-count="1" verbose="1">
    
    <test name="Android Smoke Tests" preserve-order="true">
        <parameter name="platform" value="android"/>
        <parameter name="env" value="qa"/>
        <classes>
            <class name="com.mobile.automation.tests.LoginTest"/>
        </classes>
    </test>
    
</suite>
```

---

## Phase 10: Listeners Implementation

### Step 10.1: Create TestListener

**Create: `src/main/java/com/mobile/automation/listeners/TestListener.java`**

```java
package com.mobile.automation.listeners;

import com.mobile.automation.core.DriverFactory;
import com.mobile.automation.utils.ScreenshotUtil;
import io.qameta.allure.Allure;
import io.qameta.allure.Attachment;
import org.testng.*;

import java.io.ByteArrayInputStream;

public class TestListener implements ITestListener {
    
    @Override
    public void onStart(ITestContext context) {
        System.out.println("===========================================");
        System.out.println("Starting Test Suite: " + context.getName());
        System.out.println("===========================================");
    }
    
    @Override
    public void onFinish(ITestContext context) {
        System.out.println("===========================================");
        System.out.println("Test Suite Finished: " + context.getName());
        System.out.println("Total Tests: " + context.getAllTestMethods().length);
        System.out.println("Passed: " + context.getPassedTests().size());
        System.out.println("Failed: " + context.getFailedTests().size());
        System.out.println("Skipped: " + context.getSkippedTests().size());
        System.out.println("===========================================");
    }
    
    @Override
    public void onTestStart(ITestResult result) {
        System.out.println("STARTING TEST: " + result.getMethod().getMethodName());
    }
    
    @Override
    public void onTestSuccess(ITestResult result) {
        System.out.println("TEST PASSED: " + result.getMethod().getMethodName());
    }
    
    @Override
    public void onTestFailure(ITestResult result) {
        System.out.println("TEST FAILED: " + result.getMethod().getMethodName());
        System.out.println("Reason: " + result.getThrowable().getMessage());
        
        // Capture screenshot on failure
        captureFailureScreenshot(result);
    }
    
    @Override
    public void onTestSkipped(ITestResult result) {
        System.out.println("TEST SKIPPED: " + result.getMethod().getMethodName());
    }
    
    @Attachment(value = "Failure Screenshot", type = "image/png")
    private byte[] captureFailureScreenshot(ITestResult result) {
        try {
            if (DriverFactory.isDriverInitialized()) {
                byte[] screenshot = ScreenshotUtil.captureScreenshot(DriverFactory.getDriver());
                
                // Attach to Allure report
                if (screenshot != null) {
                    Allure.addAttachment(
                        result.getMethod().getMethodName() + "_failure",
                        new ByteArrayInputStream(screenshot)
                    );
                }
                
                return screenshot;
            }
        } catch (Exception e) {
            System.out.println("Failed to capture screenshot: " + e.getMessage());
        }
        return null;
    }
}
```

### Step 10.2: Create RetryListener

**Create: `src/main/java/com/mobile/automation/listeners/RetryListener.java`**

```java
package com.mobile.automation.listeners;

import com.mobile.automation.utils.RetryAnalyzer;
import org.testng.IAnnotationTransformer;
import org.testng.annotations.ITestAnnotation;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public class RetryListener implements IAnnotationTransformer {
    
    @Override
    public void transform(ITestAnnotation annotation, Class testClass, 
                         Constructor testConstructor, Method testMethod) {
        // Automatically add RetryAnalyzer to all test methods
        annotation.setRetryAnalyzer(RetryAnalyzer.class);
    }
}
```

### Step 10.3: Create TestDataReader Utility

**Create: `src/main/java/com/mobile/automation/utils/TestDataReader.java`**

```java
package com.mobile.automation.utils;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

public class TestDataReader {
    
    private static Map<String, Object> testData;
    
    static {
        loadTestData();
    }
    
    private TestDataReader() {
    }
    
    private static void loadTestData() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            InputStream inputStream = TestDataReader.class.getClassLoader()
                    .getResourceAsStream("testdata/testdata.json");
            
            testData = mapper.readValue(inputStream, Map.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load test data", e);
        }
    }
    
    @SuppressWarnings("unchecked")
    public static <T> T get(String keyPath) {
        String[] keys = keyPath.split("\\.");
        Object value = testData;
        
        for (String key : keys) {
            if (value instanceof Map) {
                value = ((Map<String, Object>) value).get(key);
            } else {
                return null;
            }
        }
        
        return (T) value;
    }
    
    @SuppressWarnings("unchecked")
    public static Map<String, Object> getTestUser(int index) {
        List<Map<String, Object>> users = get("testUsers");
        if (users != null && index < users.size()) {
            return users.get(index);
        }
        return null;
    }
    
    public static String getString(String keyPath) {
        Object value = get(keyPath);
        return value != null ? value.toString() : null;
    }
}
```

**Compile listeners:**

```bash
mvn clean compile
```

---

## Phase 11: Logging Setup

### Step 11.1: Create Log4j2 Configuration

**Create: `src/test/resources/log4j2.xml`**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<Configuration status="WARN">
    <Properties>
        <Property name="LOG_PATTERN">%d{yyyy-MM-dd HH:mm:ss} [%t] %-5level %logger{36} - %msg%n</Property>
        <Property name="LOG_DIR">target/logs</Property>
    </Properties>

    <Appenders>
        <Console name="Console" target="SYSTEM_OUT">
            <PatternLayout pattern="${LOG_PATTERN}"/>
        </Console>

        <RollingFile name="FileLogger" fileName="${LOG_DIR}/automation.log"
                     filePattern="${LOG_DIR}/automation-%d{yyyy-MM-dd}.log">
            <PatternLayout pattern="${LOG_PATTERN}"/>
            <Policies>
                <TimeBasedTriggeringPolicy/>
                <SizeBasedTriggeringPolicy size="10MB"/>
            </Policies>
        </RollingFile>
    </Appenders>

    <Loggers>
        <Root level="info">
            <AppenderRef ref="Console"/>
            <AppenderRef ref="FileLogger"/>
        </Root>
    </Loggers>
</Configuration>
```

---

## Phase 12: Testing and Validation

### Step 12.1: Prepare Test Environment

```bash
# Create apps directory
mkdir apps

# Copy your app file
# cp /path/to/your/app.apk apps/

# Start Appium server
appium --address 127.0.0.1 --port 4723
```

### Step 11.2: Run Tests

```bash
# In a new terminal

# Run tests with Maven
mvn clean test -Dplatform=android -Denv=qa

# View results in target/surefire-reports/
```

---

## Phase 13: Allure Reporting

### Step 13.1: Add Allure Annotations

Update `LoginTest.java`:

```java
import io.qameta.allure.*;

@Epic("Authentication")
@Feature("Login")
public class LoginTest extends BaseTest {
    
    @Test(priority = 1)
    @Severity(SeverityLevel.BLOCKER)
    @Description("Verify user can login with valid credentials")
    @Step("Execute successful login test")
    public void testSuccessfulLogin() {
        // test code
    }
}
```

### Step 13.2: Update POM for Allure AspectJ Weaver

Update your `pom.xml` to add AspectJ weaver configuration in the Surefire plugin:

```xml
<!-- Surefire Plugin with AspectJ -->
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
    <version>3.2.5</version>
    <configuration>
        <suiteXmlFiles>
          3.3: Update TestNG XML with Listeners

Update `src/test/resources/testng.xml` to include listeners:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE suite SYSTEM "https://testng.org/testng-1.0.dtd">

<suite name="Smoke Test Suite" parallel="false" thread-count="1" verbose="1">
    
    <!-- Add Listeners -->
    <listeners>
        <listener class-name="com.mobile.automation.listeners.TestListener"/>
        <listener class-name="com.mobile.automation.listeners.RetryListener"/>
        <listener class-name="io.qameta.allure.testng.AllureTestNg"/>
    </listeners>
    
    <test name="Android Smoke Tests" preserve-order="true">
        <parameter name="platform" value="android"/>
        <parameter name="env" value="qa"/>
        <classes>
            <class name="com.mobile.automation.tests.LoginTest"/>
        </classes>
    </test>
    
</suite>
```

### Step 13.4: Generate Allure Report

```bash
# Run tests
mvn clean test

# Generate and open report
mvn allure:serve
```

---

## Phase 14 <artifactId>aspectjweaver</artifactId>
            <version>1.9.21</version>
        </dependency>
    </dependencies>
</plugin>
```

Also verify Allure dependencies are present:

```xml
<dependency>
    <groupId>io.qameta.allure</groupId>
    <artifactId>allure-testng</artifactId>
    <version>2.25.0</version>
</dependency>
```

### Step 12.3: Generate Allure Report

```bash
# Run tests
mvn clean test

# Generate and open report
mvn allure:serve
```

---

## Phase 13: CI/CD Integration
4
### Step 13.1: Create Jenkinsfile

**Create: `Jenkinsfile`** (in project root)

```groovy
pipeline {
    agent any
    
    parameters {
        choice(name: 'PLATFORM', choices: ['android', 'ios'], description: 'Platform')
        choice(name: 'ENVIRONMENT', choices: ['qa', 'dev'], description: 'Environment')
    }
    
    tools {
        maven 'Maven-3.9.0'
        jdk 'JDK-17'
    }
    
    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }
        
        stage('Build') {
            steps {
                sh 'mvn clean compile'
            }
        }
        
        stage('Run Tests') {
            steps {
                sh """
                    mvn test \
                        -Dplatform=${params.PLATFORM} \
                        -Denv=${params.ENVIRONMENT}
                """
            }
        }
        
        stage('Generate Report') {
            steps {
                sh 'mvn allure:report'
            }
        }
    }
    
    post {
        always {
            junit '**/target/surefire-reports/*.xml'
            allure includeProperties: false, results: [[path: 'target/allure-results']]
        }
    }
}
```
4
### Step 13.2: Create GitHub Actions Workflow

**Create: `.github/workflows/test-automation.yml`**

```yaml
name: Mobile Test Automation

on:
  push:
    branches: [ main ]
  pull_request:
    branches: [ main ]

jobs:
  test:
    runs-on: ubuntu-latest
    
    steps:
      - name: Checkout code
        uses: actions/checkout@v3
      
      - name: Set up JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'temurin'
      
      - name: Install Appium
        run: |
          npm install -g appium@next
          appium driver install uiautomator2
      
      - name: Start Appium
        run: |
          appium &
          sleep 10
      
      - name: Run tests
        run: mvn test -Dplatform=android -Denv=qa
      
      - name: Generate Allure Report
        if: always()
        run: mvn allure:report
      
      - name: Upload Results
        if: always()
        uses: actions/upload-artifact@v3
        with:
          name: allure-results
          path: target/allure-results
```

### Step 14.3: Create Docker Support

**Create: `docker/Dockerfile`**

```dockerfile
FROM appium/appium:latest

RUN apt-get update && apt-get install -y \
    openjdk-17-jdk \
    maven
5: Final Validation and Best Practices

### Step 15
COPY . /app

EXPOSE 4723

CMD ["appium"]
```

**Create: `docker/docker-compose.yml`**

```yaml
version: '3.8'

services:
  appium-server:
    build:
      context: ..
      dockerfile: docker/Dockerfile
    ports:
      - "4723:4723"
    volumes:
      - ../apps:/app/apps
```

---

## Phase 14: Final Validation and Best Practices

### Step 14.1: Create README

**Create: `README.md`**

```markdown
# Appium Framework

## Setup

1. Install Java 17
2. Install Maven
3. Install Node.js and Appium
4. Clone repository
5. Run `mvn clean install -DskipTests`

## Run Tests

```bash
# Start Appium
appium

# Run tests
mvn test -Dplatform=android -Denv=qa
```

## Generate Reports

```bash
mvn allure:serve
```
```

### Step 15.2: Create Setup Scripts

**Create: `setup.sh` (Linux/macOS)**

```bash
#!/bin/bash

echo "==================================="
echo "Appium Framework Setup Script"
echo "==================================="

# Check Java
if ! command -v java &> /dev/null; then
    echo "❌ Java not found. Please install JDK 17"
    exit 1
fi
echo "✅ Java found: $(java -version 2>&1 | head -n 1)"

# Check Maven
if ! comma5.4-v mvn &> /dev/null; then
    echo "❌ Maven not found. Please install Maven"
    exit 1
fi
echo "✅ Maven found: $(mvn -version | head -n 1)"

# Check Node.js
if ! command -v node &> /dev/null; then
    echo "❌ Node.js not found. Please install Node.js"
    exit 1
fi
echo "✅ Node.js found: $(node --version)"

# Check Appium
if ! command -v appium &> /dev/null; then
    echo "❌ Appium not found. Installing..."
    npm install -g appium@next
fi
echo "✅ Appium found: $(appium --version)"

# Install Appium drivers
echo "Installing Appium drivers..."
appium driver install uiautomator2
appium driver install xcuitest 2>/dev/null

# Create directories
echo "Creating directories..."
mkdir -p apps
mkdir -p target/logs
mkdir -p target/screenshots

# Build project
echo "Building Maven project..."
mvn clean install -DskipTests

echo "==================================="
echo "✅ Setup completed successfully!"
echo "==================================="
echo ""
echo "Next steps:"
echo "1. Place your app file in the 'apps/' directory"
echo "2. Update config files in src/main/resources/config/"
echo "3. Start Appium: appium --address 127.0.0.1 --port 4723"
echo "4. Run tests: mvn test -Dplatform=android -Denv=qa"
```

**Create: `setup.bat` (Windows)**

```batch
@echo off
echo ===================================
echo Appium Framework Setup Script
echo ===================================

:: Check Java
java -version >nul 2>&1
if errorlevel 1 (
    echo X Java not found. Please install JDK 17
    exit /b 1
)
echo ✓ Java found

:: Check Maven
mvn -version >nul 2>&1
if errorlevel 1 (
    echo X Maven not found. Please install Maven
    exit /b 1
)
echo ✓ Maven found

:: Check Node.js
node --version >nul 2>&1
if errorlevel 1 (
    echo X Node.js not found. Please install Node.js
    exit /b 1
)
echo ✓ Node.js found

:: Check Appium
appium --version >nul 2>&1
if errorlevel 1 (
    echo Installing Appium...
    call npm install -g appium@next
)
echo ✓ Appium found

:: Install Appium drivers
echo Installing Appium drivers...
call appium driver install uiautomator2

:: Create directories
echo Creating directories...
if not exist "apps" mkdir apps
if not exist "target\logs" mkdir target\logs
if not exist "target\screenshots" mkdir target\screenshots

:: Build project
echo Building Maven project...
call mvn clean install -DskipTests

echo ===================================
echo ✓ Setup completed successfully!
echo ===================================
echo.
echo Next steps:
echo 1. Place your app file in the 'apps' directory
echo 2. Update config files in src\main\resources\config\
echo 3. Start Appium: appium --address 127.0.0.1 --port 4723
echo 4. Run tests: mvn test -Dplatform=android -Denv=qa
```

Make the shell script executable:
```bash
chmod +x setup.sh
```

### Step 15.3: Validate Everything

```bash
# Clean build
mvn clean install -DskipTests

# Verify structure
tree -L 3

# Start Appium
appium

# Run tests (in new terminal)
mvn clean test -Dplatform=android -Denv=qa

# Generate report
mvn allure:serve
```

### Step 14.3: Verify Best Practices

✅ **NO static drivers** - Using ThreadLocal  
✅ **NO Thread.sleep()** - Using explicit waits  
✅ **NO hardcoded values** - Using YAML config  
✅ **ThreadLocal management** - In DriverFactory  
✅ **Proper cleanup** - In tearDown methods  
✅ **Smart retry** - Only infrastructure failures  

---

## 🎉 Framework Complete!

You've now built a complete enterprise-grade Appium framework from scratch!

### Next Steps:

1. **Add more page objects** for your app screens
2. **Write more test scenarios**
3. **Configure CI/CD** in Jenkins/GitHub Actions
4. **Add more utilities** as needed
5. **Integrate with cloud providers** (BrowserStack/SauceLabs)

### Project Checklist:

- [x] Maven project setup
- [x] Configuration management
- [x] Driver factory (ThreadLocal)
- [x] Utility classes
- [x] Page Object Model
- [x] Test classes
- [x] TestNG configuration
- [x] Logging setup
- [x] Allure reporting
- [x] CI/CD pipelines
- [x] Docker support
- [x] Documentation

---

## 📚 Additional Resources

- [Appium Documentation](https://appium.io/docs/en/latest/)
- [Selenium Documentation](https://www.selenium.dev/documentation/)
- [TestNG Documentation](https://testng.org/doc/)
- [Allure Documentation](https://docs.qameta.io/allure/)

---

**Happy Testing! 🚀**
