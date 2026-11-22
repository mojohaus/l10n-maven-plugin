# Agent guide for l10n-maven-plugin

This file is for automated agents and human contributors working on this repository. It records
what the code and the POMs do not make obvious.

## Layout

This is a single-module Maven plugin with one goal, `report` (`L10NStatusReport`, a Maven report).
It lists the resource bundles of a project and, for each configured locale, how many keys each
bundle has. The `aggregate` parameter builds one report at the root of a multi-module project.

- The report's own texts are in `src/main/resources/l10n-status-report*.properties` (default,
  `en`, `de`).
- The site is built from `src/site/markdown`.

## Build gates

- `spotless-maven-plugin` from mojo-parent runs during `process-sources`. Run `mvn spotless:apply`
  before committing. It formats Java with palantir-java-format, sorts the POM, and also reformats
  Markdown, including this file and the README.
- The build compiles with `mojo.java.target` 8.
- The plugin compiles against `mavenVersion` and declares a literal Maven 3.9.0 as its
  `<prerequisites>`; `minimalMavenBuildVersion` holds the build to the same floor. Raising
  `mavenVersion` must not raise the prerequisite.
- Fields of `L10NStatusReport` are plugin parameters; renaming one changes the plugin's
  configuration API.

## Tests

- There are no unit tests: `src/test` does not exist and Surefire reports "No tests to run".
  The integration tests are the only tests.

## Integration tests

- They live in `src/it`: `report` and `report-aggregate` (a two-module project). Run them with the
  `run-its` profile; the invoker execution installs the plugin into `target/local-repo` itself, so
  no separate `mvn install` is needed. Run one with:

  ```shell
  mvn -B -P run-its verify -Dinvoker.test=report
  ```

  There are no `setup-*` projects.

- The invoker goal is `site`, and the projects configure the plugin under `<reporting>`.
  The post-build hook is `verify.groovy`. Write it as `assert` statements. Invoker fails a script
  whose last expression is anything other than `true` or `null`, so a trailing `list.each { }`
  fails the test; use a `for` loop.

- The invoker configuration does not set parallel threads or `JAVA_TOOL_OPTIONS`; the projects
  run one after another.

## Continuous integration

- `.github/workflows/maven.yml` calls the MojoHaus `maven-verify.yml` reusable workflow in
  `mojohaus/.github`, which sets the JDK and OS matrix and runs Maven 3 and Maven 4. Its default
  build goal activates `run-its`, so the integration tests run in CI; run them locally as above.
- Branches pushed to this repository build under the `push` event.

## Dependencies

- Dependabot ignores four named artifacts, not all of `org.apache.maven:*`: `maven-core`,
  `maven-model` and `maven-plugin-api` stay below 3.10 and `maven-reporting-api` below 4.1, so
  Maven core stays on `mavenVersion` and the site plugin keeps reporting-api 4.0.0.
- `dependencyManagement` overrides `commons-io` and `plexus-archiver` for security fixes in
  transitive dependencies.

## Documentation updates

A change that users or contributors can notice ships with the documentation it affects, in the same pull
request. A refactor with no visible effect needs none.

- A new or changed parameter or default of the report: the Javadoc of the field in `L10NStatusReport`, which
  becomes the goal page, and the matching section of `src/site/markdown/index.md` (Reading the Report, Choosing
  the Bundles, Aggregate Report).
- The page is plain Markdown. Its first plugin declaration carries the version as
  `<version><!-- latest release --></version>`, so no example goes stale.
- A page added, removed or renamed: the menu in `src/site/site.xml`. Never rename a published page; its URL is
  linked from elsewhere.
- The Maven or Java requirement, or how to build and release: `README.md`, and the Requirements section of
  `src/site/markdown/index.md`.
- A new convention, build gate or test fixture: this file.

Check the result with `mvn site` and open `target/site/index.html`.

## Pull requests

- Squash and rebase merges are enabled; merge commits are not. Release Drafter, through the shared
  release-drafter workflow, builds the release notes from pull request titles
  and labels, so the title should describe the change for a user.

