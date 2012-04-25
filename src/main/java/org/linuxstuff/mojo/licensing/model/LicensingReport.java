package org.linuxstuff.mojo.licensing.model;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.maven.plugin.MojoExecutionException;
import org.linuxstuff.mojo.licensing.FileUtil;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.io.xml.StaxDriver;

@XStreamAlias("licensing")
public class LicensingReport {

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

		try {
			FileUtil.createNewFile(file);

			FileOutputStream fos = new FileOutputStream(file);

			xstream.toXML(this, fos);

			fos.close();
		} catch (IOException e) {
			throw new MojoExecutionException("Failure while creating new file " + file, e);
		}
	}
	
	public void writeTextReport(File file) throws MojoExecutionException {

	    SortedMap<String,SortedSet<String>> artifactsPerLicense = new TreeMap<String,SortedSet<String>>();
        //SortedMap<String,SortedSet<String>> multiLicensed = new TreeMap<String,SortedSet<String>>();
	    for (ArtifactWithLicenses awl : getLicensedArtifacts()) {
	        String artifactName = awl.getName();
	        Set<String> licenses = awl.getLicenses();
	        for (String license : licenses) {
	            SortedSet<String> artifacts = artifactsPerLicense.get( license );
	            if (artifacts == null) {
	                artifacts = new TreeSet<String>();
	                artifactsPerLicense.put( license, artifacts );
	            }
	            artifacts.add( artifactName );
	        }
        }
	    for ( Entry<String,SortedSet<String>> entry : artifactsPerLicense.entrySet()) {
	        System.out.println("License: " + entry.getKey());
	        for (String artifactName : entry.getValue()) {
	            System.out.println(artifactName);
	        }
	    }
	}

}
