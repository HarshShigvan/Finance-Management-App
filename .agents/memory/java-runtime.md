---
name: Replit Java runtime
description: Java/Maven version compatibility for this project workspace.
---

Target Java 17 in Maven projects here. The installed Replit Java tools module provides GraalVM Java 19, so targeting 21 fails compilation while Java 17 source compatibility runs on the available runtime.

**Why:** the module label does not state the actual JDK version, and its runtime is older than current Spring Boot examples often target.

**How to apply:** check `java -version` before selecting the Maven compiler release; prefer Java 17 unless the installed runtime changes.
