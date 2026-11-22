title: Introduction
author:
- Milos Kleint
date: 2007-08-26

<!--
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
-->

Localization Tools Maven Plugin
===============================

The Localization Tools Maven Plugin helps with internationalization and
localization of your projects.

Generate the Report
-------------------

To generate the localization report, just add the following in your project's
`pom.xml`:

```xml
<project>
  ...
  <build>
    ...
  </build>
  ...
  <reporting>
    <plugins>
      <plugin>
        <groupId>org.codehaus.mojo</groupId>
        <artifactId>l10n-maven-plugin</artifactId>
        <version><!-- latest release --></version>
        <configuration>
          <locales>
            <locale>de</locale>
            <locale>es</locale>
            <locale>fr</locale>
            ...
          </locales>
        </configuration>
      </plugin>
    </plugins>
  </reporting>
  ...
</project>
```

Add a `<locale>` element for every localization you have.

The report will be generated when you execute the Maven site plugin, like this:

```sh
mvn site
```

The report is written to `l10n-status.html`; a [sample](l10n-status.html) is part of this site.

Reading the Report
------------------

The report lists the resource bundles of the project, one row per base file such as `messages.properties`, with
the number of keys in the default bundle. Files whose name ends in a locale suffix, such as `messages_de.properties`,
are not listed on their own: for each configured locale, the plugin looks for `<bundle>_<locale>.properties` next to
the base file. A locale column then shows `M` (keys missing from the translation), `E` (extra keys that the
default bundle does not have) and `NT` (keys with the same value as in the default bundle, which may be
untranslated). A locale without a file shows `-`.

Choosing the Bundles
--------------------

The plugin scans the resource directories of the project. By default it reads every `*.properties` file. Use
`includes` and `excludes` to narrow this down:

```xml
<configuration>
  <locales>
    <locale>de</locale>
  </locales>
  <includes>
    <include>**/messages*.properties</include>
  </includes>
</configuration>
```

Includes configured on a `<resource>` of the project apply too. If a `<resource>` has its own excludes, they are
used and the `excludes` parameter of the plugin is ignored for that resource.

Aggregate Report
----------------

In a multi-module project, set `aggregate` to `true` in the root POM to build one report that covers the resources
of all modules. The report is only generated in the root project of the build; the modules do not get one. You can
also set it on the command line with `-Dmaven.l10n.aggregate=true`.

```xml
<configuration>
  <aggregate>true</aggregate>
  <locales>
    <locale>de</locale>
  </locales>
</configuration>
```

Requirements
------------

The plugin requires Maven 3.9.0 or later and Java 8 or later.

You can refer to the [generated documentation](./report-mojo.html) to discover
the different configuration options for the report mojo.
