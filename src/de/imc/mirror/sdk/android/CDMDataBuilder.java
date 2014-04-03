package de.imc.mirror.sdk.android;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.imc.mirror.sdk.cdm.CDMElement;
import de.imc.mirror.sdk.cdm.CDMVersion;
import de.imc.mirror.sdk.android.cdm.CDMData_0_1;
import de.imc.mirror.sdk.android.cdm.CDMData_0_2;
import de.imc.mirror.sdk.android.cdm.CDMData_1_0;
import de.imc.mirror.sdk.android.cdm.CDMData_2_0;
import de.imc.mirror.sdk.android.cdm.CreationInfo;
import de.imc.mirror.sdk.android.cdm.Reference;
import de.imc.mirror.sdk.android.cdm.References;
import de.imc.mirror.sdk.android.cdm.Summary;
import de.imc.mirror.sdk.android.exceptions.InvalidBuildException;

/**
 * Builder for information provided by the Common Data Model.
 * @author nmach, simon.schwantzer(at)im-c.de
 *
 */
public class CDMDataBuilder {
	
	private CDMVersion cdmVersion;
	private Map<String, String> attributes;
	private Map<String, CDMElement> elements;
	
	/**
	 * Creates a new builder for the CDM information.
	 * @param cdmVersion The version of the CDM to generate.
	 */
	public CDMDataBuilder(CDMVersion cdmVersion){
		attributes = new HashMap<String, String>();
		elements = new HashMap<String, CDMElement>();
		if (cdmVersion == null) {
			this.cdmVersion = CDMVersion.CDM_2_0;
		} else {
			this.cdmVersion = cdmVersion;
		}
		attributes.put("cdmVersion", this.cdmVersion.getVersionString());
		// place holder, will be replaced on publishing 
		this.setId("n/a");
		this.setTimestamp("1970-01-01T00:00:00+00:00");
	}
	
	/**
	 * Adds a reference to another data object.
	 * The reference indicates a "is child of" or "belongs to" relation. 
	 * CDM version: 0.2 or greater
	 * @param ref Data object identifier of the referenced object.
	 * @return The builder instance for chaining.
	 */
	public CDMDataBuilder setRef(String ref){
		attributes.put("ref", ref);
		return this;
	}
	
	/**
	 * Adds an object identifier.
	 * The data object identifier will be automatically set/overwritten by the Spaces Service when the
	 * object is published on a space. Custom identifiers are provided with the <code>customId</code>
	 * attribute.
	 * CDM version: 0.1 or greater 
	 * @param id String to set as data object identifier.
	 * @return The builder instance for chaining.
	 */
	public CDMDataBuilder setId(String id){
		attributes.put("id", id);
		return this;
	}
	
	/**
	 * Adds a publishing timestamp.
	 * The timestamp will be automatically set/overwritten by the Spaces Service when the object is
	 * published on a space.
	 * CDM version: 0.1 or greater
	 * @param timestamp Timestamp as ISO 8601 string.
	 * @return The builder instance for chaining.
	 */
	public CDMDataBuilder setTimestamp(String timestamp){
		attributes.put("timestamp", timestamp);
		return this;
	}

	/**
	 * Adds a custom identifier for the data object.
	 * CDM version: 1.0 or greater
	 * @param customId String to be used as custom identifier.
	 * @return The builder instance for chaining.
	 */
	public CDMDataBuilder setCustomId(String customId){
		attributes.put("customId", customId);
		return this;
	}

	/**
	 * Sets the publisher of the data object.
	 * When published on a space, the publisher will be corrected by the Spaces Service if set and
	 * not correct.
	 * CDM version: 1.0 or greater
	 * @param publisher Full-JID of the user publishing the data object. 
	 * @return The builder instance for chaining.
	 */
	public CDMDataBuilder setPublisher(String publisher){
		attributes.put("publisher", publisher);
		return this;
	}
	
	/**
	 * Sets the creator of the data object.
	 * With CDM version 1.0, the creator information moved to the Common Data Types and is now part
	 * of the application specific data model.
	 * CDM version: 0.1, 0.2
	 * @param creator Full-JID of the user who created the data object.
	 * @return The builder instance for chaining.
	 */
	public CDMDataBuilder setCreator(String creator){
		attributes.put("creator", creator);
		return this;
	}
	
	/**
	 * Sets the version of the data model instantiated by the data object.
	 * The model version information is REQUIRED since CDM version 1.0.
	 * CDM version: 1.0 or greater.
	 * @param modelVersion Version of the data model as string.
	 * @return The builder instance for chaining.
	 */
	public CDMDataBuilder setModelVersion(String modelVersion){
		attributes.put("modelVersion", modelVersion);
		return this;
	}
	
	/**
	 * Marks the object as update for another data object.
	 * CDM version: 2.0 or greater.
	 * @param objectId Identifier for the data object updated with this object.
	 * @return The builder instance for chaining.
	 */
	public CDMDataBuilder setUpdates(String objectId) {
		attributes.put("updates", objectId);
		return this;
	}
	
	/**
	 * Marks the object as copy of another data object.
	 * CDM version: 2.0 or greater.
	 * @param objectId Identifier of the data object copied by this object.
	 * @return The builder instance for chaining.
	 */
	public CDMDataBuilder setCopyOf(String objectId) {
		attributes.put("copyOf", objectId);
		return this;
	}
	
	/**
	 * Sets a textual summary for the data object.
	 * @param summary Summary of the data objects content.
	 * @return The builder instance for chaining.
	 */
	public CDMDataBuilder setSummary(Summary summary) {
		elements.put("summary", summary);
		return this;
	}
	
	/**
	 * Sets the creation information for the data object.
	 * @param creationInfo Creation information object.
	 * @return The builder instance for chaining.
	 */
	public CDMDataBuilder setCreationInfo(CreationInfo creationInfo) {
		elements.put("creationInfo", creationInfo);
		return this;
	}
	
	/**
	 * Sets the list of references for the data object.
	 * @param references References object.
	 * @return The builder instance for chaining.
	 */
	public CDMDataBuilder setReferences(References references) {
		elements.put("references", references);
		return this;
	}
	
	/**
	 * Adds an reference.
	 * @param reference Reference to add.
	 * @return The builder instance for chaining.
	 */
	public CDMDataBuilder addReference(Reference reference) {
		List<Reference> referenceList; 
		if (elements.containsKey("references")) {
			referenceList = ((References) elements.get("references")).getReferences();
		} else {
			referenceList = new ArrayList<Reference>();
		}
		referenceList.add(reference);
		setReferences(new References(referenceList));
		return this;
	}
	
	/**
	 * Builds and returns a CDMDataObject with the given parameters.
	 * @return A typed CDMData object with the information provided to this builder.
	 * @throws InvalidBuildException The given data cannot be applied to a CDM of the given version. 
	 */
	public CDMData build() throws InvalidBuildException {
		CDMData cdmData;
		switch (cdmVersion){
		case CDM_0_1:
			cdmData = new CDMData_0_1(attributes, elements);
			break;
		case CDM_0_2:
			cdmData = new CDMData_0_2(attributes, elements);
			break;
		case CDM_1_0:
			cdmData = new CDMData_1_0(attributes, elements);
			break;
		case CDM_2_0:
		default:
			cdmData = new CDMData_2_0(attributes, elements);
		}
		return cdmData;
	}
}
