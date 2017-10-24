package fr.bugeaud.generator.rules;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.xml.bind.annotation.adapters.HexBinaryAdapter;
import javax.xml.xpath.XPath;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFHyperlink;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFStyle;
import org.apache.poi.xwpf.usermodel.XWPFStyles;
import org.apache.xmlbeans.XmlCursor;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.openpackaging.parts.WordprocessingML.AltChunkType;
import org.docx4j.openpackaging.parts.WordprocessingML.MainDocumentPart;
import org.docx4j.toc.TocGenerator;
import org.jsoup.Jsoup;
import org.jsoup.select.Elements;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTBookmark;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTColor;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTDecimalNumber;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTFonts;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTHpsMeasure;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTMarkupRange;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTOnOff;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTRPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTString;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTStyle;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STStyleType;

/**
 * 
 * @author bugeaud at gmail dot com
 * @licence CeCILL 2.1
 */
public class Test {
 
 
    public static final String RULE_SHEET_NAME = "Rules";
    public static final int HEADER_ROW_INDEX = 0;
    
    /**
     * Build a Map of PropertyDescriptor for a given class
     * @param klass the target class
     * @return a Map of PropertyDescriptor for a given class
     * @throws IntrospectionException when trying to introspect the
     */
    public static Map<String, PropertyDescriptor> getProperties(Class<?> klass) throws IntrospectionException{
        final PropertyDescriptor[] properties = Introspector.getBeanInfo(klass).getPropertyDescriptors();        
        return Stream.of(properties).collect(Collectors.toMap(PropertyDescriptor::getDisplayName, Function.identity()));
    }
    
    /**
     * Return the sheet table headers in the order written
     * @param sheet the sheet table
     * @return the list of column name given in the header
     */
    public static List<String> getSheetHeaders(XSSFSheet sheet){
        final List<String> headers = new ArrayList<String>();
        final XSSFRow headerRow = sheet.getRow(HEADER_ROW_INDEX);
        
        // Will load a formater according to the default locale
        final DataFormatter formater = new DataFormatter();
        short lastCell = headerRow.getLastCellNum();
        for(short i=0;i<lastCell;i++){
            XSSFCell cell = headerRow.getCell(i);
            if(cell!=null){
                // Add the cell value alond to its
                headers.add(Introspector.decapitalize(formater.formatCellValue(cell)));
            }            
        }
        return headers;
    }
    
    protected final static <T> T buildDataInstance(XSSFRow row, Class<T> theClass, List<String> headers, Map<String, PropertyDescriptor> propertiesCache)
            throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException{
        final T newInstance = theClass.newInstance();
        short lastCell = row.getLastCellNum();
        for(short i=0;i<lastCell;i++){
            final XSSFCell cell = row.getCell(i);
            final String value = cell.getStringCellValue();
            final String propertyName = headers.get(i);
            final PropertyDescriptor propertyDescriptor = propertiesCache.get(propertyName);
            if(propertyDescriptor==null){
                // If no descriptor was found, simply get out
                continue;
            }
            propertyDescriptor.getWriteMethod().invoke(newInstance, value);
        }

        return newInstance;
    } 
    
    
    public static <T> List<T> buildDataList(XSSFSheet sheet, Class<T> theClass) throws IntrospectionException, IllegalAccessException, InstantiationException, IllegalArgumentException, InvocationTargetException{
        final List<T> dataList = new ArrayList<>();
        final List<String> headers = getSheetHeaders(sheet);
        final Map<String, PropertyDescriptor> properties = getProperties(theClass);
        
        final Iterator<Row> it = sheet.iterator();
        
        // Assumes it has at least one row and as it is a header, drop it
        it.next();
        
        // Loop on rows to build bean instances
        while(it.hasNext()) {
            final Row r = it.next();
            final XSSFRow xr = (XSSFRow)r;
            T instance = buildDataInstance(xr,theClass,headers,properties);
            if(instance == null){
                // Let's skip
                continue;
            }
            dataList.add(instance);
        }

        return dataList;
    }
    
    public static void cloneParagraph(XWPFParagraph clone, XWPFParagraph source) {
        CTPPr pPr = clone.getCTP().isSetPPr() ? clone.getCTP().getPPr() : clone.getCTP().addNewPPr();
        pPr.set(source.getCTP().getPPr());
        for (XWPFRun r : source.getRuns()) {
            XWPFRun nr = clone.createRun();
            cloneRun(nr, r);
        }
    }

    public static void cloneRun(XWPFRun clone, XWPFRun source) {
        CTRPr rPr = clone.getCTR().isSetRPr() ? clone.getCTR().getRPr() : clone.getCTR().addNewRPr();
        rPr.set(source.getCTR().getRPr());
        clone.setText(source.getText(0));
    }
    
    /**
     * Return the paragraph from that contain the bookmark matching the given name
     * @param doc the target document
     * @param name the given bookmark name
     * @return the matching paragraph, null if none was found
     */
    public static XWPFParagraph getFirstBookmarkFromName(XWPFDocument doc, final String name){
            for (XWPFParagraph p : doc.getParagraphs()){
                for(CTBookmark bookmark : p.getCTP().getBookmarkStartArray()){
                    if(name.equals(bookmark.getName())){ 
                        return p;
                        //Node n = bookmark.getDomNode();
                        //System.err.printf("Bookmark: \n DOM: %s \n toString: %s\n", n, bookmark);

                        //Node node = bookmark.getDomNode().cloneNode(true);
                        //p.getCTP().getDomNode().insertBefore(newChild, refChild)
                    }
                }
            }

        return null;
    }
    
    public static void cloneBookmarkAtEnd(XWPFDocument doc, String bookmarkName ){
        
    }

    public static void main(String[] args) throws Exception{
        //HTMLRewriter rewriter = new HTMLRewriter("<p>Ceci est un test</p><code>test()</code><p>qui marche</p>", createCodeFormater("CppLexer",false));
        HTMLRewriter rewriter = new HTMLRewriter("Ceci est un test \n sans HTML du tout <code>#include &lt;stdio.h&gt;\n int main(int argc, char* argv[]){}</code> .\n blah blah<pre>#include \"test.h\"\n// Test de commentaire\nint main(int argc, char* argv[]){\nprintf(\"test\");\n}</pre>", createCodeFormater("CppLexer",true));
        System.out.println(rewriter.result());
    }
    
    public static void main7(String[] args) throws Exception{
        CodeFormater formater = createCodeFormater();
        String style = formater.highlightCode("");
        Elements elements = Jsoup.parse(style).select("style");
        System.out.println(elements.toString());
    }
    
    /*public static void main6(String[] args) throws Exception{
        try(FileInputStream inputStream = new FileInputStream("C:\\Users\\J\\Documents\\econocom\\Projets\\Dassault\\UsineLogicielle\\TestRegles.xlsm")){
            XSSFWorkbook workBook = new XSSFWorkbook(inputStream);
            XSSFSheet sheet = workBook.getSheet(RULE_SHEET_NAME);

            List<Rule> rules = buildDataList(sheet, Rule.class);
            
            System.out.printf("There was %d rules loaded\n", rules!=null?rules.size():0);
            
            CodeFormater formater = createCodeFormater();
            
            final WordprocessingMLPackage wordMLPackage = WordprocessingMLPackage.createPackage();            
            final MainDocumentPart doc = wordMLPackage.getMainDocumentPart();
            
            final TocGenerator tocGenerator = new TocGenerator(wordMLPackage);
        
            tocGenerator.generateToc(0, "TOC \\o \"1-3\" \\h \\z \\u ", true);
            
            doc.addParagraphOfText("Rules auto-generated");

            for(Rule rule : rules){
                String validExample = formater.highlightCode(rule.getValidExample());
                String invalidExample = formater.highlightCode(rule.getInvalidExample());
                //System.out.printf("Example:\n%s\n",);
                
                // Create the title
                doc.addStyledParagraphOfText("Heading1", String.format("%s - %s", rule.getId(),rule.getName()));
                
                // Create the description
                doc.addParagraphOfText(rule.getDescription());
                
                System.out.println(doc.getStyleTree().getParagraphStylesTree());
                
                if(validExample!=null && !"".equals(validExample.trim())){
                    // Create the valid example block
                    doc.addStyledParagraphOfText("Caption", "Valid example");
                    doc.addAltChunk(AltChunkType.Html.Html, validExample.getBytes());                    
                }
                
                if(invalidExample!=null && !"".equals(invalidExample.trim())){
                    // Create the invalid example block if there is one provided
                    doc.addStyledParagraphOfText("Caption", "Invalid Example");
                    doc.addAltChunk(AltChunkType.Html, invalidExample.getBytes());
                }
                
            }

            // Update the ToC
            tocGenerator.updateToc(true);
            
            wordMLPackage.save(new File("C:\\Users\\J\\Documents\\econocom\\Projets\\Dassault\\test-docx4j.docx"));
            //wordMLPackage.save(new java.io.File(System.getProperty("user.dir") + "/test.docx"));
            
        }
    }*/

    public static CodeFormater createCodeFormater()throws ScriptException{
        return createCodeFormater("CppLexer");
    }

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
        scriptEngine.eval("formater = HtmlFormatter(full=True)");
        
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
 
private static void addCustomHeadingStyle(XWPFDocument docxDocument, XWPFStyles styles, String strStyleId, int headingLevel, int pointSize, String hexColor, String font) {

    CTStyle ctStyle = CTStyle.Factory.newInstance();
    ctStyle.setStyleId(strStyleId);


    CTString styleName = CTString.Factory.newInstance();
    styleName.setVal(strStyleId);
    ctStyle.setName(styleName);
   
    CTOnOff onoffnull = CTOnOff.Factory.newInstance();
    ctStyle.setUnhideWhenUsed(onoffnull);

    // style shows up in the formats bar
    ctStyle.setQFormat(onoffnull);

    if(headingLevel>=0){
        CTDecimalNumber indentNumber = CTDecimalNumber.Factory.newInstance();
        indentNumber.setVal(BigInteger.valueOf(headingLevel));

        // lower number > style is more prominent in the formats bar
        ctStyle.setUiPriority(indentNumber);    

        // style defines a heading of the given level
        CTPPr ppr = CTPPr.Factory.newInstance();
        ppr.setOutlineLvl(indentNumber);
        ctStyle.setPPr(ppr);        
    }

    XWPFStyle style = new XWPFStyle(ctStyle);

    CTHpsMeasure size = CTHpsMeasure.Factory.newInstance();
    size.setVal(new BigInteger(String.valueOf(pointSize)));
    CTHpsMeasure size2 = CTHpsMeasure.Factory.newInstance();
    size2.setVal(new BigInteger("24"));

    CTFonts fonts = CTFonts.Factory.newInstance();
    fonts.setAscii(font);

    CTRPr rpr = CTRPr.Factory.newInstance();
    rpr.setRFonts(fonts);
    rpr.setSz(size);
    rpr.setSzCs(size2);

    CTColor color=CTColor.Factory.newInstance();
    color.setVal(hexToBytes(hexColor));
    rpr.setColor(color);
    style.getCTStyle().setRPr(rpr);
    // is a null op if already defined

    style.setType(STStyleType.PARAGRAPH);
    styles.addStyle(style);

}

public static byte[] hexToBytes(String hexString) {
     HexBinaryAdapter adapter = new HexBinaryAdapter();
     byte[] bytes = adapter.unmarshal(hexString);
     return bytes;
}
    



public static final String HEADING_1 = "Heading1";
public static final String HEADING_2 = "Heading2";
public static final String HEADING_3 = "Heading3";
public static final String HEADING_4 = "Heading4";
public static final String CODE = "Code";
public static final String NORMAL = "Normal";

    public static XWPFDocument buildRulesDocument(List<Rule> rules){
        XWPFDocument doc = new XWPFDocument();
        XWPFStyles styles = doc.createStyles();

        addCustomHeadingStyle(doc, styles, HEADING_1, 1, 36, "4288BC","Calibra");
        addCustomHeadingStyle(doc, styles, HEADING_2, 2, 28, "4288BC","Calibra");
        addCustomHeadingStyle(doc, styles, HEADING_3, 3, 24, "4288BC","Calibra");
        addCustomHeadingStyle(doc, styles, HEADING_4, 4, 20, "000000","Calibra");
        addCustomHeadingStyle(doc, styles, NORMAL, -1, 20, "000000","Calibra");
        addCustomHeadingStyle(doc, styles, CODE, -1, 20, "000000","Courier New");
        
        doc.createTOC();
        
        for(Rule rule:rules){
            addRuleBlock(doc, rule);
        }
        return doc;
    }
    
    protected static void addRuleBlock(XWPFDocument doc, Rule rule){
        
        XWPFParagraph heading = doc.createParagraph();
        heading.setStyle(HEADING_1);
        heading.createRun().setText(String.format("%s - %s", rule.getId(),rule.getName()));
                
        XWPFParagraph block = doc.createParagraph();
        block.setStyle(NORMAL);
        block.createRun().setText(rule.getHtmlDesc());
                
        /*
        @todo Improve the rule generation
        
        XWPFParagraph validExampleTitle = doc.createParagraph();
        validExampleTitle.setStyle(HEADING_2);
        validExampleTitle.createRun().setText("Exemple valide");
        
        XWPFParagraph validExample = doc.createParagraph();
        validExample.setStyle(CODE);
        validExample.createRun().setText(rule.getValidExample());
        
        XWPFParagraph invalidExampleTitle = doc.createParagraph();
        invalidExampleTitle.setStyle(HEADING_2);
        invalidExampleTitle.createRun().setText("Exemple invalide");
        
        XWPFParagraph invalidExample = doc.createParagraph();
        invalidExample.setStyle(CODE);
        invalidExample.createRun().setText(rule.getInvalidExample());*/
        
    }
    
    public static final List<Rule> buildTestRules(){
        final List<Rule> rules = new ArrayList<>();
        
        final Rule r1 = new Rule();
        r1.setName("Règle 1");
        r1.setHtmlDesc("Avoir un main standard.\n"
                + "Correct : <code>#include \"stdio.h\"\nint main(){}\n</code>"
                + "Incorrect:<code>#include \"stdio.h\"\nint main(int argv, char *argc[]){}\n</code>");
        rules.add(r1);
        
        final Rule r2 = new Rule();
        r2.setName("Règle 2");
        r2.setHtmlDesc("Utiliser printf"
                + "Correct : <code>#include \"stdio.h\"\nint main(){}\n</code>"
                + "Incorrect:<code>#include \"stdio.h\"\nint main(int argv, char *argc[]){\nprintf(\"HelloWorld\")}\n</code>"
        );
        rules.add(r2);
        
        return rules;
    }
    
    public static final void saveDocument(XWPFDocument doc)throws IOException{
         try(FileOutputStream out = new FileOutputStream(new File("C:\\Users\\J\\Documents\\Osiatis\\Projets\\Dassault\\test-generated.docx"))){
             doc.write(out);
         }
    }
    
    public static void main4(String[] args) throws Exception{
        List<Rule> rules = buildTestRules();
        XWPFDocument doc = buildRulesDocument(rules);
        saveDocument(doc);
    }
    
    public static void main5(String[] args) throws Exception{
        List<Rule> rules = buildTestRules();
        XWPFDocument doc = buildRulesDocument(rules);
        saveDocument(doc);
    }
    
    public static void main2(String[] args) throws Exception{
        //XSSFWorkbook wb = new XSSFWorkbook("");
        try(FileInputStream inputStream = new FileInputStream("C:\\Users\\J\\Documents\\Osiatis\\Projets\\Dassault\\Template-Rules-000.docx")){
            XWPFDocument doc = new XWPFDocument(inputStream);
            XWPFParagraph paragraphBegin = getFirstBookmarkFromName(doc,"REPEAT_LINE");
            //XWPFParagraph paragraphEnd = getLastBookmarkFromName(doc,"REPEAT_LINE");
            
            List<Rule> rules = buildTestRules();
            //XmlCursor cursor = p.getCTP().newCursor();
                        
            /*
            for(Rule rule : rules){
                XWPFParagraph newP = doc.createParagraph();
                cloneParagraph(newP, paragraphBegin);

            }*/
            saveDocument(doc);
            
            /*System.out.printf("%s %d\n",doc,doc.getHyperlinks().length);
            for (XWPFParagraph p : doc.getParagraphs()){
                for(CTBookmark bookmark : p.getCTP().getBookmarkStartArray()){
                    if("REPEAT_LINE".equals(bookmark.getName())){
                        Node n = bookmark.getDomNode();
                        System.err.printf("Bookmark: \n DOM: %s \n toString: %s\n", n, bookmark);

                        //Node node = bookmark.getDomNode().cloneNode(true);
                        //p.getCTP().getDomNode().insertBefore(newChild, refChild)
                    }
                }
            }*/
            
                    
                    /*
            for(XWPFHyperlink link : doc.getHyperlinks()){
                System.out.println(link);
            }*/
        }
        
        
                  
    }
}
