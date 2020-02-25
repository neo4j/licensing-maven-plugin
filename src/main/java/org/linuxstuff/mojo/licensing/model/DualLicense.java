package org.linuxstuff.mojo.licensing.model;

import java.util.Set;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamImplicit;

@XStreamAlias("dual-license")
public class DualLicense {

	@XStreamAsAttribute
	@XStreamAlias("name")
	private String finalName;

	@XStreamImplicit(itemFieldName = "option")
	private Set<String> optionalLicenses;

	public DualLicense() {
		/* Hello XStream */
	}

	public DualLicense(String finalName, Set<String> optionalLicenses) {
		this.finalName = finalName;
		this.optionalLicenses = optionalLicenses;
	}

	public String getFinalName() {
		return finalName;
	}

	public Set<String> getOptionalLicenses() {
		return optionalLicenses;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((finalName == null) ? 0 : finalName.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DualLicense other = (DualLicense) obj;
		if (finalName == null) {
			if (other.finalName != null)
				return false;
		} else if (!finalName.equals(other.finalName))
			return false;
		return true;
	}

	/**
	 * This method does not attempt to do anything creative with cascading final
	 * names or anything. It simply combines the "optional licenses" of the other
	 * {@code DualLicense}.
	 */
	public void combineWith(DualLicense other) {
		optionalLicenses.addAll(other.getOptionalLicenses());
	}
}
