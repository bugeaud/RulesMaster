package fr.bugeaud.generator.rules;

import com.beust.jcommander.DynamicParameter;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.docx4j.openpackaging.exceptions.Docx4JException;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.openpackaging.parts.WordprocessingML.AltChunkType;
import org.docx4j.openpackaging.parts.WordprocessingML.MainDocumentPart;
import org.docx4j.toc.TocGenerator;
import org.docx4j.wml.ObjectFactory;
import org.docx4j.wml.Tbl;
import org.docx4j.wml.Tc;
import org.docx4j.wml.Tr;

/**
 * Generate a document based on the supplied rule spreadsheet
 * @author bugeaud at gmail dot com
 * @licence CeCILL 2.1
 */
public class DocumentGenerator {
    
    @Parameter(names="-in",description = "The spreadsheet file containing the rules", required = true)
    private String inputFileName;
    
    @Parameter(names="-sheet",description = "The spreadsheet's sheet name", required = true)
    private String sheetName;
    
    @Parameter(names="-out", description = "The generated DOCX document that will display the rules", required = true)
    private String outputFileName;
    
    @Parameter(names="-lexer", description = "The Pygment lexer class name to use (", required = true)
    private String lexerName;
    
    @DynamicParameter(names = "-A", description = "Add a property/column alias for name resolution. Multiple aliases are accepted for the same target property.")
    private Map<String, String> aliases = new HashMap<>();
    
    @Parameter(names = "-G", description = "Group using the category field into subtrees")
    private boolean group = false;
    
    @Parameter(names = "-filter", description = "A Java filter expression Group using the category field into subtrees")
    String filter;
    
    @Parameter(names = "-v", description = "Verbose, print more log (info, errors ...)")
    private boolean verbose = false;
    
    @Parameter(names = "-V", description = "Validate the HTML content of the descriptions. This is usefull when debugging some generation error.")
    private boolean validate = false;
    
    @Parameter(names = "-help", help = true, description = "Shows this help")
    private boolean help = false;

    public boolean isHelp(){
        return help;
    }
  
    public boolean shouldValidate(){
        return validate;
    } 
   
    public boolean isVerbose(){
        return verbose;
    }
    
    public static final int USAGE_EXIT_CODE = 2;

    
    public DocumentGenerator(){
    }

    protected ObjectFactory factory = new ObjectFactory();
    
    protected void addTc(MainDocumentPart doc, Tr tr, String text) {
        final Tc tc = factory.createTc();
        tc.getContent().add( doc.createParagraphOfText(text) );
        tr.getContent().add( tc );
    }
    
    private static final int DEFAULT_WIDTH = 30;
    
    public void generateReferences(MainDocumentPart doc, Map<String,String> references){
        if (references == null || references.isEmpty()){
            // If no references was given, simply don't do anything
            return;
        }
        final Tbl tbl = factory.createTbl();// TblFactory.createTable(0, 5, DEFAULT_WIDTH/references.size());
        final Tr tr = factory.createTr();
        
        for (Map.Entry e : references.entrySet()) {
            // Add the cell with the reference
            addTc(doc, tr, String.format("\u2316%s§%s", e.getKey(),e.getValue()));
        }
        // Add the row to the document
        tbl.getContent().add(tr);

        // Add the table to the doc
        doc.addObject(tbl);
        
    }
    
    /**
     * Create a heading paragraph with the according style for the title in the document
     * @param level the level of the paragraph
     * @param title the title to display
     * @param doc the document to write into
     */
    public void generateParagrapth(int level, String title, MainDocumentPart doc) {
        doc.addStyledParagraphOfText("Heading"+level, title);
    }
    
    public void generateRuleList(int level, List<Rule> rules, MainDocumentPart doc, CodeFormater formater ) throws Docx4JException {
            for(Rule rule : rules){
                // Temporary skip all rules but 670
                /*if(!"670".equals(rule.getId())){
                    continue;
                }*/
                
                /*final String validExample = rule.getValidExample();
                final String invalidExample = rule.getInvalidExample();*/
                
                // Create the title
                doc.addStyledParagraphOfText("Heading"+level, String.format("%s - %s", rule.getId(),rule.getName()));
                
                // Add the synthetic references 
                generateReferences(doc,rule.getReferences());
                
                // Create the description, and add inline to bring color instead of the basic
                // doc.addParagraphOfText(...);
                if(rule.getHtmlDesc()!=null){
                    if (shouldValidate()){
                        try{
                            HTMLValidator.validateHtml(rule.getHtmlDesc());
                        }catch(InvalidHTML ex){
                            Logger.getLogger(DocumentGenerator.class.getName()).log(Level.WARNING, String.format("The description (ID=%s) has some HTML validity issues",rule.getId()), ex);
                            Logger.getLogger(DocumentGenerator.class.getName()).log(Level.INFO, String.format("The description (ID=%s) has some HTML validity issues : {\"%s\"}",rule.getId(),rule.getHtmlDesc()), ex);
                        }
                    }
                    try {  
                        doc.addAltChunk(AltChunkType.Html.Html, new HTMLRewriter(rule.getHtmlDesc(), formater).result().getBytes());
                    } catch (UnableToTransform ex) {
                        // This is a serious error, but we will not exit at this point as we want to check if there are further issues pending
                        Logger.getLogger(DocumentGenerator.class.getName()).log(Level.SEVERE, String.format("The rule's description (ID=%s) was not transformed",rule.getId()), ex);
                        Logger.getLogger(DocumentGenerator.class.getName()).log(Level.INFO, String.format("The rule's description (ID=%s) faulty content is {\"%s\"}",rule.getId(),rule.getHtmlDesc()), ex);                        
                    }
                }                  
                
                /*if(validExample!=null && !"".equals(validExample.trim())){
                    // Create the valid example block
                    final String validExampleStyled = formater.highlightCode(rule.getValidExample());
                    doc.addStyledParagraphOfText("Caption", "Exemple");
                    doc.addAltChunk(AltChunkType.Html.Html, validExampleStyled.getBytes());                    
                }
                
                if(invalidExample!=null && !"".equals(invalidExample.trim())){
                    // Create the invalid example block if there is one provided
                    final String invalidExampleStyled = formater.highlightCode(rule.getInvalidExample());
                    doc.addStyledParagraphOfText("Caption", "Contre-Exemple");
                    doc.addAltChunk(AltChunkType.Html, invalidExampleStyled.getBytes());
                }*/
                
            }

    }
    
    public void generateDocument() throws Exception{
        try(FileInputStream inputStream = new FileInputStream(inputFileName)){
            // If the alias parameters is empty, we simply do not use aliases for names
            final Map<String,String> aliasMap = aliases.size()>0 ? aliases : null;
            
            // Create the rule's list
            final List<Rule> rules = BeansBuilder.createData(inputStream, sheetName, Rule.class, aliasMap);
            
            Logger.getLogger(DocumentGenerator.class.getName()).log(Level.INFO, String.format("There was %d rules loaded\n", rules!=null?rules.size():0));                                    
            
            // Create a formater with the lexer's name
            CodeFormater formater = HTMLRewriter.createCodeFormater(lexerName);
            
            // Create a new docx
            final WordprocessingMLPackage wordMLPackage = WordprocessingMLPackage.createPackage();            
            final MainDocumentPart doc = wordMLPackage.getMainDocumentPart();
            
            final TocGenerator tocGenerator = new TocGenerator(wordMLPackage);
        
            tocGenerator.generateToc(0, "TOC \\o \"1-3\" \\h \\z \\u ", true);
            
            doc.addParagraphOfText("Rules auto-generated");
            
            if(group){
                // If we need to group let's first build the tree
                // We will need to clean the list (aka remove null category), as a stream refuses to map to null as key
                final Map<String,List<Rule>> rulesTree = BeanTreeBuilder.group(BeanTreeBuilder.clean(rules,"Basic"));
                
                for(Map.Entry<String,List<Rule>> e : rulesTree.entrySet()){
                
                    // Create a level 1 paragraph title
                    generateParagrapth(1, e.getKey(), doc);
                
                    // Embed the list as level 2
                    generateRuleList(2, e.getValue(), doc, formater);
                }
            }else{
                // If we don't need the group display all the rules as a single level
                generateRuleList(1, rules, doc, formater);
            }
            
            // Update the ToC
            tocGenerator.updateToc(true);
            
            // Save the file
            wordMLPackage.save(new File(outputFileName));
            //wordMLPackage.save(new java.io.File(System.getProperty("user.dir") + "/test.docx"));
            
        }
    }
    
    public static void main(String[] args) throws Exception{
        final DocumentGenerator generator = new DocumentGenerator();
        final JCommander commander = new JCommander(generator,args);

        // Display the help when required and exit
        if(generator.isHelp()){
            commander.usage();
            System.exit(USAGE_EXIT_CODE);
        }
        
        // Change the level of the root handlers to INFO if verbose was set
        if(generator.isVerbose()){
            final Handler[] handlers = Logger.getLogger( "" ).getHandlers();
            for (Handler handler : handlers) {
                handler.setLevel(Level.INFO);
                Logger.getLogger(DocumentGenerator.class.getName()).log(Level.INFO, String.format("Handler %s was set to verbose (INFO)", handler));
            }
            
        }
        
        // Perform the document generation task
        generator.generateDocument();
    }
    
}
