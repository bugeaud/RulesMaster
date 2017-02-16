package fr.bugeaud.generator.rules;

/**
 * This interface define a contract with the Python beautifier package to ease the call
 * @author bugeaud at gmail dot com
 */
public interface CodeFormater {
    /**
     * Highlight the code according to the parameters set
     * @param code the source code
     * @return the highlighted source code
     */
    String highlightCode(String code);  
}
   