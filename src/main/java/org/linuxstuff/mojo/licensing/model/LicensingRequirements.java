package org.linuxstuff.mojo.licensing.model;

import java.util.HashSet;
import java.util.Set;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamImplicit;

@XStreamAlias("licensing-requirements")
public class LicensingRequirements {

	@XStreamAlias("missing-licenses")
	private Set<ArtifactWithLicenses> missingLicenses = new HashSet<ArtifactWithLicenses>();

    @XStreamAlias("override-licenses")
    private Set<ArtifactWithLicenses> overrideLicenses = new HashSet<ArtifactWithLicenses>();
	
    @XStreamAlias("missing-artifacts")
    private Set<ArtifactWithLicenses> missingArtifacts = new HashSet<ArtifactWithLicenses>();

	@XStreamAlias("coalesced-licenses")
	private Set<CoalescedLicense> coalescedLicenses = new HashSet<CoalescedLicense>();

    @XStreamAlias("dual-licenses")
    private Set<DualLicense> dualLicenses = new HashSet<DualLicense>();

	@XStreamAlias("disliked-licenses")
	@XStreamImplicit(itemFieldName = "disliked-license")
	private Set<String> dislikedLicenses = new HashSet<String>();

	@XStreamAlias("liked-licenses")
	@XStreamImplicit(itemFieldName = "liked-license")
	private Set<String> likedLicenses = new HashSet<String>();

	@XStreamAlias("dislike-exemptions")
	@XStreamImplicit(itemFieldName = "dislike-exemption")
	private Set<String> dislikeExemptions = new HashSet<String>();

	public void addArtifactMissingLicense(ArtifactWithLicenses missingLicense) {
		missingLicenses.add(missingLicense);
	}
	
	public void addOverrideLicense(ArtifactWithLicenses overrideLicense) {
	    overrideLicenses.add( overrideLicense );
	}
	
	public void addMissingArtifact(ArtifactWithLicenses missingArtifact) {
	    missingArtifacts.add(missingArtifact);
	}

	public void addCoalescedLicense(CoalescedLicense coalescedLicense) {
		coalescedLicenses.add(coalescedLicense);
	}
	
	public void addDualLicense(DualLicense dualLicense) {
	    dualLicenses.add( dualLicense );
	}

	public void addDislikedLicense(String licenseName) {
		dislikedLicenses.add(licenseName);
	}

	public void addLikedLicense(String licenseName) {
		likedLicenses.add(licenseName);
	}

	public void addDislikeExemption(String artifactId) {
		dislikeExemptions.add(artifactId);
	}

	public boolean isDislikedLicense(String license) {
		return dislikedLicenses.contains(license);
	}

	public boolean isLikedLicense(String license) {
		return likedLicenses.contains(license);
	}
	
	public String getCorrectLicenseName(String name) {
		for (CoalescedLicense coalesced : coalescedLicenses) {
			if (coalesced.getFinalName().equalsIgnoreCase(name.trim()))
				return name;
			for (String otherName : coalesced.getOtherNames()) {
				if (otherName.equalsIgnoreCase(name.trim()))
					return coalesced.getFinalName();
			}
		}

		return name;
	}
	
	/**
	 * Coalesce license names and split dual licenses, replace override licenses.
	 * @param artifact
	 */
	public void normalizeLicenses(ArtifactWithLicenses artifact) {
	    String id = artifact.getArtifactId();
	    for (ArtifactWithLicenses replacement : overrideLicenses) {
	        // check only start of id, so the version can be left out
	        if (id.startsWith( replacement.getArtifactId())) {
	            artifact.setLicenses( replacement.getLicenses() );
	            break;
	        }
	    }
	    Set<String> normalizedLicenses = new HashSet<String>();
	    for (String license: artifact.getLicenses())
	    {
	        String correct = getCorrectLicenseName( license );
	        boolean found = false;
            for (DualLicense dualLicense : getDualLicenses())
            {
                if (correct.equals(dualLicense.getFinalName()))
                {
                    for (String option : dualLicense.getOptionalLicenses())
                    {
                        normalizedLicenses.add( option );
                    }
                    found = true;
                    continue;
                }
            }
            if (!found)
	        {
	            normalizedLicenses.add( correct );
	        }
	    }
	    artifact.setLicenses( normalizedLicenses );
	}

	public boolean isExemptFromDislike(String artifactId) {
		if (dislikeExemptions == null) {
			return false;
		}

		return dislikeExemptions.contains(artifactId);
	}

	public Set<String> getLicenseNames(String id) {
		Set<String> licenses = new HashSet<String>();
		for (ArtifactWithLicenses missing : missingLicenses) {
			if (missing.getArtifactId().equals(id)) {
				licenses.addAll(missing.getLicenses());
			}
		}

		return licenses;
	}

	public boolean containsDislikedLicenses() {
		return !dislikedLicenses.isEmpty();
	}

	public boolean containsLikedLicenses() {
		return !likedLicenses.isEmpty();
	}

	public Set<ArtifactWithLicenses> getMissingLicenses() {
		return missingLicenses;
	}
	
	public Set<ArtifactWithLicenses> getOverrideLicenses() {
	    return overrideLicenses;
	}

    public Set<ArtifactWithLicenses> getMissingArtifacts() {
        return missingArtifacts;
    }

	public Set<CoalescedLicense> getCoalescedLicenses() {
		return coalescedLicenses;
	}
	
	public Set<DualLicense> getDualLicenses() {
	    return dualLicenses;
	}

	public Set<String> getDislikedLicenses() {
		return dislikedLicenses;
	}

	public Set<String> getLikedLicenses() {
		return likedLicenses;
	}

	public Set<String> getDislikeExemptions() {
		return dislikeExemptions;
	}

	public void combineWith(LicensingRequirements req) {

		if (req.getDislikedLicenses() != null) {
			for (String source : req.getDislikedLicenses()) {
				addDislikedLicense(source);
			}
		}

		if (req.getLikedLicenses() != null) {
			for (String source : req.getLikedLicenses()) {
				addLikedLicense(source);
			}
		}

		if (req.getDislikeExemptions() != null) {
			for (String source : req.getDislikeExemptions()) {
				addDislikeExemption(source);
			}
		}

        mergeArtifactsWithLicenses( req.getMissingLicenses(), getMissingLicenses() );
        mergeArtifactsWithLicenses( req.getOverrideLicenses(), getOverrideLicenses() );
		mergeArtifactsWithLicenses( req.getMissingArtifacts(), getMissingArtifacts() );

		if (req.getCoalescedLicenses() != null) {

			for (CoalescedLicense source : req.getCoalescedLicenses()) {
				if (getCoalescedLicenses().contains(source)) {
					for (CoalescedLicense destination : getCoalescedLicenses()) {
						if (source.equals(destination)) {
							destination.combineWith(source);
						}
					}
				} else {
					addCoalescedLicense(source);
				}
			}
		}


        if (req.getDualLicenses() != null) {

            for (DualLicense source : req.getDualLicenses()) {
                if (getDualLicenses().contains(source)) {
                    for (DualLicense destination : getDualLicenses()) {
                        if (source.equals(destination)) {
                            destination.combineWith(source);
                        }
                    }
                } else {
                    addDualLicense(source);
                }
            }
        }
	}

    private void mergeArtifactsWithLicenses(
            Set<ArtifactWithLicenses> sourceArtifacts,
            Set<ArtifactWithLicenses> destinationArtifacts )
    {
        if (sourceArtifacts != null) {
		    for (ArtifactWithLicenses source : sourceArtifacts) {
		        if (destinationArtifacts.contains(source)) {
		            for (ArtifactWithLicenses destination : destinationArtifacts) {
		                if (source.equals( destination )) {
		                    destination.combineWith(source);
		                }
		            }
		        } else {
		            destinationArtifacts.add( source );
		        }
		    }
		}
    }
}
