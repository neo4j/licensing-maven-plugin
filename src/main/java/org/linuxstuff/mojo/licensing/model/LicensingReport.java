package org.linuxstuff.mojo.licensing.model;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.resource.ResourceManager;
import org.codehaus.plexus.resource.loader.FileResourceCreationException;
import org.codehaus.plexus.resource.loader.ResourceNotFoundException;
import org.codehaus.plexus.util.FileUtils;
import org.linuxstuff.mojo.licensing.FileUtil;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.io.xml.StaxDriver;

@XStreamAlias("licensing")
public class LicensingReport {

	private static final String LINE = "------------------------------------------------------------------------------";

    private static final String FILE_ENCODING = "UTF-8";

    @XStreamAlias("disliked-licenses")
	@XStreamAsAttribute
	long dislikedArtifactsCount;

	@XStreamAlias("missing-licenses")
	@XStreamAsAttribute
	long missingLicensesCount;

	@XStreamAlias("licensing-check")
	@XStreamAsAttribute
	boolean passing = true;

	@XStreamAlias("artifacts")
	private Set<ArtifactWithLicenses> licensedArtifacts = new HashSet<ArtifactWithLicenses>();

	@XStreamAlias("license-missing")
	private Set<ArtifactWithLicenses> licenseMissing = new HashSet<ArtifactWithLicenses>();

	@XStreamAlias("disliked-artifacts")
	private Set<ArtifactWithLicenses> dislikedArtifacts = new HashSet<ArtifactWithLicenses>();

	public LicensingReport() {
	}

	private void updatePassing() {
		passing = (licenseMissing.isEmpty() && dislikedArtifacts.isEmpty());
	}

	public void addLicensedArtifact(ArtifactWithLicenses artifact) {	    
		licensedArtifacts.add(artifact);
	}

	public void addMissingLicense(ArtifactWithLicenses artifact) {
		licenseMissing.add(artifact);
		missingLicensesCount = licenseMissing.size();
		updatePassing();
	}

	public void addDislikedArtifact(ArtifactWithLicenses artifact) {
		dislikedArtifacts.add(artifact);
		dislikedArtifactsCount = dislikedArtifacts.size();
		updatePassing();
	}

	public Set<ArtifactWithLicenses> getLicensedArtifacts() {
		return licensedArtifacts;
	}

	public Set<ArtifactWithLicenses> getLicenseMissing() {
		return licenseMissing;
	}

	public Set<ArtifactWithLicenses> getDislikedArtifacts() {
		return dislikedArtifacts;
	}

	/**
	 * Merges the passed in {@code LicensingReport} into this one, making this
	 * one a combination of the two.
	 */
	public void combineWith(LicensingReport artifactReport) {
		for (ArtifactWithLicenses artifact : artifactReport.getDislikedArtifacts()) {
			addDislikedArtifact(artifact);
		}

		for (ArtifactWithLicenses artifact : artifactReport.getLicensedArtifacts()) {
			addLicensedArtifact(artifact);
		}

		for (ArtifactWithLicenses artifact : artifactReport.getLicenseMissing()) {
			addMissingLicense(artifact);
		}

	}

	public void writeReport(File file) throws MojoExecutionException {

		XStream xstream = new XStream(new StaxDriver());

		xstream.processAnnotations(LicensingReport.class);
		xstream.processAnnotations(ArtifactWithLicenses.class);

		FileOutputStream fos = null;
		try {
			FileUtil.createNewFile(file);

			fos = new FileOutputStream(file);

			xstream.toXML(this, fos);
		} catch (IOException e) {
			throw new MojoExecutionException("Failure while creating new file " + file, e);
        } finally {
            if (fos != null) {
                try
                {
                    fos.close();
                }
                catch ( IOException e )
                {
                    throw new MojoExecutionException("Error while closing file " + file, e);
                }            
            }
        }
	}

    public void writeTextReport( File file, ResourceManager locator,
            String prependText, String appendText, boolean includeDualList,
            boolean includeFullLicense ) throws MojoExecutionException
    {
	    PrintWriter writer = null;
        String lineSep = System.getProperty( "line.separator" );
        System.setProperty( "line.separator", "\r\n" );        
        try {
            File prefixResource = getTextResourceFile(locator, prependText);
            File postfixResource = getTextResourceFile(locator, appendText);
            FileUtil.createNewFile(file);
            
            writer = new PrintWriter( file, FILE_ENCODING );
            
            writeToFile( prefixResource, writer );
            generateTextReport(writer, locator, includeDualList, includeFullLicense);
            writeToFile( postfixResource, writer );
        } catch (IOException e) {
            throw new MojoExecutionException("Failure while creating new file " + file, e);
        } finally {
            if (writer != null) {
                writer.close();            
                System.setProperty( "line.separator", lineSep );
            }
        }
	}

    private File getTextResourceFile( ResourceManager locator, String fileName ) throws MojoExecutionException
    {
        if (fileName == null || "".equals(fileName)) {
            return null;
        }
        File textResource;
        try
        {
            textResource = locator.getResourceAsFile(fileName);
        }
        catch ( ResourceNotFoundException e )
        {
            throw new MojoExecutionException("File not found" , e );
        }
        catch ( FileResourceCreationException e )
        {
            throw new MojoExecutionException("Could not create file resource." , e );
        }
        return textResource;
    }

    private void writeToFile( File fromFile, PrintWriter writer )
            throws IOException
    {
        if (fromFile  != null)
        {
            String[] lines = FileUtils.fileRead( fromFile, FILE_ENCODING ).split( "\n" );
            for (String line : lines) {
                writer.println( line );
            }
            writer.println();
        }
    }

    private void generateTextReport( PrintWriter writer,
            ResourceManager locator, boolean includeDualList, boolean includeFullLicense )
            throws IOException, MojoExecutionException
    {
        SortedMap<String,SortedSet<String>> artifactsPerLicense = new TreeMap<String,SortedSet<String>>();
        SortedMap<String,SortedSet<String>> multiLicensed = new TreeMap<String,SortedSet<String>>();
	    for (ArtifactWithLicenses awl : getLicensedArtifacts()) {
	        String artifactName = awl.getName();
	        Set<String> licenses = awl.getLicenses();
	        for (String license : licenses) {
	            SortedSet<String> artifacts = artifactsPerLicense.get( license );
	            if (artifacts == null) {
	                artifacts = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
	                artifactsPerLicense.put( license, artifacts );
	            }
	            artifacts.add( artifactName );
	            if (licenses.size() > 1) {
	                SortedSet<String> artifactLicenses = multiLicensed.get( artifactName );
	                if (artifactLicenses==null) {
	                    artifactLicenses = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
	                    multiLicensed.put( artifactName, artifactLicenses );
	                }
	                artifactLicenses.add( license );	                
	            }
	        }
        }
	    if (!includeFullLicense)
	    {
	        writer.println( "Third-party licenses" );
            writer.println( "--------------------" );
	    }
	    for ( Entry<String,SortedSet<String>> entry : artifactsPerLicense.entrySet()) {
	        if (includeFullLicense)
	        {
                writer.println( LINE );
	        }
	        else
	        {
	            writer.println();
	        }
	        writer.println(entry.getKey());
	        for (String artifactName : entry.getValue()) {
	            writer.println("  " + artifactName);
	        }
	        if (includeFullLicense)
	        {
	            writer.println( LINE + "\n" );
	            writeToFile(getTextResourceFile( locator, entry.getKey() ), writer);
	            writer.println();
	        }
	    }
        writer.println();
	    if (multiLicensed.size() > 0) {
	        writer.println("Dependencies with multiple licenses");
            writer.println("-----------------------------------");
            for ( Entry<String,SortedSet<String>> entry : multiLicensed.entrySet()) {
                writer.println();
                writer.println(entry.getKey());
                for (String licenseName : entry.getValue()) {
                    writer.println("  " + licenseName);
                }
            }
            writer.println();
	    }
    }
}
