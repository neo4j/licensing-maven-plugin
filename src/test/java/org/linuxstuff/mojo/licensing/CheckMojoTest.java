package org.linuxstuff.mojo.licensing;

import com.google.common.collect.Sets;
import org.apache.maven.model.License;
import org.apache.maven.project.MavenProject;
import org.junit.Test;
import org.linuxstuff.mojo.licensing.model.CoalescedLicense;
import org.linuxstuff.mojo.licensing.model.LicensingReport;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.TreeMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class CheckMojoTest extends AbstractLicensingTest
{

    @Test
    public void artifactWithSingleCoalescingLicenseIsValid()
    {
        String mitLicense = "MIT License";
        String anotherMitLicense = "MIT license";
        License license = prepareLisence( anotherMitLicense );
        DependenciesTool dependenciesTool = prepareDependencyTool( license );

        CheckMojo checkMojo = new CheckMojo();
        checkMojo.dependenciesTool = dependenciesTool;
        checkMojo.licensingRequirements.addLikedLicense( mitLicense );
        checkMojo.licensingRequirements.addCoalescedLicense( new CoalescedLicense( mitLicense, Sets.newHashSet( anotherMitLicense ) ) );
        checkMojo.includeOnlyLikedInReport = true;

        LicensingReport licensingReport = checkMojo.generateReport( mavenProject );
        assertTrue( licensingReport.getDislikedArtifacts().isEmpty() );
        assertEquals( 1, licensingReport.getLicensedArtifacts().size() );
    }

    private static License prepareLisence( String anotherMitLicense )
    {
        License license = new License();
        license.setName( anotherMitLicense );
        return license;
    }

    private static DependenciesTool prepareDependencyTool( License license )
    {
        TreeMap<String,MavenProject> projectMap = new TreeMap<>();
        MavenProject dependencyProject = new MavenProject();
        dependencyProject.setLicenses( Collections.singletonList( license ) );
        projectMap.put( "a", dependencyProject );
        DependenciesTool dependenciesTool = Mockito.mock( DependenciesTool.class );
        when( dependenciesTool.loadProjectDependencies( any(), any(), any(), any(), any() ) ).thenReturn( projectMap );
        return dependenciesTool;
    }
}