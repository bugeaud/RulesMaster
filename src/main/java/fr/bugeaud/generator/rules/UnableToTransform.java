package fr.bugeaud.generator.rules;

import org.jsoup.nodes.Element;

/**
 * UnableToTrasform is an exception that will be raised while browsing when the required transform is not able to be performed
 * @author bugeaud at gmail dot com
 * @licence CeCILL 2.1
 */
public class UnableToTransform extends Exception {
    
    public UnableToTransform(String reason, Element root) {
        super(String.format("Unable to transform because at %s for the reason: %s",generatePath(root),reason));
    }
    
    private static final String generatePath(Element root){
        StringBuilder builder = new StringBuilder();
        generatePath(builder,root);
        return builder.toString();
    }
    
    private static final void generatePath(StringBuilder builder, Element root){
        Element parent = root.parent();
        if(parent!=null){
            generatePath(builder, parent);
        }
        builder.append("/").append(root.nodeName());        
    }
    
}
