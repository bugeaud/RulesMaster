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
import java.nio.charset.Charset;
import nu.validator.messages.MessageEmitter;
import nu.validator.messages.MessageEmitterAdapter;
import nu.validator.messages.TextMessageEmitter;
import nu.validator.servlet.imagereview.ImageCollector;
import nu.validator.source.SourceCode;
import nu.validator.validation.SimpleDocumentValidator;
import nu.validator.xml.SystemErrErrorHandler;
import org.xml.sax.InputSource;

/**
 *
 * @author J
 */
public class HTMLValidator {
    
    static final Charset UTF_8 = Charset.forName("UTF-8"); 
    
    /**
     * Verifies that a HTML content is valid.
     * @param htmlContent the HTML content
     * @throws InvalidHTML when trouble validading the given html or when the html is not valid
     */
    public static void validateHtml( String htmlContent ) throws InvalidHTML {

        InputStream in = new ByteArrayInputStream( htmlContent.getBytes(UTF_8));
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        SourceCode sourceCode = new SourceCode();
        //ImageCollector imageCollector = new ImageCollector(sourceCode);
        //boolean showSource = false;
        //MessageEmitter emitter = new TextMessageEmitter( out, false );
        //MessageEmitterAdapter errorHandler = new MessageEmitterAdapter( sourceCode, showSource, imageCollector, 0, false, emitter );
        //errorHandler.setErrorsOnly( true );
        InvalidHTML errorHandler = new InvalidHTML("There were errors gathered during HTML validation", null);
        
        try{
            SimpleDocumentValidator validator = new SimpleDocumentValidator();
            validator.setUpMainSchema( "http://s.validator.nu/html5-rdfalite.rnc", new SystemErrErrorHandler());
            validator.setUpValidatorAndParsers( errorHandler, true, false );
            validator.checkHtmlInputSource( new InputSource( in ));
        }catch(Exception ex){
            throw new InvalidHTML("There was a serious error while trying to parse the given description",ex);
        }
        // If there was some error log it and raise it as invalid HTML
        /*if(errorHandler.getErrors()>0){
            throw new InvalidHTML("The given description has various HTML validity issues : "+ new String(out.toByteArray()),null);
        }*/
        if(errorHandler.countAllError()>0){
            // Throw the handler as an exception
            throw errorHandler;
        }
    }
    
}
