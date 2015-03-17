package org.salesforce.apexdoc.service;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.salesforce.apexdoc.ApexDoc;
import org.salesforce.apexdoc.model.ApexModel;
import org.salesforce.apexdoc.model.ClassModel;
import org.salesforce.apexdoc.model.MethodModel;
import org.salesforce.apexdoc.model.PropertyModel;

public class ClassParsingService {
	
	public ClassModel parseFileContents(BufferedReader br) {
        try {
            String strLine;
            boolean commentsStarted = false;
            boolean docBlockStarted = false;
            int nestedCurlyBraceDepth = 0;
            ArrayList<String> lstComments = new ArrayList<>();
            ClassModel cModel = null;
            ClassModel cModelParent = null;
            Stack<ClassModel> cModels = new Stack<>();

            // DH: Consider using java.io.StreamTokenizer to read the file a
            // token at a time?
            //
            // new strategy notes:
            // any line with " class " is a class definition
            // any line with scope (global, public, private) is a class, method,
            // or property definition.
            // you can detect a method vs. a property by the presence of ( )'s
            // you can also detect properties by get; or set;, though they may
            // not be on the first line.
            // in apex, methods that start with get and take no params, or set
            // with 1 param, are actually properties.
            //

            int iLine = 0;
            while ((strLine = br.readLine()) != null) {
                iLine++;

                strLine = strLine.trim();
                if (strLine.length() == 0)
                    continue;

                // ignore anything after // style comments. this allows hiding of tokens from ApexDoc.
                int ich = strLine.indexOf("//");
                if (ich > -1) {
                    strLine = strLine.substring(0, ich);
                }

                // gather up our comments
                if (strLine.startsWith("/*")) {
                    commentsStarted = true;
                    boolean commentEnded = false;
                    if(strLine.startsWith("/**")){
                    	if (strLine.endsWith("*/")) {
                            strLine = strLine.replace("*/", "");
                            commentEnded = true;
                    	}
                    	lstComments.add(strLine);
                    	docBlockStarted = true;
                    }
                    if (strLine.endsWith("*/") || commentEnded) {
                        commentsStarted = false;
                        docBlockStarted = false;
                    }
                    continue;
                }

                if (commentsStarted && strLine.endsWith("*/")) {
                    strLine = strLine.replace("*/", "");
                    if(docBlockStarted){
                    	lstComments.add(strLine);
                    	docBlockStarted = false;
                    }
                    commentsStarted = false;
                    continue;
                }

                if (commentsStarted) {
                	if(docBlockStarted){
                		lstComments.add(strLine);
                	}
                    continue;
                }

                // keep track of our nesting so we know which class we are in
                int openCurlies = StringUtils.countMatches(strLine, "{");
                int closeCurlies = StringUtils.countMatches(strLine, "}");
                nestedCurlyBraceDepth += openCurlies;
                nestedCurlyBraceDepth -= closeCurlies;

                // if we are in a nested class, and we just got back to nesting level 1,
                // then we are done with the nested class, and should set its props and methods.
                if (nestedCurlyBraceDepth == 1 && openCurlies != closeCurlies && cModels.size() > 1 && cModel != null) {
                    cModels.pop();
                    cModel = cModels.peek();
                    continue;
                }

                // ignore anything after an =. this avoids confusing properties with methods.
                ich = strLine.indexOf("=");
                if (ich > -1) {
                    strLine = strLine.substring(0, ich);
                }

                // ignore anything after an {. this avoids confusing properties with methods.
                ich = strLine.indexOf("{");
                if (ich > -1) {
                    strLine = strLine.substring(0, ich);
                }

                // ignore lines not dealing with scope
                if (ApexDoc.strContainsScope(strLine) == null &&
                        // interface methods don't have scope
                        !(cModel != null && cModel.getIsInterface() && strLine.contains("("))) {
                    continue;
                }

                // look for a class
                if ((strLine.toLowerCase().contains(" class ") || strLine.toLowerCase().contains(" interface "))) {

                    // create the new class
                    ClassModel cModelNew = new ClassModel(cModelParent);
                    fillApexModel(cModelNew, strLine, lstComments, iLine);
                    lstComments.clear();

                    // keep track of the new class, as long as it wasn't a single liner {}
                    // but handle not having any curlies on the class line!
                    if (openCurlies == 0 || openCurlies != closeCurlies) {
                        cModels.push(cModelNew);
                        cModel = cModelNew;
                    }

                    // add it to its parent (or track the parent)
                    if (cModelParent != null)
                        cModelParent.addChildClass(cModelNew);
                    else
                        cModelParent = cModelNew;
                    continue;
                }

                // look for a method
                if (strLine.contains("(")) {
                    // deal with a method over multiple lines.
                    while (!strLine.contains(")")) {
                        strLine += br.readLine();
                        iLine++;
                    }
                    MethodModel mModel = new MethodModel();
                    fillApexModel(mModel, strLine, lstComments, iLine);
                    cModel.getMethods().add(mModel);
                    lstComments.clear();
                    continue;
                }

                // handle set & get within the property
                if (strLine.contains(" get ") ||
                        strLine.contains(" set ") ||
                        strLine.contains(" get;") ||
                        strLine.contains(" set;") ||
                        strLine.contains(" get{") ||
                        strLine.contains(" set{"))
                    continue;

                // must be a property
                PropertyModel propertyModel = new PropertyModel();
                fillApexModel(propertyModel, strLine, lstComments, iLine);
                cModel.getProperties().add(propertyModel);
                lstComments.clear();

            }
            
            // we only want to return the parent class
            return cModelParent;
        } catch (Exception e) { // Catch exception if any
            System.err.println("Error: " + e.getMessage());
        }

        return null;
    }

    private void fillApexModel(ApexModel model, String name, ArrayList<String> lstComments, int iLine) {
    	model.setNameLine(name, iLine);
        Map<String, List<String>> tokenValues = tokenizeDocBlock(lstComments);
        model.mergeDocBlockData(tokenValues);
    }
    
    private Map<String, List<String>> tokenizeDocBlock(List<String> docBlock){
    	Map<String, List<String>> tokenValues = new HashMap<>();
        String lastToken = null;
        String lastTokenValue = null;
        Pattern p = Pattern.compile("(@[\\w]*)(.*)");
    	
        for (String comment : docBlock) {
        	if(comment.contains("/*") || comment.contains("*/")){
        		continue;
        	}
            comment = comment.trim().replaceAll("^\\s?\\*\\s?", "");
            
            Matcher m = p.matcher(comment);
        	if (m.find()) {
        		if(lastTokenValue != null){
        			tokenValues.get(lastToken).add(lastTokenValue);
        		}
        		
        		lastToken = m.group(1);
        		if(!tokenValues.containsKey(lastToken)){
        			tokenValues.put(lastToken, new ArrayList<String>());
        		}
        		lastTokenValue = m.group(2);
        	} else if(lastToken == null){
        		lastToken = "@description";
        		lastTokenValue = comment;
        		tokenValues.put(lastToken, new ArrayList<String>());
        	} else {
        		lastTokenValue += comment;
        	}
            
        }
        if(lastTokenValue != null){
			tokenValues.get(lastToken).add(lastTokenValue);
		}
        return tokenValues;
    }

}
