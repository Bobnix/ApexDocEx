package org.salesforce.apexdoc.model;

import java.util.ArrayList;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.Test;

public class MethodModelTest {
	
	@Test
	public void testConstructor(){
		MethodModel model = new MethodModel();
		Assert.assertNotNull(model.getParams());
	}
	
	@Test
	public void testGetMethodName(){
		MethodModel model = new MethodModel();
		model.setNameLine("public string methName(String arg1);", 1);
		Assert.assertEquals(model.getMethodName(), "methName");
	}
	
	@Test
	public void testGetMethodNameComplex(){
		MethodModel model = new MethodModel();
		model.setNameLine("public string meth_Name1 (String arg1);", 1);
		Assert.assertEquals(model.getMethodName(), "meth_Name1");
	}

	@Test
	public void testGetNameLine(){
		MethodModel model = new MethodModel();
		model.setNameLine("public string meth_Name1 (String arg1); //This is a method", 1);
		Assert.assertEquals(model.getNameLine(), "public string meth_Name1 (String arg1)");
	}
	
	@Test
	public void testSplitParameters(){
		List<String> testParam = new ArrayList<String>();
		testParam.add("param withSpace");
		testParam.add("param      withMultipleSpace");
		testParam.add("param\t withTab");
		
		MethodModel model = new MethodModel();
		model.setParams(testParam);
		
		List<List<String>> retVal = model.getSplitParams();

		Assert.assertEquals(retVal.get(0).get(0), "param");
		Assert.assertEquals(retVal.get(0).get(1), "withSpace");
		Assert.assertEquals(retVal.get(1).get(0), "param");
		Assert.assertEquals(retVal.get(1).get(1), "withMultipleSpace");
		Assert.assertEquals(retVal.get(2).get(0), "param");
		Assert.assertEquals(retVal.get(2).get(1), "withTab");
	}

}
