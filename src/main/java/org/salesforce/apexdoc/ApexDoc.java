package org.salesforce.apexdoc;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.salesforce.apexdoc.model.ClassGroup;
import org.salesforce.apexdoc.model.ClassModel;
import org.salesforce.apexdoc.model.MethodModel;
import org.salesforce.apexdoc.model.PropertyModel;
import org.salesforce.apexdoc.service.FileManager;

public class ApexDoc {

    private static FileManager fm;
    private static String[] rgstrScope = {"global","public","webService"};

    // public entry point when called from the command line.
    public static void main(String[] args) {
        try {
        	ApexDoc prog = new ApexDoc();
        	prog.RunApexDoc(args);
        } catch (Exception ex) {
            ex.printStackTrace();
            System.out.println(ex.getMessage());
            printHelp();
            System.exit(-1);
        }
    }

    // public main routine which is used by both command line invocation and
    // Eclipse PlugIn invocation
    private void RunApexDoc(String[] args) {
        String sourceDirectory = "";
        String targetDirectory = "";
        String homefilepath = "";
        String authorfilepath = "";
        String hostedSourceURL = "";

        // parse command line parameters
        for (int i = 0; i < args.length; i++) {

            if (args[i] == null) {
                continue;
            } else if (args[i].equalsIgnoreCase("-s")) {
                sourceDirectory = args[++i];
            } else if (args[i].equalsIgnoreCase("-g")) {
                hostedSourceURL = args[++i];
            } else if (args[i].equalsIgnoreCase("-t")) {
                targetDirectory = args[++i];
            } else if (args[i].equalsIgnoreCase("-h")) {
                homefilepath = args[++i];
            } else if (args[i].equalsIgnoreCase("-a")) {
                authorfilepath = args[++i];
            } else if (args[i].equalsIgnoreCase("-p")) {
                String strScope = args[++i];
                rgstrScope = strScope.split(";");
            } else {
                printHelp();
                System.exit(-1);
            }
        }

        // find all the files to parse
        fm = new FileManager(targetDirectory, rgstrScope);
        ArrayList<File> files = fm.getFiles(sourceDirectory);
        ArrayList<ClassModel> cModels = new ArrayList<ClassModel>();

        // parse each file, creating a class model for it
        for (File fromFile : files) {
            String fromFileName = fromFile.getAbsolutePath();
            if (fromFileName.endsWith(".cls")) {
                ClassModel cModel = parseFileContents(fromFileName);
                if (cModel != null) {
                    cModels.add(cModel);
                }
            }

        }

        // create our Groups
        TreeMap<String, ClassGroup> mapGroupNameToClassGroup = createMapGroupNameToClassGroup(cModels, sourceDirectory);

        // load up optional specified file templates
        String projectDetail = fm.parseHTMLFile(authorfilepath);
        String homeContents = fm.parseHTMLFile(homefilepath);

        // create our set of HTML files
        fm.createDoc(mapGroupNameToClassGroup, cModels, projectDetail, homeContents, hostedSourceURL);

        // we are done!
        System.out.println("ApexDoc has completed!");
    }

    private static void printHelp() {
        System.out.println("ApexDoc - a tool for generating documentation from Salesforce Apex code class files.\n");
        System.out.println("    Invalid Arguments detected.  The correct syntax is:\n");
        System.out.println("apexdoc -s <source_directory> [-t <target_directory>] [-g <source_url>] [-h <homefile>] [-a <authorfile>] [-p <scope>]\n");
        System.out.println("<source_directory> - The folder location which contains your apex .cls classes");
        System.out.println("<target_directory> - Optional. Specifies your target folder where documentation will be generated.");
        System.out.println("<source_url> - Optional. Specifies a URL where the source is hosted (so ApexDoc can provide links to your source).");
        System.out.println("<homefile> - Optional. Specifies the html file that contains the contents for the home page\'s content area.");
        System.out.println("<authorfile> - Optional. Specifies the text file that contains project information for the documentation header.");
        System.out.println("<scope> - Optional. Semicolon seperated list of scopes to document.  Defaults to 'global;public'. ");
    }

    private TreeMap<String, ClassGroup> createMapGroupNameToClassGroup(ArrayList<ClassModel> cModels,
            String sourceDirectory) {
        TreeMap<String, ClassGroup> map = new TreeMap<String, ClassGroup>();
        for (ClassModel cmodel : cModels) {
            String strGroup = cmodel.getClassGroup();
            String strGroupContent = cmodel.getClassGroupContent();
            if (strGroupContent != null)
                strGroupContent = sourceDirectory + "/" + strGroupContent;
            ClassGroup cg;
            if (strGroup != null) {
                cg = map.get(strGroup);
                if (cg == null)
                    cg = new ClassGroup(strGroup, strGroupContent);
                else if (cg.getContentSource() == null)
                    cg.setContentSource(strGroupContent);
                // put the new or potentially modified ClassGroup back in the map
                map.put(strGroup, cg);
            }
        }
        return map;
    }

    private ClassModel parseFileContents(String filePath) {
        try {
            FileInputStream fstream = new FileInputStream(filePath);
            // Get the object of DataInputStream
            DataInputStream in = new DataInputStream(fstream);
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            String strLine;
            boolean commentsStarted = false;
            boolean docBlockStarted = false;
            int nestedCurlyBraceDepth = 0;
            ArrayList<String> lstComments = new ArrayList<String>();
            ClassModel cModel = null;
            ClassModel cModelParent = null;
            Stack<ClassModel> cModels = new Stack<ClassModel>();

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
                int openCurlies = countChars(strLine, '{');
                int closeCurlies = countChars(strLine, '}');
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
                if (strContainsScope(strLine) == null &&
                        // interface methods don't have scope
                        !(cModel != null && cModel.getIsInterface() && strLine.contains("("))) {
                    continue;
                }

                // look for a class
                if ((strLine.toLowerCase().contains(" class ") || strLine.toLowerCase().contains(" interface "))) {

                    // create the new class
                    ClassModel cModelNew = new ClassModel(cModelParent);
                    fillClassModel(cModelParent, cModelNew, strLine, lstComments, iLine);
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
                    fillMethodModel(mModel, strLine, lstComments, iLine);
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
                fillPropertyModel(propertyModel, strLine, lstComments, iLine);
                cModel.getProperties().add(propertyModel);
                lstComments.clear();
                continue;

            }

            // Close the input stream
            in.close();
            // we only want to return the parent class
            return cModelParent;
        } catch (Exception e) { // Catch exception if any
            System.err.println("Error: " + e.getMessage());
        }

        return null;
    }

    public static String strContainsScope(String str) {
        str = str.toLowerCase();
        for (int i = 0; i < rgstrScope.length; i++) {
            if (str.toLowerCase().contains(rgstrScope[i].toLowerCase() + " ")) {
                return rgstrScope[i];
            }
        }
        return null;
    }

    private void fillPropertyModel(PropertyModel propertyModel, String name, ArrayList<String> lstComments,
            int iLine) {
        propertyModel.setNameLine(name, iLine);
        Map<String, List<String>> tokenValues = tokenizeDocBlock(lstComments);

        if(tokenValues.containsKey("@description")){
        	propertyModel.setDescription(tokenValues.get("@description").get(0));
        }
    }
    
    private void fillMethodModel(MethodModel mModel, String name, ArrayList<String> lstComments, int iLine) {
        mModel.setNameLine(name, iLine);
        Map<String, List<String>> tokenValues = tokenizeDocBlock(lstComments);

        if(tokenValues.containsKey("@description")){
        	mModel.setDescription(tokenValues.get("@description").get(0));
        }
        if(tokenValues.containsKey("@author")){
        	mModel.setAuthor(tokenValues.get("@author").get(0));
        }
        if(tokenValues.containsKey("@date")){
        	mModel.setDate(tokenValues.get("@date").get(0));
        }
        if(tokenValues.containsKey("@return")){
        	mModel.setReturns(tokenValues.get("@return").get(0));
        }
        if(tokenValues.containsKey("@param")){
        	mModel.setParams(tokenValues.get("@param"));
        }
    }
    
    private Map<String, List<String>> tokenizeDocBlock(List<String> docBlock){
    	Map<String, List<String>> tokenValues = new HashMap<String, List<String>>();
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

    private void fillClassModel(ClassModel cModelParent, ClassModel cModel, String name,
            ArrayList<String> lstComments, int iLine) {
        cModel.setNameLine(name, iLine);
        if (name.toLowerCase().contains(" interface "))
            cModel.setIsInterface(true);
        
        Map<String, List<String>> tokenValues = tokenizeDocBlock(lstComments);

        if(tokenValues.containsKey("@description")){
        	cModel.setDescription(tokenValues.get("@description").get(0));
        }
        if(tokenValues.containsKey("@author")){
        	cModel.setAuthor(tokenValues.get("@author").get(0));
        }
        if(tokenValues.containsKey("@date")){
        	cModel.setDate(tokenValues.get("@date").get(0));
        }
        if(tokenValues.containsKey("@group")){
        	cModel.setClassGroup(tokenValues.get("@group").get(0));
        }
        if(tokenValues.containsKey("@group-content")){
        	cModel.setClassGroupContent(tokenValues.get("@group-content").get(0));
        }
    }

    /*************************************************************************
     * @description Count the number of occurrences of character in the string
     * @param str
     * @param ch
     * @return int
     */
    private int countChars(String str, char ch) {
        int count = 0;
        for (int i = 0; i < str.length(); ++i) {
            if (str.charAt(i) == ch) {
                ++count;
            }
        }
        return count;
    }

}