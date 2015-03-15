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

    public MethodModel() {
        params = new ArrayList<String>();
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
    
    public List<List<String>> getSplitParams() {
    	List<List<String>> splitParams = new ArrayList<List<String>>();
    	for (String param : params) {
    		
            if (param != null && param.trim().length() > 0) {
                Pattern p = Pattern.compile("\\s");
                Matcher m = p.matcher(param);
                
                String paramName;
                String paramDescription;
                if (m.find()) {
                	int ich = m.start();
                    paramName = param.substring(0, ich);
                    paramDescription = param.substring(ich + 1).trim();
                } else {
                    paramName = param;
                    paramDescription = null;
                }
                splitParams.add(Arrays.asList(paramName, paramDescription));
            }
        }
    	return splitParams;
    }

    public String getReturnType() {
        return returnType;
    }

    public void setReturnType(String returnType) {
        this.returnType = returnType;
    }
    public String getMethodName() {
        String nameLine = getNameLine().trim();
        if (nameLine != null && nameLine.length() > 0) {
        	Pattern p = Pattern.compile("([\\d\\w_]*)[\\s]?\\(");
        	Matcher m = p.matcher(nameLine);
        	if (m.find()) {
        		String val = m.group(1);
        		return val;
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
	}
}
