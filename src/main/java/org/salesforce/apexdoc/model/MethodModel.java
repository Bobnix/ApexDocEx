package org.salesforce.apexdoc.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.salesforce.apexdoc.ApexDoc;

public class MethodModel extends ApexModel {

    public MethodModel() {
        params = new ArrayList<String>();
    }

    public void setNameLine(String nameLine, int iLine) {
        // remove anything after the parameter list
        if (nameLine != null) {
            int i = nameLine.lastIndexOf(")");
            if (i >= 0)
                nameLine = nameLine.substring(0, i + 1);
        }
        super.setNameLine(nameLine, iLine);
    }

    public ArrayList<String> getParams() {
        return params;
    }

    public void setParams(ArrayList<String> params) {
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
                    paramDescription = param.substring(ich + 1);
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
            int lastindex = nameLine.indexOf("(");
            if (lastindex >= 0) {
                String methodName = ApexDoc.strPrevWord(nameLine, lastindex);
                return methodName;
            }
        }
        return "";
    }

    private ArrayList<String> params;
    private String returnType;
}
