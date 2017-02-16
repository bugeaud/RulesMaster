/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.bugeaud.generator.rules;

import java.util.List;
import java.util.Vector;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * InvalidHTML is an exception that will be raised during validation when some error happens
 * @author bugeaud at gmail dot com
 * @licence CeCILL 2.1
 */
public class InvalidHTML extends Exception implements ErrorHandler{
    
    private List<SAXParseException> warnings = new Vector<>();
    private List<SAXParseException> errors = new Vector<>();
    private List<SAXParseException> fatalErrors = new Vector<>();
    
    public InvalidHTML(String reason, Exception root) {
        super(reason,root);
    }
    
    public int countAllError(){
        return errors.size()+fatalErrors.size();
    } 
    
    public int countWarning(){
        return warnings.size();
    }
    
    @Override
    public void warning(SAXParseException exception) throws SAXException {
        warnings.add(exception);
    }

    @Override
    public void error(SAXParseException exception) throws SAXException {
        errors.add(exception);
    }

    @Override
    public void fatalError(SAXParseException exception) throws SAXException {
        fatalErrors.add(exception);
    }

    @Override
    public String toString() {
        return "InvalidHTML(fatalErrors="+fatalErrors+"errors="+errors+",warnings="+warnings+"):"+super.toString();
    }
    
}
