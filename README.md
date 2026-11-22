# Localization Tools Maven Plugin

[![Apache License, Version 2.0, January 2004](https://img.shields.io/github/license/mojohaus/l10n-maven-plugin.svg?label=License)](https://www.apache.org/licenses/LICENSE-2.0)
[![Maven Central](https://img.shields.io/maven-central/v/org.codehaus.mojo/l10n-maven-plugin.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/org.codehaus.mojo/l10n-maven-plugin)
[![GitHub CI](https://github.com/mojohaus/l10n-maven-plugin/actions/workflows/maven.yml/badge.svg?branch=master)](https://github.com/mojohaus/l10n-maven-plugin/actions/workflows/maven.yml)

The Localization Tools Maven Plugin adds a report to the Maven site that shows how complete the translations of
a project are. It lists the `*.properties` resource bundles and, for each configured locale, the keys that are
missing, extra or still the same as in the default bundle.

It requires Maven 3.9.0 or later and Java 8 or later. Add it under `<reporting>` and run `mvn site`:

```xml
<reporting>
  <plugins>
    <plugin>
      <groupId>org.codehaus.mojo</groupId>
      <artifactId>l10n-maven-plugin</artifactId>
      <version><!-- latest release --></version>
      <configuration>
        <locales>
          <locale>de</locale>
          <locale>fr</locale>
        </locales>
      </configuration>
    </plugin>
  </plugins>
</reporting>
```

The [plugin site](https://www.mojohaus.org/l10n-maven-plugin/) has the configuration options and a sample report.

Conventions for contributors and agents are in [AGENTS.md](AGENTS.md).
