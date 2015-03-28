package org.salesforce.apexdoc.service;

import org.testng.Assert;
import org.testng.annotations.Test;

public class FileManagerTest {

    @Test
    public void testParseHTMLFile(){
        FileManager fm = new FileManager(){
            @Override
            protected String parseFile(String filePath) {
                return "<html><body>Hello World</body></html>";
            }
        };

        Assert.assertEquals(fm.parseHTMLFile("anything"), "Hello World");
    }
}
