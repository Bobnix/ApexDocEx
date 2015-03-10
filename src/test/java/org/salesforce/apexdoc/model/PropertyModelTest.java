package org.salesforce.apexdoc.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.Test;

public class PropertyModelTest {

	@Test
	public void testGetNameLineProperty(){
		PropertyModel model = new PropertyModel();
		model.setNameLine("public String prop{get; set;}", 1);
		Assert.assertEquals(model.getNameLine(), "public String prop");
	}
	
	@Test
	public void testGetNameLineVar(){
		PropertyModel model = new PropertyModel();
		model.setNameLine("public String var;", 1);
		Assert.assertEquals(model.getNameLine(), "public String var");
	}
	
	@Test
	public void testGetNameLineVarWithValue(){
		PropertyModel model = new PropertyModel();
		model.setNameLine("public String var = '2';", 1);
		Assert.assertEquals(model.getNameLine(), "public String var");
	}
	
	@Test
	public void testGetPropertyName(){
		PropertyModel model = new PropertyModel();
		model.setNameLine("public String prop", 1);
		Assert.assertEquals(model.getPropertyName(), "prop");
	}
	
	@Test
	public void testGetPropertyNameInvalid(){
		PropertyModel model = new PropertyModel();
		model.setNameLine("prop", 1);
		Assert.assertEquals(model.getPropertyName(), "");
	}
	
	@Test
	public void testSort(){
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
		
		Collections.sort(models);

		Assert.assertEquals(models.get(0).getPropertyName(), "const");
		Assert.assertEquals(models.get(1).getPropertyName(), "prop");
		Assert.assertEquals(models.get(2).getPropertyName(), "var");
	}
	
}
