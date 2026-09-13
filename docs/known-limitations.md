<!--
SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
SPDX-License-Identifier: Apache-2.0
-->

# Known limitations

## Non-deterministic formatter output across repeated runs

Palantir formatter can produce non-idempotent output for some long or heavily wrapped constructs.
In observed cases this affects **both comment indentation and code layout** between consecutive runs.
This behavior is upstream in Palantir formatter and is not introduced by JHarmonizer.

### Concrete examples

Trailing `//` comment indentation can shift:

```java
// pass 1
assertEquals(
    2L,
    recovery.getMaxTransactionId()); // transaction ID is still 2 because that's what was written to the
                                     // journal

// pass 2
assertEquals(
    2L,
    recovery.getMaxTransactionId()); // transaction ID is still 2 because that's what was written to the
// journal
```

Fluent call wrapping can change:

```java
// pass 1
factory.setShutdownQuietPeriod(
        Duration
                .ZERO); // Quiet period not necessary since sending threads will have completed before shutting
                        // down event sender

// pass 2
factory.setShutdownQuietPeriod(
        Duration.ZERO); // Quiet period not necessary since sending threads will have completed before shutting
        // down event sender
```

String-concatenation indentation in annotations can shift:

```java
// pass 1
@WritesAttribute(
        attribute = "http.headers.XXX",
        description =
                "Each of the HTTP Headers that is received in the request will be added as an attribute, prefixed"
                        + " with \"http.headers.\" For example, if the request contains an HTTP Header named"
                        + " \"x-my-header\", then the value will be added to an attribute named"
                        + " \"http.headers.x-my-header\""),

// pass 2
@WritesAttribute(
        attribute = "http.headers.XXX",
        description =
                "Each of the HTTP Headers that is received in the request will be added as an attribute, prefixed"
                            + " with \"http.headers.\" For example, if the request contains an HTTP Header named"
                            + " \"x-my-header\", then the value will be added to an attribute named"
                            + " \"http.headers.x-my-header\""),
```

Aspect pointcut annotation wrapping can change:

```java
// pass 1
@Around(
        "within(org.apache.nifi.web.dao.ProcessGroupDAO+) && execution(void enableComponents(String,"
                + " org.apache.nifi.controller.ScheduledState, java.util.Set<String>)) && args(groupId, state,"
                + " componentIds)")
public void enableComponentsAdvice(

// pass 2
@Around("within(org.apache.nifi.web.dao.ProcessGroupDAO+) && execution(void enableComponents(String,"
        + " org.apache.nifi.controller.ScheduledState, java.util.Set<String>)) && args(groupId, state,"
        + " componentIds)")
public void enableComponentsAdvice(
        ProceedingJoinPoint proceedingJoinPoint, String groupId, ScheduledState state, Set<String> componentIds)
        throws Throwable {
```

### Workarounds

1. **Inline comments:** avoid long trailing `//` comments; move long notes to a standalone line or a short block comment above the statement.
2. **String concatenations:** prefer extracting long literals into named constants or helper variables so the formatter has fewer fragile wrap points.
3. **Annotations and pointcuts:** keep argument values and expression strings shorter per line where practical to reduce wrap oscillation risk.
4. **Persistent oscillation:** use `// @jharmonizer:sort-off` (keeps formatting but disables sorting) or `// @jharmonizer:fully-off` (disables all harmonization) as a temporary mitigation.

---

## Maven archetype template placeholders

Files that contain template placeholders such as `package ${package};` are not valid Java source.
Palantir formatter cannot parse them; JHarmonizer leaves them unmodified, marks them with an ERROR
result, and logs a warning. The overall run continues and the CLI still exits with status 0.

### Options

1. Add `// @jharmonizer:fully-off` as the first line of the template file to skip harmonization
   (recommended when you want clean runs without formatter warnings).
2. Leave the file without an opt-out directive and accept that each run will report a per-file ERROR
   and warning; the file is skipped and left unmodified.
