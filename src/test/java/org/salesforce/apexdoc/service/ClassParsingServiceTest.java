package org.salesforce.apexdoc.service;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.salesforce.apexdoc.ApexDoc;
import org.salesforce.apexdoc.model.ClassModel;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.*;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.Arrays;

/**
 * Created by Bob on 3/15/2015.
 */
public class ClassParsingServiceTest {

    @Test
    public void testParseFileContents() throws IOException, NoSuchFieldException, IllegalAccessException {
        Field scope =  ApexDoc.class.getDeclaredField("rgstrScope");
        scope.setAccessible(true);
        scope.set(null, Arrays.asList("public", "private").toArray());

        URL url = Thread.currentThread().getContextClassLoader().getResource("test.cls");
        FileInputStream fstream = new FileInputStream(url.getPath());
        DataInputStream in = new DataInputStream(fstream);
        BufferedReader br = new BufferedReader(new InputStreamReader(in));

        ClassParsingService service = new ClassParsingService();
        ClassModel parsedClass = service.parseFileContents(br, false);
        in.close();

        Assert.assertEquals(parsedClass.getChildClassesSorted().size(), 1);
        Assert.assertEquals(parsedClass.getMethods().size(), 1);
        Assert.assertEquals(parsedClass.getProperties().size(), 2);
    }
}
