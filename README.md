[![SonarQube Cloud](https://sonarcloud.io/images/project_badges/sonarcloud-light.svg)](https://sonarcloud.io/summary/new_code?id=SoftwareJCompany_Jbolt)
# ⚡ JBolt

**JBolt** is a high-performance, native REST API client and stress-testing tool built for developers who need **raw power**, **low memory footprint**, and **full control**.

Unlike Electron-based alternatives, JBolt leverages **Java 25**, **JavaFX**, and **Virtual Threads (Project Loom)** to handle massive concurrency during load tests — without freezing your machine.

---

## 🚀 Key Features

- **Native Performance**
  Runs on the JVM with native rendering (Metal on macOS, DirectX on Windows).

- **Built-in Stress Testing**
  Load generator powered by **Virtual Threads**, capable of simulating thousands of concurrent requests.

- **Collection Management**
  Import and export **Postman collections (v2.1)**.

- **Privacy First**
  100% offline. No cloud, no accounts, no telemetry.

---

## 🛠 Tech Stack

- **Language:** Java 25
- **UI Framework:** JavaFX 25 + AtlantaFX (modern UI, dark mode ready)
- **Build System:** Gradle (Kotlin DSL)
- **Testing:** JUnit 5, TestFX

---

## 🏗 Setup & Build

### Prerequisites
- **JDK 25** installed

### Run locally

```bash
# Clone the repository
git clone https://github.com/CamilYed/JBolt.git
cd JBolt

# Run the application
./gradlew run
```
