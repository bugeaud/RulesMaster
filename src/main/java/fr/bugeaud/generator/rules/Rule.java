package fr.bugeaud.generator.rules;

import java.util.Map;

/**
 * A rules bean
 * @author bugeaud at gmail dot com
 * @licence CeCILL 2.1 
 */
public class Rule {
    
    /**
     * Identifier of the rule, shall be unique
     */    
    private String id;
    /**
     * Some human friendly/meanfull name for the rule
     */
    private String name;
    
    /**
     * Description of the rule, can be plain text or an html bribe (&lt;body&gt; like content).
     * Use a code tag to indicate an inlined source code reference.
     * Use a code tag inside a pre tag to get a block code reference. 
     */
    private String description;
    /**
     * If not integrated inside the description, list a valid code example
     */
    private String validExample;
    /**
     * If not integrated inside the description, list an invalid code example
     */
    private String invalidExample;
        
    private String severity;
    private String status;
    private String type;
    /**
     * List the category, this is used to group rules
     */
    private String category;
    
    /**
     * Indicate external references in third party document. Key is the document name and Value the reference inside the document.
     */
    private Map<String, String> references;

    /**
     * @return the id
     */
    public String getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * @param description the description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * @return the validExample
     */
    public String getValidExample() {
        return validExample;
    }

    /**
     * @param validExample the validExample to set
     */
    public void setValidExample(String validExample) {
        this.validExample = validExample;
    }

    /**
     * @return the invalidExample
     */
    public String getInvalidExample() {
        return invalidExample;
    }

    /**
     * @param invalidExample the invalidExample to set
     */
    public void setInvalidExample(String invalidExample) {
        this.invalidExample = invalidExample;
    }

    /**
     * @return the severity
     */
    public String getSeverity() {
        return severity;
    }

    /**
     * @param severity the severity to set
     */
    public void setSeverity(String severity) {
        this.severity = severity;
    }

    /**
     * @return the status
     */
    public String getStatus() {
        return status;
    }

    /**
     * @param status the status to set
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * @return the type
     */
    public String getType() {
        return type;
    }

    /**
     * @param type the type to set
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * @return the category
     */
    public String getCategory() {
        return category;
    }

    /**
     * @param category the category to set
     */
    public void setCategory(String category) {
        this.category = category;
    }

    /**
     * @return the references
     */
    public Map<String, String> getReferences() {
        return references;
    }

    /**
     * @param references the references to set
     */
    public void setReferences(Map<String, String> references) {
        this.references = references;
    }
    
    @Override
    public String toString() {
        final String id = getId();
        return id==null ? super.toString(): id + " " + getName();
    }
            
}
