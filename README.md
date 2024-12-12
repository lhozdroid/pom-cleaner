# POM Cleaner Maven Plugin

The **POM Cleaner Maven Plugin** is a Maven tool designed to streamline and enhance the maintainability of your `pom.xml` file. It provides functionalities to:

- Organize your `pom.xml` by extracting hardcoded version numbers of dependencies and plugins into the `<properties>` section and sorting entries for better readability.
- Revert the `pom.xml` by replacing property references with the actual version values.

---

## Table of Contents

1. [Features](#features)
2. [Installation](#installation)
3. [Usage](#usage)
    - [Organize Goal (`organize`)](#organize-goal-organize)
    - [Revert Goal (`revert`)](#revert-goal-revert)
4. [Goals](#goals)
5. [Prerequisites](#prerequisites)
6. [License](#license)

---

## Features

1. **Organize (`organize`)**
    - Extracts hardcoded versions of dependencies and plugins into the `<properties>` section.
    - Sorts dependencies alphabetically and by scope for better readability.
    - Invokes the `tidy:pom` goal to ensure a clean, consistent POM structure.

2. **Revert (`revert`)**
    - Replaces property references for dependencies and plugins with their corresponding actual values.
    - Removes unused properties from the `<properties>` section.
    - Invokes the `tidy:pom` goal to maintain a well-structured POM file.

---

## Installation

To include the plugin in your Maven project, add the following to your `pom.xml`:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>pom-cleaner-maven-plugin</artifactId>
    <version>0.0.1</version>
</plugin>
```

---

## Usage

### Organize Goal (`organize`)

This goal organizes your `pom.xml` by performing the following tasks:
1. Extracts hardcoded versions of dependencies and plugins into the `<properties>` section.
2. Sorts dependencies alphabetically and by scope.
3. Invokes the `tidy:pom` goal to ensure consistency.

**Command:**

```bash
mvn pom-cleaner:organize
```

**Example Transformation:**

Before:

```xml
<dependency>
    <groupId>org.apache.maven</groupId>
    <artifactId>maven-core</artifactId>
    <version>3.8.1</version>
</dependency>
```

After:

```xml
<dependency>
    <groupId>org.apache.maven</groupId>
    <artifactId>maven-core</artifactId>
    <version>${org.apache.maven.maven-core}</version>
</dependency>

<properties>
    <org.apache.maven.maven-core>3.8.1</org.apache.maven.maven-core>
</properties>
```

---

### Revert Goal (`revert`)

This goal reverts the changes made by the `organize` goal by:
1. Replacing property references with their corresponding actual values.
2. Removing properties that are no longer used.
3. Invoking the `tidy:pom` goal to maintain a clean structure.

**Command:**

```bash
mvn pom-cleaner:revert
```

**Example Transformation:**

Before:

```xml
<dependency>
    <groupId>org.apache.maven</groupId>
    <artifactId>maven-core</artifactId>
    <version>${org.apache.maven.maven-core}</version>
</dependency>

<properties>
    <org.apache.maven.maven-core>3.8.1</org.apache.maven.maven-core>
</properties>
```

After:

```xml
<dependency>
    <groupId>org.apache.maven</groupId>
    <artifactId>maven-core</artifactId>
    <version>3.8.1</version>
</dependency>
```

---

## Goals

### 1. `organize`
- Default Phase: `process-sources`
- Extracts hardcoded versions into `<properties>`.
- Sorts dependencies and invokes `tidy:pom`.

### 2. `revert`
- Default Phase: `process-sources`
- Replaces property references with actual values.
- Removes unused properties and invokes `tidy:pom`.

---

## Prerequisites

Ensure the following are installed:
- **Java Development Kit (JDK)**: Version 8 or higher.
- **Apache Maven**: Version 3.0 or higher.

---

## License

This project is licensed under the MIT License.

---
