package org.salesforce.apexdoc;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.TreeMap;

import org.salesforce.apexdoc.model.ClassGroup;
import org.salesforce.apexdoc.model.ClassModel;
import org.salesforce.apexdoc.service.ClassParsingService;
import org.salesforce.apexdoc.service.FileManager;

public class ApexDoc {

    private FileManager fm;
    private static String[] rgstrScope = {"global","public","webService"};
    
    private ClassParsingService parsingService;

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
        ArrayList<ClassModel> cModels = new ArrayList<>();

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
        TreeMap<String, ClassGroup> map = new TreeMap<>();
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
				DataInputStream in = new DataInputStream(fstream);
	            BufferedReader br = new BufferedReader(new InputStreamReader(in));
	            ClassModel parsedClass = getClassParsingService().parseFileContents(br);
	            in.close();
	            return parsedClass;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return null;
            
    }

    public static String strContainsScope(String str) {
        str = str.toLowerCase();
        for (String scope : rgstrScope) {
            if (str.toLowerCase().contains(scope.toLowerCase() + " ")) {
                return scope;
            }
        }
        return null;
    }
    
    private ClassParsingService getClassParsingService(){
    	if(parsingService == null){
    		parsingService = new ClassParsingService();
    	}
    	return parsingService;
    }

}