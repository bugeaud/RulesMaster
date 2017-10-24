/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.bugeaud.generator.rules.validator;

import fr.bugeaud.generator.rules.HTMLValidator;
import fr.bugeaud.generator.rules.InvalidHTML;
import static org.testng.Assert.*;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 *
 * @author J
 */
public class BasicHTMLValidatorTest {
    
    public BasicHTMLValidatorTest() {
    }

/**
 * Ensure that the HTML Validator component is working on basic valid HTML5
 * @throws InvalidHTML if HTML5 validation fails
 */
    @Test
    public void coreHTMLValidator() throws InvalidHTML{
       HTMLValidator.validateHtml("<!DOCTYPE html><html><head><title>Hello!</title></head><body><p>Hello,world!</p></body></html>");
    }

    @BeforeClass
    public static void setUpClass() throws Exception {

    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @BeforeMethod
    public void setUpMethod() throws Exception {
    }

    @AfterMethod
    public void tearDownMethod() throws Exception {
    }
}
