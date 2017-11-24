package org.linuxstuff.mojo.licensing;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.linuxstuff.mojo.licensing.model.ArtifactWithLicenses;
import org.linuxstuff.mojo.licensing.model.LicensingReport;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static java.util.Arrays.asList;

public class CheckForFailureTest {

	@Rule
	public TemporaryFolder temporaryFolder = new TemporaryFolder();

	/**
	 * Ignore all our problems.
	 */
	@Test
	public void testIgnoreEverything() throws MojoFailureException {

		LicensingReport report = new LicensingReport();

		CheckMojo mojo = new CheckMojo();

		mojo.failIfDisliked = false;
		mojo.failIfMissing = false;

		report.addMissingLicense(new ArtifactWithLicenses("missing"));
		report.addDislikedArtifact(new ArtifactWithLicenses("disliked"));

		mojo.checkForFailure(report);
	}

	/**
	 * Blow up because of the disliked artifact.
	 */
	@Test(expected = MojoFailureException.class)
	public void testDislkedThrowsException() throws MojoFailureException {
		LicensingReport report = new LicensingReport();

		CheckMojo mojo = new CheckMojo();

		mojo.failIfDisliked = true;
		mojo.failIfMissing = false;

		report.addDislikedArtifact(new ArtifactWithLicenses("disliked"));

		mojo.checkForFailure(report);

	}

	/**
	 * Blow up because of the artifact with a missing license.
	 */
	@Test(expected = MojoFailureException.class)
	public void testMissingThrowsException() throws MojoFailureException {
		LicensingReport report = new LicensingReport();

		CheckMojo mojo = new CheckMojo();

		mojo.failIfDisliked = false;
		mojo.failIfMissing = true;

		report.addMissingLicense(new ArtifactWithLicenses("disliked"));

		mojo.checkForFailure(report);

	}

	/**
	 * Don't blow up about the disliked artifact (but enable blowing up for
	 * artifacts missing licenses).
	 */
	@Test
	public void testIgnoreDisliked() throws MojoFailureException {

		LicensingReport report = new LicensingReport();

		CheckMojo mojo = new CheckMojo();

		mojo.failIfDisliked = false;
		mojo.failIfMissing = true;

		report.addDislikedArtifact(new ArtifactWithLicenses("disliked"));

		mojo.checkForFailure(report);
	}

	/**
	 * Don't blow up about artifacts missing licenses (but enable blowing up for
	 * disliked licenses).
	 */
	@Test
	public void testIgnoreMissing() throws MojoFailureException {

		LicensingReport report = new LicensingReport();

		CheckMojo mojo = new CheckMojo();

		mojo.failIfDisliked = true;
		mojo.failIfMissing = false;

		report.addMissingLicense(new ArtifactWithLicenses("missing"));

		mojo.checkForFailure(report);
	}

	/**
	 * Also blow up if *everything* is bad. Technically the user sees a
	 * different message, but I'm not about to scrape an exception message.
	 */
	@Test(expected = MojoFailureException.class)
	public void testEverythingIsBad() throws MojoFailureException {

		LicensingReport report = new LicensingReport();

		CheckMojo mojo = new CheckMojo();

		mojo.failIfDisliked = true;
		mojo.failIfMissing = true;

		report.addMissingLicense(new ArtifactWithLicenses("missing"));
		report.addDislikedArtifact(new ArtifactWithLicenses("disliked"));

		mojo.checkForFailure(report);
	}

	@Test
	public void ignoreLicenseFilesEndOfLineCharactersDuringComparision()
			throws MojoExecutionException, IOException
	{
		CheckMojo checkMojo = new CheckMojo();

		List<String> fileLines = asList( "a", "b", "c" );
		File windowsLicenseFile = generateTextFile( fileLines, "windows", IOUtils.LINE_SEPARATOR_WINDOWS );
		File linuxLicenseFile = generateTextFile( fileLines, "linux", IOUtils.LINE_SEPARATOR_UNIX );

		checkMojo.compareToExistingFile( windowsLicenseFile, linuxLicenseFile.getAbsolutePath() );
	}

	@Test( expected = MojoExecutionException.class )
	public void detectDifferentLicenseFilesFromDifferentPlatforms() throws IOException, MojoExecutionException
	{
		CheckMojo checkMojo = new CheckMojo();

		File windowsLicenseFile = generateTextFile( asList( "a", "b", "c" ), "windows", IOUtils.LINE_SEPARATOR_WINDOWS );
		File linuxLicenseFile = generateTextFile( asList( "a", "c", "b" ), "linux", IOUtils.LINE_SEPARATOR_UNIX );

		checkMojo.compareToExistingFile( windowsLicenseFile, linuxLicenseFile.getAbsolutePath() );
	}

	private File generateTextFile( List<String> fileLines, String fileName, String lineSeparator ) throws IOException
	{
		File windowsLicenseFile = temporaryFolder.newFile( fileName );
		FileUtils.writeLines( windowsLicenseFile, fileLines, lineSeparator );
		return windowsLicenseFile;
	}
}
