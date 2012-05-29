package org.linuxstuff.mojo.licensing;

import java.io.File;
import java.util.Collection;
import java.util.Set;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.resource.loader.FileResourceCreationException;
import org.codehaus.plexus.resource.loader.ResourceNotFoundException;
import org.linuxstuff.mojo.licensing.model.ArtifactWithLicenses;
import org.linuxstuff.mojo.licensing.model.LicensingReport;

/**
 * Determine licensing information of all dependencies. This is generally
 * obtained by dependencies providing a license block in their POM. However this
 * plugin supports a requirements file which can supplement licensing
 * information for artifacts missing licensing information.
 * 
 * @goal check
 * @phase verify
 * @threadSafe
 * @requiresDependencyResolution test
 * @requiresProject true
 * @since 1.0
 */
public class CheckMojo extends AbstractLicensingMojo {

	/**
	 * A fail the build if any artifacts are missing licensing information.
	 * 
	 * @parameter expression="${failIfMissing}" default-value="true"
	 * @since 1.0
	 */
	protected boolean failIfMissing;

	/**
	 * A fail the build if any artifacts have disliked licenses.
	 * 
	 * @parameter expression="${failIfDisliked}" default-value="true"
	 * @since 1.0
	 */
	protected boolean failIfDisliked;

	/**
	 * If using liked licenses, only use those in the report.
	 * 
	 * @parameter expression="${includeOnlyLikedInReport}" default-value="true"
	 * @since 1.0
	 */
	protected boolean includeOnlyLikedInReport;

    /**
     * Output the result as a plain text file.
     * 
     * @parameter expression="${writeTextReport}" default-value="false"
     * @since 1.0
     */
    protected boolean plainTextReport;

    /**
     * File to prepend to the text-based report.
     * 
     * @parameter expression="${prependText}"
     * @since 1.0
     */
    protected String prependText;

    /**
     * File to append to the text-based report.
     * 
     * @parameter expression="${appendText}"
     * @since 1.0
     */
    protected String appendText;

    /**
	 * Fail the build if any dependencies are either under disliked licenses or
	 * are missing licensing information.
	 */
	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {

		if (skip) {
			getLog().debug("licensing.skip=true, not doing anything.");
			return;
		}

		readLicensingRequirements();

		LicensingReport report = generateReport(project);

		File file = new File(project.getBuild().getDirectory(), thirdPartyLicensingFilename);

		if (plainTextReport) {
		    File prefixResource = getTextResourceFile(prependText);
            File postfixResource = getTextResourceFile(appendText);
	        report.writeTextReport(file, prefixResource, postfixResource);
		} else {
		    report.writeReport(file);
		}
		
		checkForFailure(report);

	}

    private File getTextResourceFile( String fileName ) throws MojoExecutionException
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

	protected LicensingReport generateReport(MavenProject project) {

		LicensingReport aReport = new LicensingReport();

		Collection<MavenProject> projects = getProjectDependencies(project);
		for (MavenProject mavenProject : projects) {

			ArtifactWithLicenses entry = new ArtifactWithLicenses();

			entry.setArtifactId(mavenProject.getId());
			entry.setName(mavenProject.getName());

			Set<String> licenses = collectLicensesForMavenProject(mavenProject);

			if (licenses.isEmpty()) {
				getLog().warn("Licensing: The artifact " + mavenProject.getId() + " has no license specified.");
				aReport.addMissingLicense(entry);
			} else {
				for (String license : licenses) {
					if (includeOnlyLikedInReport && licensingRequirements.containsLikedLicenses()) {
						if (licensingRequirements.isLikedLicense( license )) {
							entry.addLicense(license);
						}
					}
					else {
						entry.addLicense(license);
					}
				}

				if (isDisliked(mavenProject)) {
					getLog().warn("Licensing: The artifact " + mavenProject.getId() + " is only under a disliked license.");
					aReport.addDislikedArtifact(entry);
				} else {
					aReport.addLicensedArtifact(entry);
				}

			}

		}

		return aReport;
	}

	protected void checkForFailure(LicensingReport report) throws MojoFailureException {
		long disliked = report.getDislikedArtifacts().size();
		long missing = report.getLicenseMissing().size();

		if (disliked > 0 && missing > 0 && failIfDisliked && failIfMissing) {
			throw new MojoFailureException("This project has " + disliked + " disliked artifact" + ((disliked == 1) ? "" : "s") + " and " + missing + " artifact" + ((missing == 1) ? "" : "s")
					+ " missing licensing information.");
		} else if (missing > 0 && failIfMissing) {
			throw new MojoFailureException("This project has " + missing + " artifact" + ((missing == 1) ? "" : "s") + " missing licensing information.");
		} else if (disliked > 0 && failIfDisliked) {
			throw new MojoFailureException("This project has " + disliked + " disliked artifact" + ((disliked == 1) ? "" : "s") + ".");
		}

	}
}
