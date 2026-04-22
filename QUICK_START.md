# Quick Start Guide

## 🚀 Getting Started in 5 Minutes

### 1. Install Prerequisites

```bash
# Install Appium
npm install -g appium@next
appium driver install uiautomator2

# Verify installation
appium -v
```

### 2. Setup Project

```bash
# Clone and build
git clone <repository-url>
cd apium-java
mvn clean install -DskipTests
```

### 3. Configure Environment

Create a `.env` file:
```bash
DEVICE_NAME=Pixel_7_Pro
PLATFORM_VERSION=14.0
APP_PATH=./apps/your-app.apk
APP_PACKAGE=com.example.app
APP_ACTIVITY=com.example.app.MainActivity
```

### 4. Place Your App

```bash
mkdir apps
# Copy your APK/IPA to apps/ directory
```

### 5. Run Tests

**Terminal 1 - Start Appium:**
```bash
appium --address 127.0.0.1 --port 4723
```

**Terminal 2 - Run Tests:**
```bash
mvn test -Dplatform=android -Denv=qa
```

### 6. View Reports

```bash
mvn allure:serve
```

## 📱 Device Setup

### Android Emulator

```bash
# List available emulators
emulator -list-avds

# Start emulator
emulator -avd Pixel_7_Pro_API_34

# Check connected devices
adb devices
```

### iOS Simulator

```bash
# List simulators
xcrun simctl list devices

# Boot simulator
xcrun simctl boot "iPhone 15"

# Open simulator app
open -a Simulator
```

## 🧪 Running Specific Tests

```bash
# Smoke tests only
mvn test -DsuiteXmlFile=src/test/resources/testng.xml

# Regression tests
mvn test -DsuiteXmlFile=src/test/resources/testng-regression.xml

# Specific test class
mvn test -Dtest=LoginTest

# Specific test method
mvn test -Dtest=LoginTest#testSuccessfulLogin

# With parallel execution
mvn test -DthreadCount=3
```

## 🔧 Common Commands

```bash
# Clean and compile
mvn clean compile

# Run tests without logs
mvn test -q

# Skip tests
mvn clean install -DskipTests

# Debug mode
mvn test -X

# Generate reports only
mvn allure:report
mvn allure:serve
```

## 📊 Understanding Reports

### Allure Report Structure
- **Overview** - Test execution summary
- **Suites** - Tests organized by suite
- **Graphs** - Visual representation of results
- **Timeline** - Test execution timeline
- **Behaviors** - Tests grouped by Epic/Feature/Story

### Accessing Reports
1. **Allure**: `target/allure-reports/index.html`
2. **TestNG**: `target/surefire-reports/index.html`
3. **Logs**: `target/logs/test-execution.log`
4. **Screenshots**: `target/screenshots/`

## ⚙️ Configuration Switching

### Different Environments

```bash
# QA Environment
mvn test -Denv=qa

# Dev Environment
mvn test -Denv=dev

# Stage Environment
mvn test -Denv=stage
```

### Different Platforms

```bash
# Android
mvn test -Dplatform=android

# iOS
mvn test -Dplatform=ios
```

### Remote Execution (BrowserStack/SauceLabs)

```bash
# Set environment variables
export BS_USERNAME=your_username
export BS_ACCESS_KEY=your_key

# Run tests
mvn test -Dserver.type=remote
```

## 🐛 Quick Troubleshooting

### Problem: Appium won't start
```bash
# Kill existing processes
pkill -f appium
# or on Windows
taskkill /F /IM node.exe

# Start fresh
appium --address 127.0.0.1 --port 4723
```

### Problem: Device not found
```bash
# Android
adb devices
adb kill-server
adb start-server

# iOS
xcrun simctl list devices
```

### Problem: App won't install
```bash
# Android - Uninstall first
adb uninstall com.example.app

# iOS - Reset simulator
xcrun simctl erase all
```

### Problem: Tests timing out
- Increase timeout in config YAML
- Check network connectivity
- Verify app is responding

## 📚 Next Steps

1. ✅ Customize [page objects](src/main/java/com/mobile/automation/pages/)
2. ✅ Add your [locators](src/main/java/com/mobile/automation/pages/)
3. ✅ Write [test cases](src/test/java/com/mobile/automation/tests/)
4. ✅ Configure [CI/CD pipeline](Jenkinsfile)
5. ✅ Review [best practices](README.md#best-practices)

## 💡 Tips

- Always use explicit waits
- Keep tests independent
- Use Page Object Model
- Add meaningful assertions
- Use Allure annotations
- Handle exceptions properly
- Keep logs clean and informative

---

For detailed documentation, see [README.md](README.md)
