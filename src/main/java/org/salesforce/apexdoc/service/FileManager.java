package org.salesforce.apexdoc.service;

import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.velocity.VelocityContext;
import org.salesforce.apexdoc.model.ClassGroup;
import org.salesforce.apexdoc.model.ClassModel;

public class FileManager {
	    
    private static final String ROOT_DIRECTORY = "ApexDocumentation";
    private static final String DEFAULT_HOME_CONTENTS = "<h1>Project Home</h1>";
	
    private String path;
    private StringBuffer infoMessages;
    private String[] rgstrScope;
    private TemplateService templateService;

	public FileManager() {
        infoMessages = new StringBuffer();
    }

    private String escapeHTML(String s) {
        StringBuilder out = new StringBuilder(Math.max(16, s.length()));
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c > 127 || c == '"' || c == '<' || c == '>' || c == '&') {
                out.append("&#");
                out.append((int) c);
                out.append(';');
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }

    public FileManager(String path, String[] scope) {
        infoMessages = new StringBuffer();

        if (path == null || path.trim().length() == 0){
            this.path = ".";
        } else {
            this.path = path;
        }
        
        rgstrScope = scope;
    }

    private boolean createHTML(TreeMap<String, String> mapFNameToContent) {
        try {
            if (path.endsWith("/") || path.endsWith("\\")) {
                path += ROOT_DIRECTORY; 
            } else {
                path += File.separator + ROOT_DIRECTORY; 
            }

            (new File(path)).mkdirs();

            for (String fileName : mapFNameToContent.keySet()) {
                String contents = mapFNameToContent.get(fileName);
                fileName = path + File.separator + fileName + ".html";
                File file = new File(fileName);
                FileWriter writer = new FileWriter(file);
                writer.write(contents);
                writer.close();
                infoMessages.append(fileName).append(" Processed...\n");
                System.out.println(fileName + " Processed...");
            }
            copy(path);
            return true;
        } catch (Exception e) {

            e.printStackTrace();
        }

        return false;
    }

    private String strHTMLScopingPanel() {
        String str = "<tr><td colspan='2' style='text-align: center;' >";
        str += "Show: ";

        for (String scope : rgstrScope) {
            str += "<input type='checkbox' checked='checked' id='cbx" + scope +
                    "' onclick='ToggleScope(\"" + scope + "\", this.checked );'>" +
                    scope + "</input>&nbsp;&nbsp;";
        }
        str += "</td></tr>";
        return str;
    }

    /********************************************************************************************
     * main routine that creates an HTML file for each class specified
     * @param mapGroupNameToClassGroup
     * @param cModels
     * @param projectDetail
     * @param homeContents
     * @param hostedSourceURL
     */
    private void makeFile(TreeMap<String, ClassGroup> mapGroupNameToClassGroup, ArrayList<ClassModel> cModels,
            String projectDetail, String homeContents, String hostedSourceURL) {
        String links = "<table width='100%'>";
        links += strHTMLScopingPanel();
        links += "<tr style='vertical-align:top;' >";
        links += getPageLinks(mapGroupNameToClassGroup, cModels);

        if (homeContents != null && homeContents.trim().length() > 0) {
            homeContents = links + "<td class='contentTD'>" + "<h2 class='section-title'>Home</h2>" + homeContents + "</td>";
            homeContents = getPageWrapper(projectDetail, homeContents);
        } else {
            homeContents = DEFAULT_HOME_CONTENTS;
            homeContents = links + "<td class='contentTD'>" + "<h2 class='section-title'>Home</h2>" + homeContents + "</td>";
            homeContents = getPageWrapper(projectDetail, homeContents);
        }

        String fileName = "";
        TreeMap<String, String> mapFNameToContent = new TreeMap<>();
        mapFNameToContent.put("index", homeContents);

        // create our Class Group content files
        createClassGroupContent(mapFNameToContent, links, projectDetail, mapGroupNameToClassGroup);

        for (ClassModel cModel : cModels) {
            String contents = links;
            if (cModel.getNameLine() != null && cModel.getNameLine().length() > 0) {
                fileName = cModel.getClassName();
                contents += "<td class='contentTD'>";

                contents += htmlForClassModel(cModel, hostedSourceURL);

                // deal with any nested classes
                for (ClassModel cmChild : cModel.getChildClassesSorted()) {
                    contents += "<p/>";
                    contents += htmlForClassModel(cmChild, hostedSourceURL);
                }

            } else {
                continue;
            }
            contents += "</div>";

            contents = getPageWrapper(projectDetail, contents);
            mapFNameToContent.put(fileName, contents);
        }
        createHTML(mapFNameToContent);
    }

    /*********************************************************************************************
     * Creates the HTML for the provided class, including its property and methods
     * @param cModel
     * @param hostedSourceURL
     * @return html string
     */
    private String htmlForClassModel(ClassModel cModel, String hostedSourceURL) {
 
        // Create a context and add data to the template placeholder
        VelocityContext context = new VelocityContext();
        context.put("class", cModel);
        context.put("hostedSourceURL", hostedSourceURL);
 
        return getTemplateService().createClassPage(context);
    }

    // create our Class Group content files
    private void createClassGroupContent(TreeMap<String, String> mapFNameToContent, String links, String projectDetail,
            TreeMap<String, ClassGroup> mapGroupNameToClassGroup) {

        for (String strGroup : mapGroupNameToClassGroup.keySet()) {
            ClassGroup cg = mapGroupNameToClassGroup.get(strGroup);
            if (cg.getContentSource() != null) {
                String cgContent = parseHTMLFile(cg.getContentSource());
                if ("".equals(cgContent)) {
                    String strHtml = getPageWrapper(projectDetail, links + "<td class='contentTD'>" +
                            "<h2 class='section-title'>" +
                            escapeHTML(cg.getName()) + "</h2>" + cgContent + "</td>");
                    mapFNameToContent.put(cg.getContentFilename(), strHtml);
                }
            }
        }
    }

    /**********************************************************************************************************
     * Generate the HTML string for the Class Menu to display on each page.
     * @param mapGroupNameToClassGroup
     *            map that holds all the Class names, and their respective Class
     *            Group.
     * @param cModels
     *            list of ClassModels
     * @return String of HTML
     */
    private String getPageLinks(TreeMap<String, ClassGroup> mapGroupNameToClassGroup, ArrayList<ClassModel> cModels) {
        
        Collections.sort(cModels);
        
        // add a bucket ClassGroup for all Classes without a ClassGroup specified
        mapGroupNameToClassGroup.put("Miscellaneous", new ClassGroup("Miscellaneous", null));
        
        Map<String, List<ClassModel>> classesByGroup = new HashMap<>();
        for (ClassModel cModel : cModels) {
        	if (cModel.getNameLine() != null && cModel.getNameLine().trim().length() > 0) { //TODO: Replace with isBlank
		    	String grp = cModel.getClassGroup() != null?cModel.getClassGroup():"Miscellaneous";
		    	if(!classesByGroup.containsKey(grp)){
		    		classesByGroup.put(grp, new ArrayList<ClassModel>());
		    	}
                classesByGroup.get(grp).add(cModel);
            }
        }
        
        // Create a context and add data to the template placeholder
        VelocityContext context = new VelocityContext();
        context.put("groups", mapGroupNameToClassGroup);
        context.put("classes", classesByGroup);
        
        return getTemplateService().createLinks(context);
    }

    private void doCopy(String source, String target) throws IOException{

        InputStream is = this.getClass().getClassLoader().getResourceAsStream("main/resource/"+source);
        FileOutputStream to = new FileOutputStream(target + "/" + source);

        byte[] buffer = new byte[4096];
        int bytesRead;

        while ((bytesRead = is.read(buffer)) != -1) {
            to.write(buffer, 0, bytesRead); // write
        }

        to.flush();
        to.close();
        is.close();
    }

    private void copy(String toFileName) throws IOException {
        doCopy("apex_doc_logo.png", toFileName);
        doCopy("ApexDoc.css", toFileName);
        doCopy("ApexDoc.js", toFileName);
        doCopy("CollapsibleList.js", toFileName);
        doCopy("jquery-latest.js", toFileName);
        doCopy("toggle_block_btm.gif", toFileName);
        doCopy("toggle_block_stretch.gif", toFileName);

    }

    public ArrayList<File> getFiles(String path) {
        File folder = new File(path);
        ArrayList<File> listOfFilesToCopy = new ArrayList<>();
        File[] listOfFiles = folder.listFiles();
        if (listOfFiles != null && listOfFiles.length > 0) {
            for (File file : listOfFiles) {
                if (file.isFile()) {
                    listOfFilesToCopy.add(file);
                }
            }
        }
        return listOfFilesToCopy;
    }

    public void createDoc(TreeMap<String, ClassGroup> mapGroupNameToClassGroup, ArrayList<ClassModel> cModels,
            String projectDetail, String homeContents, String hostedSourceURL) {
        makeFile(mapGroupNameToClassGroup, cModels, projectDetail, homeContents, hostedSourceURL);
    }

    private String parseFile(String filePath) {
        try {
            if (filePath != null && filePath.trim().length() > 0) {
                return new String(Files.readAllBytes(new File(filePath).toPath()));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return "";
    }

    public String parseHTMLFile(String filePath) {

        String contents = (parseFile(filePath)).trim();
        if (contents.length() > 0) {
            int startIndex = contents.indexOf("<body>");
            int endIndex = contents.indexOf("</body>");
            if (startIndex != -1) {
                if (endIndex != -1) {
                    contents = contents.substring(startIndex, endIndex);
                    return contents;
                }
            }
        }
        return "";
    }
    
    private String getPageWrapper(String projectDetail, String content) {
 
        // Create a context and add data to the template placeholder
        VelocityContext context = new VelocityContext();
        context.put("projectDetail", projectDetail);
        context.put("content", content);
 
        return getTemplateService().createPageWrapper(context);
    }
    
    private TemplateService getTemplateService() {
    	if(templateService == null){
    		templateService = new TemplateService();
    	}
		return templateService;
	}

}