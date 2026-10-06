---
name: Spring workflow process cleanup
description: Handling a file-backed H2 lock when migrating a Spring Boot app between Replit workflows.
---

When replacing or removing a custom workflow that runs Spring Boot through Maven, check for a surviving Maven/JVM child before starting the new managed artifact workflow. An orphaned app process can retain the file-backed H2 lock even though the old workflow is gone.

**Why:** the previous workflow removal did not terminate its Java child, and the managed service then failed to open the same H2 database.

**How to apply:** after a workflow migration, inspect running processes and confirm the old app has stopped before starting a replacement that uses the same H2 file.
