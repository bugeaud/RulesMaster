/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.bugeaud.generator.rules;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.List;
import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import nu.validator.messages.MessageEmitter;
import nu.validator.messages.MessageEmitterAdapter;
import nu.validator.messages.TextMessageEmitter;
import nu.validator.servlet.imagereview.ImageCollector;
import nu.validator.source.SourceCode;
import nu.validator.validation.SimpleDocumentValidator;
import nu.validator.xml.SystemErrErrorHandler;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Rewrite HTML to bring the coloring of embeded source code.
 * @author bugeaud at gmail dot com
 * @licence CeCILL 2.1
 */
public class HTMLRewriter{

    private String template;
    private CodeFormater formater;
    private Document document;
    
    protected static final String EMPTY_CODE = "";
    
    
    public HTMLRewriter(String template, CodeFormater formater) throws UnableToTransform {
        this.template = template;
        this.formater = formater;
        parse();
        transform();
    }
    
    protected synchronized void parse(){
        document = Jsoup.parseBodyFragment(template);
    }
    
    protected Elements buildDefaultStyle(){
        final String style = formater.highlightCode(EMPTY_CODE);
        return Jsoup.parse(style).select("style");
    }
    
    /**
     * Find the insertion root of a document. First try to get the head, then the body
     * if none exists return the document as a root
     * @return the root
     */
    protected Element getDocumentInsertionRoot(){
        Elements el = document.select("head");
        // Return the head element if it exists
        if(el!=null && el.size()>0){
            return el.first();
        }
        el = document.select("body");
        // Return the body element if it exists
        if(el!=null && el.size()>0){
            return el.first();
        }
        // If neither a head or a body exists, return the document
        return document;
    }
    
    /**
     * Ensure that pre blocks are seen as code block in the future
     */
    protected void fixPreCodeBlocks(){
        // Reverse the code > pre to pre > code
        final Elements reversedCodePre = document.select("code > pre");
        for(Element e : reversedCodePre){
            e.tagName("code");
            e.parent().tagName("pre");
        }
        
        final Elements preToFix = document.select("pre > *:not(code)");
        for(Element el : preToFix ){
            List<Node> nodes = el.childNodesCopy();
            el.empty();
            el.appendElement("code").insertChildren(0, nodes);            
        }
        
        /*
        // Get all the pre tags
        final Elements presTodo = document.select("pre");
        
        // Get all the code tags inside a pre tag
        final Elements presOk = document.select("pre > code");
        
        // Look for all the pre tag without a code tag inside
        presTodo.stream().filter((el) -> !(presOk.contains(el))).forEach((el) -> {
            List<Node> nodes = el.childNodesCopy();
            el.empty();
            el.appendElement("code").insertChildren(0, nodes);
        });
        */
    }
    
    
    protected void transform() throws UnableToTransform{
        transform(1);
    }
    
    protected void transform(int shiftLevel) throws UnableToTransform{
        
        // First ensure that there is no "lonely" pre tag that represents some code blocks
        fixPreCodeBlocks();
        
        // Get the formater's default style
        final Elements defaultStyle = buildDefaultStyle();
        
        // Insert this at the document's root
        final Element insertionRoot = getDocumentInsertionRoot();
        insertionRoot.insertChildren(0, defaultStyle.clone());
        
        // Add an style rule for monotype on .highlight font-familly: monospace ! important;
        insertionRoot.appendElement("style").attr("type", "text/css")
                .appendText("\n .highlight { font-family: monospace ! important; }");
        
        // First pass update the code blocks (pre>code)
        final Elements codeBlocks = document.select("pre > code, code > pre");
        transformCode(codeBlocks,false);
        
        // Second pass update the inline blocks (code tag alone)
        final Elements codeInline = document.select("code");
        transformCode(codeInline, true);
        
        // do some span cleaning
        final Elements dirtyNodes = document.select("span:empty");
        for(Element el : dirtyNodes){
            el.remove();
        }
        
        // Perform some extra cleaning for block
        final Elements cleanTargets = document.select("pre > div.highlight");
        for(Element e : cleanTargets){
            e.parent().tagName("div");
        }
        
        shiftHeading(shiftLevel);

    }
    
    private static final int LEVEL_CHAR_INDEX = 1; 
    
    /**
     * Increment the level of the heading in the current document by the shiftLevel amount
     * @param shiftLevel the amount to increment
     */
    protected void shiftHeading(int shiftLevel){
        final Elements headings = document.select("h1, h2, h3, h4, h5, h6, h7");
        for(Element e : headings){
            // increment the level of heading of the curent HTML heading tags
            e.tagName("h"+(Character.getNumericValue(e.tagName().charAt(LEVEL_CHAR_INDEX))+shiftLevel));
        }       
    }
    
    protected void transformCode(Elements elements, boolean inline) throws UnableToTransform{
    for(Element el : elements){
            Node child0 = el.childNode(0);
            if(! (child0 instanceof TextNode)){
                throw new UnableToTransform("Unexpected DOM Tree structure."
                        + " Check if the target source is a valid HTML, then", el);
            }
            // Fetch the source text to style 
            TextNode textNode = (TextNode)el.childNode(0);
            //final String sourceText = el.ownText();
            final String sourceText = textNode.getWholeText();
            
            // Style the text using the formater
            final String styledText = formater.highlightCode(sourceText);
            
            // Parse the styled text
            final Document styledTextDocument = Jsoup.parse(styledText);
            
            // Get the content of the Styled document
            final Elements selectedElements = styledTextDocument.body().children();
            
            // Merge the style text in the the Document
            //final Elements selectedElements = styledTextDocument.select("body");
            if(selectedElements !=null && selectedElements.size()>0){
                // Change the first container to a span (header was here)
                selectedElements.first().tagName("span");
                
                final Element parentContext = el.parent();
                //el.replaceWith(selectedElements.first().clone());
                
                // Backup the co
                final int insertionPoint = el.siblingIndex();
                
                // remove the code tag
                el.remove();
                
                // insert the new block
                parentContext.insertChildren(insertionPoint, selectedElements);
                
                // Change all the div and pre to span to ensure we are still inline
                if(inline){
                    // If content must be inlined, inactive the layout tags
                    final Elements cleanTargets = selectedElements.select("div.highlight, pre");
                    for(Element e : cleanTargets){
                        e.tagName("span");
                    }  
                }
                
            }
                    
        }
    }
    
    /**
     * Get the result of the rewritting as String
     * @return the result
     */
    public String result(){    
        return document.toString();
    }
    
    /*
    public static CodeFormater createCodeFormater()throws ScriptException{
        return createCodeFormater("CppLexer");
    }*/

    public static CodeFormater createCodeFormater(String lexer)throws ScriptException{
        return createCodeFormater(lexer,true);
    }
    
    /**
     * Create a code formater with a given lexer and formater
     * @param lexer the selected Pygments Lexer class name
     * @param generateStyle If true, the associated CSS definition will be included
     * @return the created CodeFormater
     * @throws ScriptException 
     */
    public static CodeFormater createCodeFormater(String lexer, boolean generateStyle)throws ScriptException{
        final ScriptEngineManager scriptManager = new ScriptEngineManager() ;
        final ScriptEngine scriptEngine = scriptManager.getEngineByName("jython");
        
        // Import the Pygments items
        scriptEngine.eval("from pygments import highlight");
        scriptEngine.eval("from pygments.lexers import "+lexer);
        scriptEngine.eval("from pygments.formatters import HtmlFormatter");
        
        // Set some default lexer and formater in the context according to the parameters
        scriptEngine.eval("lexer = "+lexer+"()");
        //scriptEngine.eval("formater = HtmlFormatter(full=True, style='native')");
        
        if(generateStyle){
            scriptEngine.eval("formater = HtmlFormatter(full=True)");
        }else{
            scriptEngine.eval("formater = HtmlFormatter()");
        }
        
        // Define a shim to set the default lexer and formater from the context
        scriptEngine.eval("def highlightCode(code): return highlight(code, lexer, formater)");
        
        // Return the shim as an interface for direct Java access
        return ((Invocable)scriptEngine).getInterface(CodeFormater.class);                       
    }
    
}
