package org.salesforce.apexdoc.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MethodModel extends ApexModel {
	
	private List<String> params;
    private String returnType;
    private List<String> exceptions;

    public MethodModel() {
        params = new ArrayList<>();
        exceptions = new ArrayList<>();
    }

    public void setNameLine(String nameLine, int iLine) {
        // remove anything after the parameter list
        if (nameLine != null) {
            int i = nameLine.lastIndexOf(")");
            if (i >= 0){
                nameLine = nameLine.substring(0, i + 1);
            }
        }
        super.setNameLine(nameLine, iLine);
    }

    public List<String> getParams() {
        return params;
    }

    public void setParams(List<String> params) {
        this.params = params;
    }

    public List<String> getExceptions() {
        return exceptions;
    }

    public void setExceptions(List<String> exceptions) {
        this.exceptions = exceptions;
    }

    public List<List<String>> getSplitParams() {
        return getSplitValues(params);
    }

    public List<List<String>> getSplitExceptions() {
        return getSplitValues(exceptions);
    }

    private List<List<String>> getSplitValues(List<String> valuesToSplit){
        List<List<String>> splitValues = new ArrayList<List<String>>();
        for (String value : valuesToSplit) {

            if (value != null && (value = value.trim()).length() > 0) {
                Pattern p = Pattern.compile("\\s");
                Matcher m = p.matcher(value);

                String valueName;
                String valueDescription;
                if (m.find()) {
                    int ich = m.start();
                    valueName = value.substring(0, ich);
                    valueDescription = value.substring(ich + 1).trim();
                } else {
                    valueName = value;
                    valueDescription = null;
                }
                splitValues.add(Arrays.asList(valueName, valueDescription));
            }
        }
        return splitValues;
    }

    public String getReturnType() {
        return returnType;
    }

    public void setReturnType(String returnType) {
        this.returnType = returnType;
    }

    public String getMethodName() {
        String nameLine = getNameLine().trim();
        if (nameLine.length() > 0) {
        	Pattern p = Pattern.compile("([\\d\\w_]*)[\\s]?\\(");
        	Matcher m = p.matcher(nameLine);
        	if (m.find()) {
        		return m.group(1);
        	}
        }
        return "";
    }

	@Override
	public void mergeDocBlockData(Map<String, List<String>> data) {
		if(data.containsKey("@description")){
        	setDescription(data.get("@description").get(0));
        }
        if(data.containsKey("@author")){
        	setAuthor(data.get("@author").get(0));
        }
        if(data.containsKey("@date")){
        	setDate(data.get("@date").get(0));
        }
        if(data.containsKey("@return")){
        	setReturns(data.get("@return").get(0));
        }
        if(data.containsKey("@param")){
            setParams(data.get("@param"));
        }
        if(data.containsKey("@throws")){
            setExceptions(data.get("@throws"));
        }
	}
}
