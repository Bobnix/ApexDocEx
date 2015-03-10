package org.salesforce.apexdoc.model;

import java.util.ArrayList;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.Test;

public class ClassModelTest {

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
	
}
