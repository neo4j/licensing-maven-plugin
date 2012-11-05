package org.linuxstuff.mojo.licensing;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Set;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.FileUtils;
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
     * @since 1.7.3
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
     * @since 1.7.3
     */
	protected boolean includeOnlyLikedInReport;

    /**
     * Output the result as a plain text file.
     * 
     * @parameter expression="${writeTextReport}" default-value="false"
     * @since 1.7.3
     */
    protected boolean plainTextReport;

    /**
     * The name of the  output txt file which contains full text licenses.
     * Only kicks in as part of generating a plain text report.
     * 
     * @parameter expression="${listReport}"
     */
    protected String listReport;

    /**
     * File to prepend to the text-based report with full license texts.
     * 
     * @parameter expression="${listPrependText}"
     */
    protected String listPrependText;

    /**
     * File to prepend to the text-based report.
     * 
     * @parameter expression="${prependText}"
     * @since 1.7.3
     */
    protected String prependText;

    /**
     * File to append to the text-based report.
     * 
     * @parameter expression="${appendText}"
     * @since 1.7.3
     */
    protected String appendText;

    /**
     * File to append to the text-based report.
     * 
     * @parameter expression="${checkExistingNoticeFile}"
     * @since 1.7.4
     */
    protected String checkExistingNoticeFile;

    /**
     * File to append to the text-based report.
     * 
     * @parameter expression="${checkExistingLicensesFile}"
     * @since 1.7.4
     */
    protected String checkExistingLicensesFile;

    /**
     * Overwrite existing license/notice.txt files.
     * 
     * @parameter expression="${overwrite}" default-value="false"
     * @since 1.7.5
     */
    protected boolean overwrite;

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
            // notice file
	        report.writeTextReport(file, locator, prependText, appendText, true, false);
            compareToExistingFile( file, checkExistingNoticeFile );
	        if (listReport != null) {
                // licenses file
	            file = new File(project.getBuild().getDirectory(), listReport);
	            report.writeTextReport(file, locator, listPrependText, null, false, true);
                compareToExistingFile( file, checkExistingLicensesFile );
	        }
		} else {
		    report.writeReport(file);
		}
		
		checkForFailure(report);

	}

    private void compareToExistingFile( File file, String existingFileName )
            throws MojoExecutionException
    {
        if ( existingFileName != null )
        {
            File existingFile = FileUtils.getFile( existingFileName );
            try
            {
                if ( !FileUtils.contentEquals( file, existingFile ) )
                {
                    generatedAndExistingDiffer( file, existingFile );
                }
                else
                {
                    getLog().info( "File confirmed: " + existingFileName );
                }
            }
            catch ( IOException ioe )
            {
                throw new MojoExecutionException(
                        "Could not compare files.", ioe );
            }
        }
    }

    protected void generatedAndExistingDiffer( File file, File existingFile )
            throws MojoExecutionException
    {
        if ( overwrite )
        {
            try
            {
                getLog().info( "Replacing " + existingFile );
                FileUtils.rename( file, existingFile );
            }
            catch ( IOException e )
            {
                throw new MojoExecutionException(
                        "Could not overwrite the existing file.\n"
                                + "Generated: " + file + "\n" + "Existing: "
                                + existingFile );
            }
        }
        else
        {
            throw new MojoExecutionException(
                    "Generated file differs from the existing file.\n"
                            + "Generated: " + file + "\n" + "Existing: "
                            + existingFile );
        }
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
				getLog().warn("Licensing: The artifact " + entry.getArtifactId() + " has no license specified.");
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

                licensingRequirements.normalizeLicenses( entry );

                if ( isDisliked( entry ) )
                {
                    getLog().warn(
                            "Licensing: The artifact " + entry.getArtifactId()
                                    + " is only under disliked licenses: " + licenses );
					aReport.addDislikedArtifact(entry);
				} else {
					aReport.addLicensedArtifact(entry);
				}
			}
		}
		
		for (ArtifactWithLicenses artifactWithLicenses : licensingRequirements.getMissingArtifacts()) {
            ArtifactWithLicenses entry = new ArtifactWithLicenses(
                    artifactWithLicenses.getArtifactId(),
                    artifactWithLicenses.getName() );
            Set<String> licenses = artifactWithLicenses.getLicenses();

            if (licenses.isEmpty()) {
                getLog().warn("Licensing: The artifact " + entry.getArtifactId() + " has no license specified.");
                aReport.addMissingLicense(entry);
            } else {
                for (String license : licenses) {
                    String correct = licensingRequirements.getCorrectLicenseName( license );
                    if (includeOnlyLikedInReport && licensingRequirements.containsLikedLicenses()) {
                        if (licensingRequirements.isLikedLicense( correct )) {
                            entry.addLicense(correct);
                        }
                    }
                    else {
                        entry.addLicense(correct);
                    }
                }

                licensingRequirements.normalizeLicenses( entry );

                if (isDisliked(entry)) {
                    getLog().warn(
                            "Licensing: The artifact " + entry.getArtifactId()
                                    + " is only under disliked licenses: "
                                    + licenses );
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
