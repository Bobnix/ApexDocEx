package org.salesforce.apexdoc;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.apache.velocity.tools.generic.EscapeTool;
import org.salesforce.apexdoc.model.ApexModel;
import org.salesforce.apexdoc.model.ClassModel;
import org.salesforce.apexdoc.model.MethodModel;
import org.salesforce.apexdoc.model.PropertyModel;

public class FileManager {
    private FileOutputStream fos;
    private DataOutputStream dos;
    private String path;
    private StringBuffer infoMessages;
    private String[] rgstrScope;

    public FileManager() {
        infoMessages = new StringBuffer();
    }

    private static String escapeHTML(String s) {
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

    private boolean createHTML(TreeMap<String, String> mapFNameToContent, IProgressMonitor monitor) {
        try {
            if (path.endsWith("/") || path.endsWith("\\")) {
                path += Constants.ROOT_DIRECTORY; // + "/" + fileName + ".html";
            } else {
                path += "/" + Constants.ROOT_DIRECTORY; // + "/" + fileName + ".html";
            }

            (new File(path)).mkdirs();

            for (String fileName : mapFNameToContent.keySet()) {
                String contents = mapFNameToContent.get(fileName);
                fileName = path + "/" + fileName + ".html";
                File file = new File(fileName);
                fos = new FileOutputStream(file);
                dos = new DataOutputStream(fos);
                dos.writeBytes(contents);
                dos.close();
                fos.close();
                infoMessages.append(fileName + " Processed...\n");
                System.out.println(fileName + " Processed...");
                if (monitor != null)
                    monitor.worked(1);
            }
            copy(path);
            return true;
        } catch (Exception e) {

            e.printStackTrace();
        }

        return false;
    }

    private String strLinkfromModel(ApexModel model, String strClassName, String hostedSourceURL) {
        return "<a target='_blank' class='hostedSourceLink' href='" + hostedSourceURL + strClassName + ".cls#L"
                + model.getInameLine() + "'>";
    }

    private String strHTMLScopingPanel() {
        String str = "<tr><td colspan='2' style='text-align: center;' >";
        str += "Show: ";

        for (int i = 0; i < rgstrScope.length; i++) {
            str += "<input type='checkbox' checked='checked' id='cbx" + rgstrScope[i] +
                    "' onclick='ToggleScope(\"" + rgstrScope[i] + "\", this.checked );'>" +
                    rgstrScope[i] + "</input>&nbsp;&nbsp;";
        }
        str += "</td></tr>";
        return str;
    }

    /********************************************************************************************
     * @description main routine that creates an HTML file for each class specified
     * @param mapGroupNameToClassGroup
     * @param cModels
     * @param projectDetail
     * @param homeContents
     * @param hostedSourceURL
     * @param monitor
     */
    private void makeFile(TreeMap<String, ClassGroup> mapGroupNameToClassGroup, ArrayList<ClassModel> cModels,
            String projectDetail, String homeContents, String hostedSourceURL, IProgressMonitor monitor) {
        String links = "<table width='100%'>";
        links += strHTMLScopingPanel();
        links += "<tr style='vertical-align:top;' >";
        links += getPageLinks(mapGroupNameToClassGroup, cModels);

        if (homeContents != null && homeContents.trim().length() > 0) {
            homeContents = links + "<td class='contentTD'>" + "<h2 class='section-title'>Home</h2>" + homeContents + "</td>";
            homeContents = Constants.getHeader(projectDetail) + homeContents + Constants.FOOTER;
        } else {
            homeContents = Constants.DEFAULT_HOME_CONTENTS;
            homeContents = links + "<td class='contentTD'>" + "<h2 class='section-title'>Home</h2>" + homeContents + "</td>";
            homeContents = Constants.getHeader(projectDetail) + homeContents + Constants.FOOTER;
        }

        String fileName = "";
        TreeMap<String, String> mapFNameToContent = new TreeMap<String, String>();
        mapFNameToContent.put("index", homeContents);

        // create our Class Group content files
        createClassGroupContent(mapFNameToContent, links, projectDetail, mapGroupNameToClassGroup, cModels, monitor);

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

            contents = Constants.getHeader(projectDetail) + contents + Constants.FOOTER;
            mapFNameToContent.put(fileName, contents);
            if (monitor != null)
                monitor.worked(1);
        }
        createHTML(mapFNameToContent, monitor);
    }

    /*********************************************************************************************
     * @description creates the HTML for the provided class, including its
     *              property and methods
     * @param cModel
     * @param hostedSourceURL
     * @return html string
     */
    private String htmlForClassModel(ClassModel cModel, String hostedSourceURL) {
    	
    	Velocity.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath"); 
        Velocity.setProperty("classpath.resource.loader.class", ClasspathResourceLoader.class.getName());
        Velocity.init();
 
        // Getting the Template
        Template temp = Velocity.getTemplate("main/resource/templates/classPage.vm");
 
        // Create a context and add data to the template placeholder
        VelocityContext context = new VelocityContext();
        context.put("class", cModel);
        context.put("hostedSourceURL", hostedSourceURL);
        context.put("esc", new EscapeTool());
 
        // Fetch template into a StringWriter
        StringWriter writer = new StringWriter();
        temp.merge( context, writer );
        return writer.toString();
    }

    // create our Class Group content files
    private void createClassGroupContent(TreeMap<String, String> mapFNameToContent, String links, String projectDetail,
            TreeMap<String, ClassGroup> mapGroupNameToClassGroup,
            ArrayList<ClassModel> cModels, IProgressMonitor monitor) {

        for (String strGroup : mapGroupNameToClassGroup.keySet()) {
            ClassGroup cg = mapGroupNameToClassGroup.get(strGroup);
            if (cg.getContentSource() != null) {
                String cgContent = parseHTMLFile(cg.getContentSource());
                if (cgContent != "") {
                    String strHtml = Constants.getHeader(projectDetail) + links + "<td class='contentTD'>" +
                            "<h2 class='section-title'>" +
                            escapeHTML(cg.getName()) + "</h2>" + cgContent + "</td>";
                    strHtml += Constants.FOOTER;
                    mapFNameToContent.put(cg.getContentFilename(), strHtml);
                    if (monitor != null)
                        monitor.worked(1);
                }
            }
        }
    }

    /**********************************************************************************************************
     * @description generate the HTML string for the Class Menu to display on
     *              each page.
     * @param mapGroupNameToClassGroup
     *            map that holds all the Class names, and their respective Class
     *            Group.
     * @param cModels
     *            list of ClassModels
     * @return String of HTML
     */
    private String getPageLinks(TreeMap<String, ClassGroup> mapGroupNameToClassGroup, ArrayList<ClassModel> cModels) {
        
        // this is the only place we need the list of class models sorted by name.
    	//TODO: replace with comparator
        TreeMap<String, ClassModel> tm = new TreeMap<String, ClassModel>();
        for (ClassModel cm : cModels)
            tm.put(cm.getClassName().toLowerCase(), cm);
        cModels = new ArrayList<ClassModel>(tm.values());
        
        // add a bucket ClassGroup for all Classes without a ClassGroup specified
        mapGroupNameToClassGroup.put("Miscellaneous", new ClassGroup("Miscellaneous", null));
        
        Map<String, List<ClassModel>> classesByGroup = new HashMap<String, List<ClassModel>>();
        for (ClassModel cModel : cModels) {
        	if (cModel.getNameLine() != null && cModel.getNameLine().trim().length() > 0) { //TODO: Replace with isBlank
		    	String grp = cModel.getClassGroup() != null?cModel.getClassGroup():"Miscellaneous";
		    	if(!classesByGroup.containsKey(grp)){
		    		classesByGroup.put(grp, new ArrayList<ClassModel>());
		    	}
                classesByGroup.get(grp).add(cModel);
            }
        }
        
        
        Velocity.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath"); 
        Velocity.setProperty("classpath.resource.loader.class", ClasspathResourceLoader.class.getName());
        Velocity.init();
 
        // Getting the Template
        Template temp = Velocity.getTemplate("main/resource/templates/pageLinks.vm");
 
        // Create a context and add data to the template placeholder
        VelocityContext context = new VelocityContext();
        context.put("groups", mapGroupNameToClassGroup);
        context.put("classes", classesByGroup);
 
        // Fetch template into a StringWriter
        StringWriter writer = new StringWriter();
        temp.merge( context, writer );
        
        return writer.toString();
    }

    private void docopy(String source, String target) throws Exception {

        InputStream is = this.getClass().getClassLoader().getResourceAsStream("main/resource/"+source);
        // InputStreamReader isr = new InputStreamReader(is);
        // BufferedReader reader = new BufferedReader(isr);
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

    private void copy(String toFileName) throws IOException, Exception {
        docopy("apex_doc_logo.png", toFileName);
        docopy("ApexDoc.css", toFileName);
        docopy("ApexDoc.js", toFileName);
        docopy("CollapsibleList.js", toFileName);
        docopy("jquery-latest.js", toFileName);
        docopy("toggle_block_btm.gif", toFileName);
        docopy("toggle_block_stretch.gif", toFileName);

    }

    public ArrayList<File> getFiles(String path) {
        File folder = new File(path);
        ArrayList<File> listOfFilesToCopy = new ArrayList<File>();
        if (folder != null) {
            File[] listOfFiles = folder.listFiles();
            if (listOfFiles != null && listOfFiles.length > 0) {
                for (int i = 0; i < listOfFiles.length; i++) {
                    if (listOfFiles[i].isFile()) {
                        listOfFilesToCopy.add(listOfFiles[i]);
                    }
                }
            }
        }
        return listOfFilesToCopy;
    }

    public void createDoc(TreeMap<String, ClassGroup> mapGroupNameToClassGroup, ArrayList<ClassModel> cModels,
            String projectDetail, String homeContents, String hostedSourceURL, IProgressMonitor monitor) {
        makeFile(mapGroupNameToClassGroup, cModels, projectDetail, homeContents, hostedSourceURL, monitor);
    }

    private String parseFile(String filePath) {
        try {
            if (filePath != null && filePath.trim().length() > 0) {
                FileInputStream fstream = new FileInputStream(filePath);
                // Get the object of DataInputStream
                DataInputStream in = new DataInputStream(fstream);
                BufferedReader br = new BufferedReader(new InputStreamReader(in));
                String contents = "";
                String strLine;

                while ((strLine = br.readLine()) != null) {
                    // Print the content on the console
                    strLine = strLine.trim();
                    if (strLine != null && strLine.length() > 0) {
                        contents += strLine;
                    }
                }
                // System.out.println("Contents = " + contents);
                br.close();
                return contents;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return "";
    }

    public String parseHTMLFile(String filePath) {

        String contents = (parseFile(filePath)).trim();
        if (contents != null && contents.length() > 0) {
            int startIndex = contents.indexOf("<body>");
            int endIndex = contents.indexOf("</body>");
            if (startIndex != -1) {
                if (contents.indexOf("</body>") != -1) {
                    contents = contents.substring(startIndex, endIndex);
                    return contents;
                }
            }
        }
        return "";
    }

}