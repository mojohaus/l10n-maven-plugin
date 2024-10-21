package org.codehaus.mojo.l10n;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.apache.maven.doxia.sink.Sink;
import org.apache.maven.model.Resource;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.reporting.AbstractMavenReport;
import org.apache.maven.reporting.AbstractMavenReportRenderer;
import org.apache.maven.reporting.MavenReportException;
import org.codehaus.plexus.util.DirectoryScanner;
import org.codehaus.plexus.util.StringUtils;

/**
 * A simple report for keeping track of l10n status. It lists all bundle properties
 * files and the number of properties in them. For a configurable list of locales it also
 * tracks the progress of localization.
 *
 * @author <a href="mkleint@codehaus.org">Milos Kleint</a>
 * @since 1.0.0
 */
@Mojo(name = "report")
public class L10NStatusReport extends AbstractMavenReport {

    /**
     * A list of locale strings that are to be watched for l10n status.
     *
     * @since 1.0.0
     */
    @Parameter
    private List<String> locales;

    /**
     * A list of exclude patterns to use. By default no files are excluded.
     *
     * @since 1.0.0
     */
    @Parameter
    private List<String> excludes;

    /**
     * A list of include patterns to use. By default, all <code>*.properties</code> files are included.
     *
     * @since 1.0.0
     */
    @Parameter
    private List<String> includes;

    /**
     * Whether to build an aggregated report at the root, or build individual reports.
     *
     * @since 1.0.0
     */
    @Parameter(defaultValue = "false", property = "maven.l10n.aggregate")
    protected boolean aggregate;

    private static final String[] DEFAULT_INCLUDES = {"**/*.properties"};

    private static final String[] EMPTY_STRING_ARRAY = {};

    @Override
    public boolean canGenerateReport() {
        if (aggregate && !project.isExecutionRoot()) {
            return false;
        }

        return !constructResourceDirs().isEmpty();
    }

    /**
     * Collects resource definitions from all projects in reactor.
     */
    protected Map<MavenProject, List<Resource>> constructResourceDirs() {
        Map<MavenProject, List<Resource>> sourceDirs = new HashMap<>();
        if (aggregate) {
            for (MavenProject prj : reactorProjects) {
                if (prj.getResources() != null && !prj.getResources().isEmpty()) {
                    sourceDirs.put(prj, prj.getResources());
                }
            }
        } else {
            if (project.getResources() != null && !project.getResources().isEmpty()) {
                sourceDirs.put(project, project.getResources());
            }
        }
        return sourceDirs;
    }

    /**
     * @see org.apache.maven.reporting.AbstractMavenReport#executeReport(java.util.Locale)
     */
    @Override
    protected void executeReport(Locale locale) throws MavenReportException {
        Set<Wrapper> included = new TreeSet<>(new WrapperComparator());
        Map<MavenProject, List<Resource>> res = constructResourceDirs();
        for (Map.Entry<MavenProject, List<Resource>> entry : res.entrySet()) {
            MavenProject prj = entry.getKey();
            List<Resource> lst = entry.getValue();
            for (Resource resource : lst) {
                File resourceDirectory = new File(resource.getDirectory());

                if (!resourceDirectory.exists()) {
                    getLog().info("Resource directory does not exist: " + resourceDirectory);
                    continue;
                }

                DirectoryScanner scanner = new DirectoryScanner();

                scanner.setBasedir(resource.getDirectory());
                List<String> allIncludes = new ArrayList<>();
                if (resource.getIncludes() != null && !resource.getIncludes().isEmpty()) {
                    allIncludes.addAll(resource.getIncludes());
                }
                if (includes != null && !includes.isEmpty()) {
                    allIncludes.addAll(includes);
                }

                if (allIncludes.isEmpty()) {
                    scanner.setIncludes(DEFAULT_INCLUDES);
                } else {
                    scanner.setIncludes(allIncludes.toArray(EMPTY_STRING_ARRAY));
                }

                List<String> allExcludes = new ArrayList<>();
                if (resource.getExcludes() != null && !resource.getExcludes().isEmpty()) {
                    allExcludes.addAll(resource.getExcludes());
                } else if (excludes != null && !excludes.isEmpty()) {
                    allExcludes.addAll(excludes);
                }

                scanner.setExcludes(allExcludes.toArray(EMPTY_STRING_ARRAY));

                scanner.addDefaultExcludes();
                scanner.scan();

                String[] includedFiles = scanner.getIncludedFiles();
                for (String name : includedFiles) {
                    File source = new File(resource.getDirectory(), name);
                    included.add(new Wrapper(name, source, prj));
                }
            }
        }

        // Write the overview
        L10NStatusRenderer r = new L10NStatusRenderer(getSink(), getBundle(locale), included, locale);
        r.render();
    }

    /**
     * @see org.apache.maven.reporting.MavenReport#getDescription(java.util.Locale)
     */
    @Override
    public String getDescription(Locale locale) {
        return getBundle(locale).getString("report.l10n.description");
    }

    /**
     * @see org.apache.maven.reporting.MavenReport#getName(java.util.Locale)
     */
    @Override
    public String getName(Locale locale) {
        return getBundle(locale).getString("report.l10n.name");
    }

    /**
     * @see org.apache.maven.reporting.MavenReport#getOutputName()
     */
    @Override
    public String getOutputName() {
        return "l10n-status";
    }

    private static ResourceBundle getBundle(Locale locale) {
        return ResourceBundle.getBundle("l10n-status-report", locale, L10NStatusReport.class.getClassLoader());
    }

    /**
     * Generates an overview page with a list of properties bundles
     * and a link to each locale's status.
     */
    class L10NStatusRenderer extends AbstractMavenReportRenderer {

        private final ResourceBundle bundle;

        /**
         * The locale in which the report will be rendered.
         */
        private final Locale rendererLocale;

        private final Set<Wrapper> files;

        private final Pattern localedPattern = Pattern.compile(".*_[a-zA-Z]{2}[_]?[a-zA-Z]{0,2}?\\.properties");

        public L10NStatusRenderer(Sink sink, ResourceBundle bundle, Set<Wrapper> files, Locale rendererLocale) {
            super(sink);

            this.bundle = bundle;
            this.files = files;
            this.rendererLocale = rendererLocale;
        }

        /**
         * @see org.apache.maven.reporting.MavenReportRenderer#getTitle()
         */
        public String getTitle() {
            return bundle.getString("report.l10n.title");
        }

        /**
         * @see org.apache.maven.reporting.AbstractMavenReportRenderer#renderBody()
         */
        public void renderBody() {
            startSection(getTitle());

            paragraph(bundle.getString("report.l10n.intro"));
            startSection(bundle.getString("report.l10n.summary"));

            startTable();
            tableCaption(bundle.getString("report.l10n.summary.caption"));
            String defaultLocaleColumnName = bundle.getString("report.l10n.column.default");
            String pathColumnName = bundle.getString("report.l10n.column.path");
            String missingFileLabel = bundle.getString("report.l10n.missingFile");
            String missingKeysLabel = bundle.getString("report.l10n.missingKey");
            String okLabel = bundle.getString("report.l10n.ok");
            String totalLabel = bundle.getString("report.l10n.total");
            String additionalKeysLabel = bundle.getString("report.l10n.additional");
            String nontranslatedKeysLabel = bundle.getString("report.l10n.nontranslated");
            String[] headers = new String[locales != null ? locales.size() + 2 : 2];
            Map<String, String> localeDisplayNames = new HashMap<>();
            headers[0] = pathColumnName;
            headers[1] = defaultLocaleColumnName;
            if (locales != null) {
                int ind = 2;
                for (String localeCode : locales) {
                    headers[ind] = localeCode;
                    ind = ind + 1;

                    Locale locale = createLocale(localeCode);
                    if (locale == null) {
                        // If the localeCode were in an unknown format use the localeCode itself as a fallback value
                        localeDisplayNames.put(localeCode, localeCode);
                    } else {
                        localeDisplayNames.put(localeCode, locale.getDisplayName(rendererLocale));
                    }
                }
            }
            tableHeader(headers);
            int[] count = new int[locales != null ? locales.size() + 1 : 1];
            Arrays.fill(count, 0);
            MavenProject lastPrj = null;
            Set<Wrapper> usedFiles = new TreeSet<>(new WrapperComparator());
            for (Wrapper wr : files) {
                if (reactorProjects.size() > 1 && (lastPrj == null || lastPrj != wr.getProject())) {
                    lastPrj = wr.getProject();
                    sink.tableRow();
                    String name = wr.getProject().getName();
                    if (name == null) {
                        name = wr.getProject().getGroupId() + ":"
                                + wr.getProject().getArtifactId();
                    }
                    tableCell("<b><i>" + name + "</b></i>", true);
                    sink.tableRow_();
                }
                if (wr.getFile().getName().endsWith(".properties")
                        && !localedPattern.matcher(wr.getFile().getName()).matches()) {
                    usedFiles.add(wr);
                    sink.tableRow();
                    tableCell(wr.getPath());
                    Properties props = new Properties();
                    try (BufferedInputStream in = new BufferedInputStream(
                            Files.newInputStream(wr.getFile().toPath()))) {
                        props.load(in);
                        wr.getProperties().put(Wrapper.DEFAULT_LOCALE, props);
                        tableCell("" + props.size(), true);
                        count[0] = count[0] + props.size();
                        if (locales != null) {
                            int i = 1;
                            for (String loc : locales) {
                                String nm = wr.getFile().getName();
                                String fn = nm.substring(0, nm.length() - ".properties".length());
                                File locFile = new File(wr.getFile().getParentFile(), fn + "_" + loc + ".properties");
                                if (locFile.exists()) {
                                    Properties props2 = new Properties();
                                    try (BufferedInputStream in2 =
                                            new BufferedInputStream(Files.newInputStream(locFile.toPath()))) {
                                        props2.load(in2);
                                        wr.getProperties().put(loc, props2);
                                        Set<String> missing = new HashSet<>(props.stringPropertyNames());
                                        missing.removeAll(props2.stringPropertyNames());
                                        Set<String> additional = new HashSet<>(props2.stringPropertyNames());
                                        additional.removeAll(props.stringPropertyNames());
                                        Set<String> nonTranslated = new HashSet<>();
                                        for (String k : props.stringPropertyNames()) {
                                            String val1 = props.getProperty(k);
                                            String val2 = props2.getProperty(k);
                                            if (val1.equals(val2)) {
                                                nonTranslated.add(k);
                                            }
                                        }
                                        count[i] = count[i] + (props.size() - missing.size() - nonTranslated.size());
                                        StringBuilder statusRows = new StringBuilder();
                                        if (!missing.isEmpty()) {
                                            statusRows
                                                    .append("<tr><td>")
                                                    .append(missingKeysLabel)
                                                    .append("</td><td><b>")
                                                    .append(missing.size())
                                                    .append("</b></td></tr>");
                                        } else {
                                            statusRows.append("<tr><td>&nbsp;</td><td>&nbsp;</td></tr>");
                                        }
                                        if (!additional.isEmpty()) {
                                            statusRows
                                                    .append("<tr><td>")
                                                    .append(additionalKeysLabel)
                                                    .append("</td><td><b>")
                                                    .append(additional.size())
                                                    .append("</b></td></tr>");
                                        } else {
                                            statusRows.append("<tr><td>&nbsp;</td><td>&nbsp;</td></tr>");
                                        }
                                        if (!nonTranslated.isEmpty()) {
                                            statusRows
                                                    .append("<tr><td>")
                                                    .append(nontranslatedKeysLabel)
                                                    .append("</td><td><b>")
                                                    .append(nonTranslated.size())
                                                    .append("</b></td></tr>");
                                        }
                                        tableCell(wrapInTable(okLabel, statusRows.toString()), true);
                                    }
                                } else {
                                    tableCell(missingFileLabel);
                                    count[i] += 0;
                                }
                                i = i + 1;
                            }
                        }
                    } catch (IOException ex) {
                        getLog().error(ex);
                    }
                    sink.tableRow_();
                }
            }
            sink.tableRow();
            tableCell(totalLabel);
            for (int i = 0; i < count.length; i++) {
                if (i != 0 && count[0] != 0) {
                    tableCell("<b>" + count[i] + "</b><br />(" + (count[i] * 100 / count[0]) + "&nbsp;%)", true);
                } else if (i == 0) {
                    tableCell("<b>" + count[i] + "</b>", true);
                }
            }
            sink.tableRow_();

            endTable();

            paragraph(bundle.getString("report.l10n.legend"));

            sink.list();
            sink.listItem();
            text(bundle.getString("report.l10n.list1"));
            sink.listItem_();
            sink.listItem();
            text(bundle.getString("report.l10n.list2"));
            sink.listItem_();
            sink.listItem();
            text(bundle.getString("report.l10n.list3"));
            sink.listItem_();
            sink.list_();
            sink.paragraph();
            text(bundle.getString("report.l10n.note"));
            sink.paragraph_();
            endSection();

            if (locales != null) {
                sink.list();
                for (String x : locales) {
                    sink.listItem();
                    link("#" + x, x + " - " + localeDisplayNames.get(x));
                    sink.listItem_();
                }
                sink.list_();

                for (String x : locales) {
                    startSection(x + " - " + localeDisplayNames.get(x));
                    sink.anchor(x);
                    sink.anchor_();
                    startTable();
                    tableCaption(bundle.getString("report.l10n.locale") + " " + localeDisplayNames.get(x));
                    tableHeader(new String[] {
                        bundle.getString("report.l10n.tableheader1"),
                        bundle.getString("report.l10n.tableheader2"),
                        bundle.getString("report.l10n.tableheader3"),
                        bundle.getString("report.l10n.tableheader4")
                    });

                    for (Wrapper wr : usedFiles) {
                        sink.tableRow();
                        tableCell(wr.getPath());
                        Properties defs = wr.getProperties().get(Wrapper.DEFAULT_LOCALE);
                        Properties locals = wr.getProperties().get(x);
                        if (locals == null) {
                            locals = new Properties();
                        }
                        Set<String> missing = new TreeSet<>(defs.stringPropertyNames());
                        missing.removeAll(locals.stringPropertyNames());
                        String cell = "";
                        for (String s : missing) {
                            cell = cell + "<tr><td>" + s + "</td></tr>";
                        }
                        tableCell(wrapInTable(okLabel, cell), true);
                        Set<String> additional = new TreeSet<>(locals.stringPropertyNames());
                        additional.removeAll(defs.stringPropertyNames());
                        cell = "";
                        for (String ex : additional) {
                            cell = cell + "<tr><td>" + ex + "</td></tr>";
                        }
                        tableCell(wrapInTable(okLabel, cell), true);
                        Set<String> nonTranslated = new TreeSet<>();
                        for (String k : defs.stringPropertyNames()) {
                            String val1 = defs.getProperty(k);
                            String val2 = locals.getProperty(k);
                            if (val1.equals(val2)) {
                                nonTranslated.add(k);
                            }
                        }

                        cell = "";
                        for (String n : nonTranslated) {
                            cell = cell + "<tr><td>" + n + "</td><td>\"" + defs.getProperty(n) + "\"</td></tr>";
                        }

                        tableCell(wrapInTable(okLabel, cell), true);

                        sink.tableRow_();
                    }
                    endTable();
                    endSection();
                }
            }
            endSection();
        }

        /**
         * Take the supplied locale code, split into its different parts and create a Locale object from it.
         *
         * @param localeCode The code for a locale in the format language[_country[_variant]]
         * @return A suitable Locale object, ot <code>null</code> if the code was in an unknown format
         */
        private Locale createLocale(String localeCode) {
            // Split the localeCode into language/country/variant
            String[] localeComponents = StringUtils.split(localeCode, "_");
            Locale locale = null;
            if (localeComponents.length == 1) {
                locale = new Locale(localeComponents[0]);
            } else if (localeComponents.length == 2) {
                locale = new Locale(localeComponents[0], localeComponents[1]);
            } else if (localeComponents.length == 3) {
                locale = new Locale(localeComponents[0], localeComponents[1], localeComponents[2]);
            }
            return locale;
        }

        private String wrapInTable(String okLabel, String cell) {
            if (cell.isEmpty()) {
                cell = okLabel;
            } else {
                cell = "<table><tbody>" + cell + "</tbody></table>";
            }
            return cell;
        }
    }

    static class Wrapper {

        private final String path;

        private final File file;

        private final MavenProject proj;

        private final Map<String, Properties> properties;

        static final String DEFAULT_LOCALE = "Default";

        public Wrapper(String p, File f, MavenProject prj) {
            path = p;
            file = f;
            proj = prj;
            properties = new HashMap<>();
        }

        public File getFile() {
            return file;
        }

        public String getPath() {
            return path;
        }

        public MavenProject getProject() {
            return proj;
        }

        public Map<String, Properties> getProperties() {
            return properties;
        }
    }

    private static class WrapperComparator implements Comparator<Wrapper> {

        public int compare(Wrapper wr1, Wrapper wr2) {
            int comp1 = wr1.getProject().getBasedir().compareTo(wr2.getProject().getBasedir());
            if (comp1 != 0) {
                return comp1;
            }
            return wr1.getFile().compareTo(wr2.getFile());
        }
    }
}
