package org.codehaus.mojo.l10n;

/*
 * Copyright 2007 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import org.apache.maven.doxia.sink.Sink;
import org.apache.maven.doxia.siterenderer.Renderer;
import org.apache.maven.project.MavenProject;
import org.apache.maven.reporting.AbstractMavenReport;
import org.apache.maven.reporting.AbstractMavenReportRenderer;
import org.apache.maven.reporting.MavenReportException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import org.apache.maven.model.Resource;
import org.codehaus.plexus.util.DirectoryScanner;
import org.codehaus.plexus.util.IOUtil;

/**
 * a simple report for keeping track for l10n status. It lists all bunlde properties
 * files, the number of properties in them. For configurable list of locales it also
 * tracks the progress of localization.
 * @author <a href="mkleint@codehaus.org">Milos Kleint</a>
 * @goal report
 */
public class L10NStatusReport extends AbstractMavenReport {

    /**
     * Report output directory.
     *
     * @parameter expression="${project.build.directory}/generated-site/xdoc"
     * @required
     */
    private String outputDirectory;

    /**
     * Doxia Site Renderer.
     *
     * @component
     */
    private Renderer siteRenderer;

    /**
     * a list locales strings that are to be watched for l10n status.
     * @parameter
     */
    private List locales;

    /**
     * The Maven Project.
     *
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    /**
     * The list of resources that are scanned for properties bundles.
     *
     * @parameter expression="${project.resources}"
     * @required
     */
    private List resources;
    
//TDB    
//    /**
//     * A list of exclude patterns to use. By default no files are excluded.
//     *
//     * @parameter expression="${maven.l10n.excludes}"
//     */
//    private ArrayList excludes;
//
//    /**
//     * A list of include patterns to use. By default all .java files are included.
//     *
//     * @parameter expression="${maven.l10n.includes}"
//     */
//    private ArrayList includes;

    /**
     * The projects in the reactor for aggregation report.
     *
     * @parameter expression="${reactorProjects}"
     * @readonly
     */
    protected List reactorProjects;

    /**
     * Whether to build an aggregated report at the root, or build individual reports.
     *
     * @parameter expression="${maven.l10n.aggregate}" default-value="false"
     */
    protected boolean aggregate;
    

    private static final String[] DEFAULT_INCLUDES = {"**/*.properties"};

    private static final String[] EMPTY_STRING_ARRAY = {};


    /**
     * @see org.apache.maven.reporting.AbstractMavenReport#getSiteRenderer()
     */
    protected Renderer getSiteRenderer() {
        return siteRenderer;
    }

    /**
     * @see org.apache.maven.reporting.AbstractMavenReport#getOutputDirectory()
     */
    protected String getOutputDirectory() {
        return outputDirectory;
    }

    /**
     * @see org.apache.maven.reporting.AbstractMavenReport#getProject()
     */
    protected MavenProject getProject() {
        return project;
    }
    
    public boolean canGenerateReport() {
        return canGenerateReport( constructResourceDirs() );
    }
    
    /**
     * @param sourceDirs
     * @return true if the report could be generated
     */
    protected boolean canGenerateReport( Map sourceDirs )
    {
        boolean canGenerate = !sourceDirs.isEmpty();

        if ( aggregate && !project.isExecutionRoot() )
        {
            canGenerate = false;
        }
        return canGenerate;
    }
    
    /**
     * collects resource definitions from all projects in reactor..
     * @return 
     */
    protected Map constructResourceDirs()
    {
        Map sourceDirs = new HashMap();
        if ( aggregate )
        {
            for ( Iterator i = reactorProjects.iterator(); i.hasNext(); )
            {
                MavenProject prj = (MavenProject) i.next();
                if (prj.getResources() != null && !prj.getResources().isEmpty()) {
                    sourceDirs.put(prj, new ArrayList(prj.getResources()));
                }

            }
        } else {
            if (resources != null && !resources.isEmpty()) {
                sourceDirs.put(project, new ArrayList(resources));
            }
        }
        return sourceDirs;
    }
    
    

    /**
     * @see org.apache.maven.reporting.AbstractMavenReport#executeReport(java.util.Locale)
     */
    protected void executeReport(Locale locale) throws MavenReportException {
        Set included = new TreeSet(new WrapperComparator());
        Map res = constructResourceDirs();
        for (Iterator it = res.keySet().iterator(); it.hasNext();) {
            MavenProject prj = (MavenProject)it.next();
            List lst = (List)res.get(prj);
            for (Iterator i = lst.iterator(); i.hasNext();) {
                Resource resource = (Resource) i.next();

                File resourceDirectory = new File(resource.getDirectory());

                if (!resourceDirectory.exists()) {
                    getLog().info("Resource directory does not exist: " + resourceDirectory);
                    continue;
                }

                DirectoryScanner scanner = new DirectoryScanner();

                scanner.setBasedir(resource.getDirectory());
                if (resource.getIncludes() != null && !resource.getIncludes().isEmpty()) {
                    scanner.setIncludes((String[]) resource.getIncludes().toArray( EMPTY_STRING_ARRAY ));
                } else {
                    scanner.setIncludes(DEFAULT_INCLUDES);
                }

                if (resource.getExcludes() != null && !resource.getExcludes().isEmpty()) {
                    scanner.setExcludes((String[]) resource.getExcludes().toArray( EMPTY_STRING_ARRAY ));
                }

                scanner.addDefaultExcludes();
                scanner.scan();

                List includedFiles = Arrays.asList(scanner.getIncludedFiles());
                for (Iterator j = includedFiles.iterator(); j.hasNext();) {
                    String name = (String) j.next();
                    File source = new File(resource.getDirectory(), name);
                    included.add(new Wrapper(name, source, prj));
                }
            }
        }

        // Write the overview
        L10NStatusRenderer r = new L10NStatusRenderer(getSink(), locale, included);
        r.render();
    }

    /**
     * @see org.apache.maven.reporting.MavenReport#getDescription(java.util.Locale)
     */
    public String getDescription(Locale locale) {
        return getBundle(locale).getString("report.l10n.description");
    }

    /**
     * @see org.apache.maven.reporting.MavenReport#getName(java.util.Locale)
     */
    public String getName(Locale locale) {
        return getBundle(locale).getString("report.l10n.name");
    }

    /**
     * @see org.apache.maven.reporting.MavenReport#getOutputName()
     */
    public String getOutputName() {
        return "l10n-status";
    }

    private static ResourceBundle getBundle(Locale locale) {
        return ResourceBundle.getBundle("l10n-status-report", locale, L10NStatusReport.class.getClassLoader());
    }

/**
     * Generates an overview page with the list of goals
     * and a link to the goal's page.
     */
    class L10NStatusRenderer extends AbstractMavenReportRenderer {

        private final Locale locale;
        private Set files;
        private Pattern localed_patt = Pattern.compile(".*_[a-zA-Z]{2}[_]?[a-zA-Z]{0,2}?\\.properties");

        public L10NStatusRenderer(Sink sink, Locale locale, Set files) {
            super(sink);

            this.locale = locale;
            this.files = files;
        }

        /**
         * @see org.apache.maven.reporting.MavenReportRenderer#getTitle()
         */
        public String getTitle() {
            return getBundle(locale).getString("report.l10n.title");
        }

        /**
         * @see org.apache.maven.reporting.AbstractMavenReportRenderer#renderBody()
         */
        public void renderBody() {
            startSection(getTitle());

            paragraph(getBundle(locale).getString("report.l10n.intro"));
            
            startTable();
            tableCaption(getBundle(locale).getString("report.l10n.summary.caption"));
            String defaultLocaleColumnName = getBundle(locale).getString("report.l10n.column.default");
            String pathColumnName = getBundle(locale).getString("report.l10n.column.path");
            String missingFileLabel = getBundle(locale).getString("report.l10n.missingFile");
            String missingKeysLabel = getBundle(locale).getString("report.l10n.missingKey");
            String okLabel = getBundle(locale).getString("report.l10n.ok");
            String totalLabel = getBundle(locale).getString("report.l10n.total");
            String additionalKeysLabel = getBundle(locale).getString("report.l10n.additional");
            String nontranslatedKeysLabel = getBundle(locale).getString("report.l10n.nontranslated");
            String[] headers = new String[locales != null ? locales.size() + 2 : 2];
            headers[0] = pathColumnName;
            headers[1] = defaultLocaleColumnName;
            if (locales != null) {
                Iterator it = locales.iterator();
                int ind = 2;
                while (it.hasNext()) {
                    headers[ind] = (String)it.next();
                    ind = ind + 1;
                }
            }
            tableHeader(headers);
            int[] count = new int[locales != null ? locales.size() + 1 : 1];
            Arrays.fill(count,0);
            Iterator it = files.iterator();
            MavenProject lastPrj = null;
            while (it.hasNext()) {
                Wrapper wr = (Wrapper) it.next();
                if (reactorProjects.size() > 1 && (lastPrj == null || lastPrj != wr.getProject())) {
                    lastPrj = wr.getProject();
                    sink.tableRow();
                    String name = wr.getProject().getName();
                    if (name == null) {
                        name = wr.getProject().getGroupId() + ":" + wr.getProject().getArtifactId();
                    }
                    tableCell("<b><i>" + name + "</b></i>", true);
                    sink.tableRow_();
                }
                if (wr.getFile().getName().endsWith(".properties") && !localed_patt.matcher(wr.getFile().getName()).matches()) {
                    sink.tableRow();
                    tableCell(wr.getPath());
                    Properties props = new Properties();
                    BufferedInputStream in = null;
                    try {
                        in = new BufferedInputStream(new FileInputStream(wr.getFile()));
                        props.load(in);
                        tableCell("" + props.size(), true);
                        count[0] = count[0] + props.size();
                        if (locales != null) {
                            Iterator it2 = locales.iterator();
                            int i = 1;
                            while (it2.hasNext()) {
                                String loc = (String)it2.next();
                                String nm = wr.getFile().getName();
                                String fn = nm.substring(0, nm.length() - ".properties".length());
                                File locFile = new File(wr.getFile().getParentFile(), fn + "_" + loc + ".properties");
                                if (locFile.exists()) {
                                    BufferedInputStream in2 = null;
                                    Properties props2 = new Properties();
                                    try {
                                        in2 = new BufferedInputStream(new FileInputStream(locFile));
                                        props2.load(in2);
                                        HashSet missing = new HashSet(props.keySet());
                                        missing.removeAll(props2.keySet());
                                        HashSet additional = new HashSet(props2.keySet());
                                        additional.removeAll(props.keySet());
                                        HashSet nonTranslated = new HashSet();
                                        Iterator itx = props.keySet().iterator();
                                        while (itx.hasNext()) {
                                            String k = (String)itx.next();
                                            String val1 = props.getProperty(k);
                                            String val2 = props2.getProperty(k);
                                            if (val2 != null && val1.equals(val2)) {
                                                nonTranslated.add(k);
                                            }
                                        }
                                        count[i] = count[i] + (props.size() - missing.size() - nonTranslated.size());
                                        String cell = "";
                                        if (missing.size() != 0) {
                                            cell = "<tr><td>" + missingKeysLabel + "</td><td><b>" + missing.size() + "</b></td></tr>";
                                        }
                                        if (additional.size() != 0) {
                                            cell = cell + "<tr><td>" +additionalKeysLabel + "</td><td><b>" + additional.size() + "</b></td></tr>";
                                        }
                                        if (nonTranslated.size() != 0) {
                                            cell = cell + "<tr><td>" +nontranslatedKeysLabel + "</td><td><b>" + nonTranslated.size() + "</b></td></tr>";
                                        }
                                        if (cell.length() == 0) {
                                            cell = okLabel;
                                        } else {
                                            cell = "<table><tbody>" + cell + "</tbody></table>";
                                        }
                                        tableCell(cell, true);
                                    } finally {
                                        IOUtil.close(in2);
                                    }
                                } else {
                                    tableCell(missingFileLabel);
                                    count[i] = count[i] + 0;
                                }
                                i = i + 1;
                            }
                        }
                    } catch (IOException ex) {
                        getLog().error(ex);
                    } finally {
                        IOUtil.close(in);
                    }
                    sink.tableRow_();
                }
            }
            sink.tableRow();
            tableCell(totalLabel);
            for (int i = 0; i < count.length; i++) {
                if (i != 0 && count[0] != 0) {
                    tableCell("<b>" + count[i] + "</b> (" + (count[i] * 100 / count[0]) + " %)", true);
                } else if (i == 0) {
                    tableCell("<b>" + count[i] + "</b>", true);
                }
            }
            sink.tableRow_();
            
            endTable();
            
            getSink().paragraph();
            text(getBundle(locale).getString("report.l10n.legend"));
            getSink().paragraph_();
            getSink().list();
            getSink().listItem();
            text(getBundle(locale).getString("report.l10n.list1"));
            getSink().listItem_();
            getSink().listItem();
            text(getBundle(locale).getString("report.l10n.list2"));
            getSink().listItem_();
            getSink().listItem();
            text(getBundle(locale).getString("report.l10n.list3"));
            getSink().listItem_();
            getSink().list_();
            getSink().paragraph();
            text(getBundle(locale).getString("report.l10n.note"));
            getSink().paragraph_();

            endSection();
        }
    }

    private static class Wrapper {

        private String path;
        private File file;
        private MavenProject proj;

        public Wrapper(String p, File f, MavenProject prj) {
            path = p;
            file = f;
            proj = prj;
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

    }
    
    private static class WrapperComparator implements Comparator {

        public int compare(Object o1, Object o2) {
            Wrapper wr1 = (Wrapper)o1;
            Wrapper wr2 = (Wrapper)o2;
            int comp1 = wr1.getProject().getBasedir().compareTo(wr2.getProject().getBasedir());
            if (comp1 != 0) {
                return comp1;
            }
            return wr1.getFile().compareTo(wr2.getFile());
        }
        
    }
}
