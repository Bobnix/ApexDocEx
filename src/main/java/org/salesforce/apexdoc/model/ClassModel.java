package org.salesforce.apexdoc.model;

import java.util.List;
import java.util.ArrayList;
import java.util.TreeMap;
import java.util.Collections;

import org.salesforce.apexdoc.comparator.MethodModelComparator;

public class ClassModel extends ApexModel implements Comparable<ClassModel> {

    private List<MethodModel> methods;
    private List<PropertyModel> properties;
    private String strClassGroup;
    private String strClassGroupContent;
    private ClassModel cmodelParent;
    private List<ClassModel> childClasses;
    private boolean isInterface;

    public ClassModel(ClassModel cmodelParent) {
        methods = new ArrayList<MethodModel>();
        properties = new ArrayList<PropertyModel>();
        this.cmodelParent = cmodelParent;
        childClasses = new ArrayList<ClassModel>();
    }

    public List<PropertyModel> getProperties() {
        return properties;
    }

    public List<PropertyModel> getPropertiesSorted() {
        List<PropertyModel> sorted = new ArrayList<PropertyModel>(properties);
        Collections.sort(sorted);
        return sorted;
    }

    public void setProperties(List<PropertyModel> properties) {
        this.properties = properties;
    }

    public List<MethodModel> getMethods() {
        return methods;
    }

    public ArrayList<MethodModel> getMethodsSorted() {
        List<MethodModel> sorted = new ArrayList<MethodModel>(methods);
        Collections.sort(sorted, new MethodModelComparator(getClassName()));
        return new ArrayList<MethodModel>(sorted);
    }

    public void setMethods(ArrayList<MethodModel> methods) {
        this.methods = methods;
    }

    public ArrayList<ClassModel> getChildClassesSorted() {
        TreeMap<String, ClassModel> tm = new TreeMap<String, ClassModel>();
        for (ClassModel cm : childClasses)
            tm.put(cm.getClassName().toLowerCase(), cm);
        return new ArrayList<ClassModel>(tm.values());
    }

    public void addChildClass(ClassModel child) {
        childClasses.add(child);
    }

    public String getClassName() {
        String nameLine = getNameLine();
        String strParent = cmodelParent == null ? "" : cmodelParent.getClassName() + ".";
        if (nameLine != null)
            nameLine = nameLine.trim();
        if (nameLine != null && nameLine.trim().length() > 0) {
            int fFound = nameLine.toLowerCase().indexOf("class ");
            int cch = 6;
            if (fFound == -1) {
                fFound = nameLine.toLowerCase().indexOf("interface ");
                cch = 10;
            }
            if (fFound > -1)
                nameLine = nameLine.substring(fFound + cch).trim();
            int lFound = nameLine.indexOf(" ");
            if (lFound == -1)
                return strParent + nameLine;
            try {
                String name = nameLine.substring(0, lFound);
                return strParent + name;
            } catch (Exception ex) {
                return strParent + nameLine.substring(nameLine.lastIndexOf(" ") + 1);
            }
        } else {
            return "";
        }

    }

    public String getTopmostClassName() {
        if (cmodelParent != null)
            return cmodelParent.getClassName();
        else
            return getClassName();
    }

    public String getClassGroup() {
        if (this.cmodelParent != null)
            return cmodelParent.getClassGroup();
        else
            return strClassGroup;
    }

    public void setClassGroup(String strGroup) {
        strClassGroup = strGroup;
    }

    public String getClassGroupContent() {
        return strClassGroupContent;
    }

    public void setClassGroupContent(String strGroupContent) {
        strClassGroupContent = strGroupContent;
    }

    public boolean getIsInterface() {
        return isInterface;
    }

    public void setIsInterface(boolean isInterface) {
        this.isInterface = isInterface;
    }

	@Override
	public int compareTo(ClassModel otherModel) {
		return (this.getClassName().toLowerCase().compareTo(otherModel.getClassName().toLowerCase()));
	}
}
