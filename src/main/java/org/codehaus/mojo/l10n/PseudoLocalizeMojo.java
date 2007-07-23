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
import java.io.BufferedOutputStream;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.util.DirectoryScanner;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringUtils;

/**
 * Allows to do an automated pseudo localization to test the completeness of your project internationalization efford.
 * This technique simulates the process of localizing products by prefixing and suffixing all your I18N-ed messages.
 * <p/>
 * For more information on pseudolocalization, 
 * see <a href="http://developers.sun.com/solaris/articles/i18n/I18N_Testing.html">I18N Testing Guidelines and Techniques</a>
 * <p/>
 * For more general information on localization, 
 * see <a href="http://java.sun.com/developer/technicalArticles/Intl/ResourceBundles/">Java Internationalization: Localization with ResourceBundles</a>
 * @author <a href="mailto:mkleint@codehaus.org">Milos Kleint</a>
 * @goal pseudo
 * @phase process-classes
 */
public class PseudoLocalizeMojo
    extends AbstractMojo
{
        
    /**
     * The output directory into which to copy the resources.
     *
     * @parameter expression="${project.build.outputDirectory}"
     * @required
     */
    private String outputDirectory;

    /**
     * The input directory into which to copy the resources.
     * The plugin scans the build output directory by default, in order to have
     * the complete set of resources that end up in the product.
     *
     * @parameter expression="${project.build.outputDirectory}"
     * @required
     */
    private String inputDirectory;

    /**
     * The list of resources we want to pseudo localize. If not specified,
     * the default pattern is "**\/*.properties"
     *
     * @parameter
     */
    private List includes;

    /**
     * The list of resources we don't want to pseudo localize.
     *
     * @parameter
     */
    private List excludes;

    private static final String[] DEFAULT_INCLUDES = {"**/*.properties"};

    private static final String[] EMPTY_STRING_ARRAY = {};

    /**
     * Pattern for replacement of localized string values.
     * The plugins iterates all properties in the property files and replaces the 
     * values using {@link java.text.MessageFormat} with this value as formatting pattern. The 
     * Pattern is expected to contain this sequence {0} exactly once with a prefix 
     * and/or suffix. 
     * 
     * @parameter default-value="XXX 什么 {0} YYY"
     * @required
     */ 
    private String pseudoLocPattern;
    
    /**
     * locale name that is used  for pseudo localization.
     * The resulting property files will have the following name:
     * &lt;filename&gt;_&lt;pseudoLocale&gt;.properties
     * @parameter default-value="xx"
     * @required
     */ 
     private String pseudoLocale;

    public void execute()
        throws MojoExecutionException
    {
        if (pseudoLocPattern.indexOf("{0}") == -1) {
            throw new MojoExecutionException("The pseudoLocPattern parameter with value '" + pseudoLocPattern + "' is misconfigured.");
        }
        generatePseudoLoc(outputDirectory );
    }

    protected void generatePseudoLoc( String outputDirectory )
        throws MojoExecutionException
    {
            File resourceDirectory = new File( inputDirectory );

            if ( !resourceDirectory.exists() )
            {
                getLog().info( "Resource directory does not exist: " + resourceDirectory );
                return;
            }

            // this part is required in case the user specified "../something" as destination
            // see MNG-1345
            File outputDir = new File( outputDirectory );
            if ( !outputDir.exists() )
            {
                if ( !outputDir.mkdirs() )
                {
                    throw new MojoExecutionException( "Cannot create resource output directory: " + outputDir );
                }
            }

            DirectoryScanner scanner = new DirectoryScanner();

            scanner.setBasedir( resourceDirectory );
            if ( includes != null && !includes.isEmpty() )
            {
                scanner.setIncludes( (String[]) includes.toArray( EMPTY_STRING_ARRAY ) );
            }
            else
            {
                scanner.setIncludes( DEFAULT_INCLUDES );
            }

            if ( excludes != null && !excludes.isEmpty() )
            {
                scanner.setExcludes( (String[]) excludes.toArray( EMPTY_STRING_ARRAY ) );
            }

            scanner.addDefaultExcludes();
            scanner.scan();

            List includedFiles = Arrays.asList( scanner.getIncludedFiles() );

            for ( Iterator j = includedFiles.iterator(); j.hasNext(); )
            {
                String name = (String) j.next();

                File source = new File( inputDirectory, name );
                File dest = new File( outputDirectory, name );
                
                String fileName = "";
                String[] split = StringUtils.split(source.getName(), ".");
                for (int i = 0; i < split.length - 1; i++) {
                    if (i == split.length - 2) {
                        fileName = fileName + split[i] + "_" + pseudoLocale + ".";
                    } else {
                        fileName = fileName + split[i] + ".";
                    }
                }
                fileName = fileName + split[split.length - 1];
                File destinationFile = new File( dest.getParentFile(), fileName );
                
                getLog().info("Pseudo-localizing " + name + " bundle file.");

                try
                {
                    copyFile( source, destinationFile);
                }
                catch ( IOException e )
                {
                    throw new MojoExecutionException( "Error copying resource " + source, e );
                }
            }
    }

    private void copyFile( File from, final File to)
        throws IOException
    {
        Properties props = new Properties();
        BufferedInputStream in = null;
        BufferedOutputStream out = null;
        to.getParentFile().mkdirs();
        try {
            in = new BufferedInputStream(new FileInputStream(from));
            props.load(in);
            Iterator it = props.keySet().iterator();
            while (it.hasNext()) {
                String key = (String)it.next();
                String val = props.getProperty(key);
                String newVal = MessageFormat.format(pseudoLocPattern, new String[] {val});
                props.setProperty(key, newVal);
            }
            out = new BufferedOutputStream(new FileOutputStream(to));
            props.store(out,"Pseudo Localized bundle file for I18N testing autogenerated by the l10n-maven-plugin.");
        } finally {
            IOUtil.close(in);
            IOUtil.close(out);
        }
    }
}
