package org.salesforce.apexdoc.model;

import java.util.*;

import org.testng.Assert;
import org.testng.annotations.Test;

public class ClassModelTest {

    @Test
    public void testSetNameLine(){
        ClassModel classModel = new ClassModel(null);
        classModel.setNameLine("public interface Selector{", 1);

        Assert.assertTrue(classModel.getIsInterface());
    }

    @Test
    public void testGetPropertiesSorted(){
        List<PropertyModel> models = new ArrayList<PropertyModel>();
        PropertyModel temp = new PropertyModel();
        temp.setNameLine("public String prop", 1);
        models.add(temp);
        temp = new PropertyModel();
        temp.setNameLine("public String var", 1);
        models.add(temp);
        temp = new PropertyModel();
        temp.setNameLine("public static final String const", 1);
        models.add(temp);

        ClassModel classModel = new ClassModel(null);
        classModel.setProperties(models);
        List<PropertyModel> retval = classModel.getPropertiesSorted();

        Assert.assertEquals(retval.get(0).getPropertyName(), "const");
        Assert.assertEquals(retval.get(1).getPropertyName(), "prop");
        Assert.assertEquals(retval.get(2).getPropertyName(), "var");
    }

    @Test
    public void testGetMethodsSorted(){
        List<MethodModel> models = new ArrayList<MethodModel>();
        MethodModel temp = new MethodModel();
        temp.setNameLine("public String increaseCounter(){", 1);
        models.add(temp);
        temp = new MethodModel();
        temp.setNameLine("public String decreaseCounter(){", 1);
        models.add(temp);
        temp = new MethodModel();
        temp.setNameLine("public FooCounter(){", 1);
        models.add(temp);

        ClassModel classModel = new ClassModel(null);
        classModel.setNameLine("public class FooCounter {", 1);
        classModel.setMethods(models);
        List<MethodModel> retval = classModel.getMethodsSorted();

        Assert.assertEquals(retval.get(0).getMethodName(), "FooCounter");
        Assert.assertEquals(retval.get(1).getMethodName(), "decreaseCounter");
        Assert.assertEquals(retval.get(2).getMethodName(), "increaseCounter");
    }

    @Test
    public void testMergeDocBlockData(){
        ClassModel model = new ClassModel(null);
        Map<String, List<String>> testData = new HashMap<String, List<String>>();
        testData.put("@description", Arrays.asList("Test description"));
        testData.put("@author", Arrays.asList("Test Person"));
        testData.put("@date", Arrays.asList("01/01/01"));
        testData.put("@group", Arrays.asList("Group 1"));
        testData.put("@group-content", Arrays.asList("Group Content 1"));
        model.mergeDocBlockData(testData);

        Assert.assertEquals(model.getDescription(), "Test description");
        Assert.assertEquals(model.getAuthor(), "Test Person");
        Assert.assertEquals(model.getDate(), "01/01/01");
        Assert.assertEquals(model.getClassGroup(), "Group 1");
        Assert.assertEquals(model.getClassGroupContent(), "Group Content 1");
    }
	
}
