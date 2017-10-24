package fr.bugeaud.generator.rules;

import fr.bugeaud.tools.sonar.rules.client.SonarRule;
import java.util.Map;

/**
 * A custom rules bean derivated from SonarQube
 * Description of the rule (htmlDesc), can be plain text or an html bribe (&lt;body&gt; like content).
 * Use a code tag to indicate an inlined source code reference.
 * Use a code tag inside a pre tag to get a block code reference. 
 * @author bugeaud at gmail dot com
 * @licence CeCILL 2.1 
 */
public class Rule extends SonarRule {
    
    /**
     * Identifier of the rule, shall be unique
     */    
    private String id;
   
    /**
     * List the category, this is used to group rules
     */
    private String category;

    
    private String source;
    private String comment;
    
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

    /**
     * @return the source
     */
    public String getSource() {
        return source;
    }

    /**
     * @param source the source to set
     */
    public void setSource(String source) {
        this.source = source;
    }

    /**
     * @return the comment
     */
    public String getComment() {
        return comment;
    }

    /**
     * @param comment the comment to set
     */
    public void setComment(String comment) {
        this.comment = comment;
    }
            
}
