package org.salesforce.apexdoc.service;

import java.io.*;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.*;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.velocity.VelocityContext;
import org.salesforce.apexdoc.model.ClassGroup;
import org.salesforce.apexdoc.model.ClassModel;

public class FileManager {
	    
    private static final String ROOT_DIRECTORY = "ApexDocumentation";
    private static final String DEFAULT_HOME_CONTENTS = "<h1>Project Home</h1>";

    private TemplateService templateService;

    private boolean createHTML(TreeMap<String, String> mapFNameToContent, String path) {
        if (StringUtils.isBlank(path)){
            path = ".";
        }

        try {
            if (path.endsWith("/") || path.endsWith("\\")) {
                path += ROOT_DIRECTORY; 
            } else {
                path += File.separator + ROOT_DIRECTORY; 
            }

            File directory = new File(path);
            FileUtils.deleteDirectory(directory);
            if(directory.mkdirs()) {

                for (String fileName : mapFNameToContent.keySet()) {
                    String contents = mapFNameToContent.get(fileName);
                    fileName = path + File.separator + fileName + ".html";
                    File file = new File(fileName);
                    FileWriter writer = new FileWriter(file);
                    writer.write(contents);
                    writer.close();
                    System.out.println(fileName + " Processed...");
                }
                copy(path);
                return true;
            }

        } catch (IOException | URISyntaxException e) {

            e.printStackTrace();
        }

        return false;
    }

    /**
     * Main routine that creates an HTML file for each class specified
     * @param mapGroupNameToClassGroup  The class groups
     * @param cModels   The class models to make html files for
     * @param projectDetail The text to use in the header
     * @param homeContents  The html to use inside the home page
     * @param rgstrScope    the scopes to process
     * @param path  the destination folder to put the files in
     */
    public void createDoc(TreeMap<String, ClassGroup> mapGroupNameToClassGroup, List<ClassModel> cModels,
            String projectDetail, String homeContents, String[] rgstrScope, String path) {
        String links = getPageLinks(mapGroupNameToClassGroup, cModels, rgstrScope);

        homeContents = StringUtils.isBlank(homeContents) ? DEFAULT_HOME_CONTENTS : homeContents;
        homeContents = links + "<td class='contentTD'>" + "<h2 class='section-title'>Home</h2>" + homeContents + "</td>";
        homeContents = getPageWrapper(projectDetail, homeContents);

        String fileName;
        TreeMap<String, String> mapFNameToContent = new TreeMap<>();
        mapFNameToContent.put("index", homeContents);

        // create our Class Group content files
        createClassGroupContent(mapFNameToContent, links, projectDetail, mapGroupNameToClassGroup);

        for (ClassModel cModel : cModels) {
            String contents = links;
            if (StringUtils.isNotBlank(cModel.getNameLine())) {
                fileName = cModel.getClassName();
                contents += "<td class='contentTD'>";

                contents += htmlForClassModel(cModel);

                // deal with any nested classes
                for (ClassModel cmChild : cModel.getChildClassesSorted()) {
                    contents += "<p/>";
                    contents += htmlForClassModel(cmChild);
                }

            } else {
                continue;
            }

            contents = getPageWrapper(projectDetail, contents);
            mapFNameToContent.put(fileName, contents);
        }
        createHTML(mapFNameToContent, path);
    }

    /**
     * Creates the HTML for the provided class, including its property and methods
     * @param cModel    The {@link ClassModel} to generate the html from
     * @return html string
     */
    private String htmlForClassModel(ClassModel cModel) {
 
        // Create a context and add data to the template placeholder
        VelocityContext context = new VelocityContext();
        context.put("class", cModel);
 
        return getTemplateService().createClassPage(context);
    }

    // create our Class Group content files
    private void createClassGroupContent(TreeMap<String, String> mapFNameToContent, String links, String projectDetail,
            TreeMap<String, ClassGroup> mapGroupNameToClassGroup) {

        for (String strGroup : mapGroupNameToClassGroup.keySet()) {
            ClassGroup cg = mapGroupNameToClassGroup.get(strGroup);
            if (cg.getContentSource() != null) {
                String cgContent = parseHTMLFile(cg.getContentSource());
                if (!"".equals(cgContent)) {
                    String strHtml = getPageWrapper(projectDetail, links + "<td class='contentTD'>" +
                            "<h2 class='section-title'>" +
                            StringEscapeUtils.escapeHtml4(cg.getName()) + "</h2>" + cgContent + "</td>");
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
    private String getPageLinks(TreeMap<String, ClassGroup> mapGroupNameToClassGroup, List<ClassModel> cModels, String[] rgstrScope) {
        
        Collections.sort(cModels);
        
        // add a bucket ClassGroup for all Classes without a ClassGroup specified
        mapGroupNameToClassGroup.put("Miscellaneous", new ClassGroup("Miscellaneous", null));
        
        Map<String, List<ClassModel>> classesByGroup = new HashMap<>();
        for (ClassModel cModel : cModels) {
        	if (StringUtils.isNotBlank(cModel.getNameLine())) {
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
        context.put("scopeList", rgstrScope);
        
        return getTemplateService().createLinks(context);
    }

    private void doCopy(String source, String target) throws IOException, URISyntaxException {
        InputStream is = this.getClass().getClassLoader().getResourceAsStream(source);
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

    //TODO: Move the assets into their own directory and copy the whole thing at once
    private void copy(String toFileName) throws IOException, URISyntaxException {
        doCopy("apex_doc_logo.png", toFileName);
        doCopy("ApexDoc.css", toFileName);
        doCopy("ApexDoc.js", toFileName);
        doCopy("CollapsibleList.js", toFileName);

    }

    public List<File> getFiles(String path) {
        return new ArrayList<>(FileUtils.listFiles(new File(path), new String[]{"cls"}, false));
    }

    protected String parseFile(String filePath) {
        try {
            if (StringUtils.isNotBlank(filePath)) {
                return new String(Files.readAllBytes(new File(filePath).toPath()));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return "";
    }

    //TODO: Not terribly happy with how this currently works, refactor later (breaking change)
    public String parseHTMLFile(String filePath) {

        String contents = (parseFile(filePath)).trim();
        if (contents.length() > 0) {
            int startIndex = contents.indexOf("<body>");
            int endIndex = contents.indexOf("</body>");
            if (startIndex != -1) {
                if (endIndex != -1) {
                    contents = contents.substring(startIndex+6, endIndex);
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