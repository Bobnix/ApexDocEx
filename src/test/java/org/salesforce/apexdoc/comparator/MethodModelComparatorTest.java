package org.salesforce.apexdoc.comparator;

import org.salesforce.apexdoc.model.ClassModel;
import org.salesforce.apexdoc.model.MethodModel;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MethodModelComparatorTest {

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
        temp = new MethodModel();
        temp.setNameLine("public String addCounter(){", 1);
        models.add(temp);

        Collections.sort(models, new MethodModelComparator("FooCounter"));

        Assert.assertEquals(models.get(0).getMethodName(), "FooCounter");
        Assert.assertEquals(models.get(1).getMethodName(), "addCounter");
        Assert.assertEquals(models.get(2).getMethodName(), "decreaseCounter");
        Assert.assertEquals(models.get(3).getMethodName(), "increaseCounter");
    }
}
