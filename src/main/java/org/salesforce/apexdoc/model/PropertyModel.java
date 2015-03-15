package org.salesforce.apexdoc.model;

import java.util.List;
import java.util.Map;


public class PropertyModel extends ApexModel implements Comparable<PropertyModel> {

    public void setNameLine(String nameLine, int iLine) {
        if (nameLine != null) {
            // remove any trailing stuff after property name. { =
            int i = nameLine.indexOf('{');
            if (i == -1){
                i = nameLine.indexOf('=');
            } if (i == -1){
                i = nameLine.indexOf(';');
            } if (i >= 0){
                nameLine = nameLine.substring(0, i);
            }

        }
        super.setNameLine(nameLine, iLine);
    }

    public String getPropertyName() {
        String nameLine = getNameLine().trim();
        if (nameLine != null && nameLine.length() > 0) {
            int lastindex = nameLine.lastIndexOf(" ");
            if (lastindex >= 0) {
                String propertyName = nameLine.substring(lastindex + 1);
                return propertyName;
            }
        }
        return "";
    }

	@Override
	public int compareTo(PropertyModel otherModel) {
		return (this.getPropertyName().toLowerCase().compareTo(otherModel.getPropertyName().toLowerCase()));
	}

	@Override
	public void mergeDocBlockData(Map<String, List<String>> data) {
		if(data.containsKey("@description")){
        	setDescription(data.get("@description").get(0));
        }
	}
}