package org.salesforce.apexdoc.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
	
	@Test
	public void testMergeDocBlockData(){
		MethodModel model = new MethodModel();
		Map<String, List<String>> testData = new HashMap<String, List<String>>();
		testData.put("@description", Arrays.asList("Test description"));
		testData.put("@author", Arrays.asList("Test Person"));
		testData.put("@date", Arrays.asList("01/01/01"));
		testData.put("@return", Arrays.asList("Return value"));
		testData.put("@param", Arrays.asList("Param 1"));
		testData.put("@param", Arrays.asList("Param 2"));
		model.mergeDocBlockData(testData);

		Assert.assertEquals(model.getDescription(), "Test description");
		Assert.assertEquals(model.getAuthor(), "Test Person");
		Assert.assertEquals(model.getDate(), "01/01/01");
		Assert.assertEquals(model.getReturns(), "Return value");
		Assert.assertTrue(model.getParams().contains("Param 1"));
		Assert.assertTrue(model.getParams().contains("Param 2"));
		
	}

}
