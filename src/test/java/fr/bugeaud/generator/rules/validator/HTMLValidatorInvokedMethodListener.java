/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.bugeaud.generator.rules.validator;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.testng.IInvokedMethod;
import org.testng.IInvokedMethodListener;
import org.testng.ITestResult;

public class HTMLValidatorInvokedMethodListener implements IInvokedMethodListener {
    @Override
    public void beforeInvocation(IInvokedMethod method, ITestResult testResult) {
    }

    @Override
    public void afterInvocation(IInvokedMethod method, ITestResult result) {
        if (method.isTestMethod() && ITestResult.FAILURE == result.getStatus()) {
            Throwable throwable = result.getThrowable();
            String originalMessage = throwable.getMessage();
            String newMessage = originalMessage + "\nReproduction Seed: ...\nCountry: ...";
            try {
                FieldUtils.writeField(throwable, "detailMessage", newMessage, true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
