package org.linuxstuff.mojo.licensing;

import org.junit.Assert;
import org.junit.Test;
import org.linuxstuff.mojo.licensing.model.ArtifactWithLicenses;
import org.linuxstuff.mojo.licensing.model.CoalescedLicense;
import org.linuxstuff.mojo.licensing.model.DualLicense;
import org.linuxstuff.mojo.licensing.model.LicensingRequirements;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.StaxDriver;

public class ReadLicensingRequirementsTest {

	@Test
	public void xstreamShouldBeAbleToReadTheRequirementsFile() {
		XStream xstream = new XStream(new StaxDriver());

		xstream.processAnnotations(LicensingRequirements.class);
		xstream.processAnnotations(ArtifactWithLicenses.class);
		xstream.processAnnotations(CoalescedLicense.class);

		LicensingRequirements licensingRequirements = (LicensingRequirements) xstream
			.fromXML(this.getClass().getResourceAsStream("/licensing-requirements-example.xml"));

		Assert.assertEquals(1, licensingRequirements.getDualLicenses().size());
		DualLicense dualLicense = licensingRequirements.getDualLicenses().iterator().next();
		Assert.assertEquals(
			"Common Development and Distribution License Version 1.1 and GNU General Public License, version 2 with the Classpath Exception",
			dualLicense.getFinalName());
		Assert.assertEquals(2, dualLicense.getOptionalLicenses().size());

		Assert.assertEquals(1, licensingRequirements.getCoalescedLicenses().size());
		CoalescedLicense coalescedLicense = licensingRequirements.getCoalescedLicenses().iterator().next();
		Assert.assertEquals(
			"GNU Affero General Public License, Version 3",
			coalescedLicense.getFinalName());
		Assert.assertEquals(1, coalescedLicense.getOtherNames().size());
	}
}
