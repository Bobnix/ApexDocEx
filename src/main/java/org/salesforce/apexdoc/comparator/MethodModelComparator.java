package org.salesforce.apexdoc.comparator;

import java.util.Comparator;

import org.salesforce.apexdoc.model.MethodModel;

public class MethodModelComparator implements Comparator<MethodModel>{
	String className;
	
	public MethodModelComparator(String className){
		this.className = className;
	}
	
	@Override
    public int compare(MethodModel o1, MethodModel o2) {
        String methodName1 = o1.getMethodName();
        String methodName2 = o2.getMethodName();
        
        if(methodName1.equals(className)){
            return Integer.MIN_VALUE;
        } else if(methodName2.equals(className)){
            return Integer.MAX_VALUE;
        }
        return (methodName1.toLowerCase().compareTo(methodName2.toLowerCase()));
    }
}
