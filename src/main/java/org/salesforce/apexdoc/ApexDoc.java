package org.salesforce.apexdoc;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import com.lexicalscope.jewel.cli.ArgumentValidationException;
import com.lexicalscope.jewel.cli.Cli;
import com.lexicalscope.jewel.cli.CliFactory;
import org.apache.commons.io.FilenameUtils;
import org.salesforce.apexdoc.model.ClassGroup;
import org.salesforce.apexdoc.model.ClassModel;
import org.salesforce.apexdoc.model.OptionsModel;
import org.salesforce.apexdoc.service.ClassParsingService;
import org.salesforce.apexdoc.service.FileManager;

public class ApexDoc {

    private static String[] rgstrScope = {"global","public","webService"};
    
    private ClassParsingService parsingService;
    private FileManager fileManager;

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

        Cli<OptionsModel> cli = CliFactory.createCli(OptionsModel.class);
        OptionsModel result = null;
        try
        {
            result = cli.parseArguments(args);
        }
        catch(ArgumentValidationException e)
        {
            printHelp();
            System.exit(-1);
        }

        if(result.getScope() != null){
            rgstrScope = result.getScope().split(";");
        }

        // find all the files to parse
        FileManager fm = getFileManager();
        List<File> files = fm.getFiles(result.getSourceDirectory());
        List<ClassModel> cModels = new ArrayList<>();

        // parse each file, creating a class model for it
        for (File fromFile : files) {
            String fromFileName = fromFile.getAbsolutePath();
            if (fromFileName.endsWith(".cls")) {
                ClassModel cModel = parseFileContents(fromFileName, result.ignoreTests());
                if (cModel != null) {
                    cModels.add(cModel);
                }
            }

        }

        // create our Groups
        TreeMap<String, ClassGroup> mapGroupNameToClassGroup = createMapGroupNameToClassGroup(cModels, result.getSourceDirectory());

        // load up optional specified file templates
        String projectDetail = fm.parseHTMLFile(result.getAuthorFile());
        String homeContents = fm.parseHTMLFile(result.getHomeFile());

        // create our set of HTML files
        fm.createDoc(mapGroupNameToClassGroup, cModels, projectDetail, homeContents, rgstrScope, result.getTargetDirectory());

        // we are done!
        System.out.println("ApexDoc has completed!");
    }

    private static void printHelp() {
        System.out.println("ApexDoc - a tool for generating documentation from Salesforce Apex code class files.\n");
        System.out.println("    Invalid Arguments detected.  The correct syntax is:\n");
        System.out.println("apexdoc -s <source_directory> [-t <target_directory>] [-g <source_url>] [-h <homefile>] [-a <authorfile>] [-p <scope>]\n");
        Cli<OptionsModel> cli = CliFactory.createCli(OptionsModel.class);
        System.out.println(cli.getHelpMessage());
    }

    private TreeMap<String, ClassGroup> createMapGroupNameToClassGroup(List<ClassModel> cModels,
            String sourceDirectory) {
        TreeMap<String, ClassGroup> map = new TreeMap<>();
        for (ClassModel cmodel : cModels) {
            String strGroup = cmodel.getClassGroup();
            String strGroupContent = cmodel.getClassGroupContent();
            if (strGroupContent != null)
                strGroupContent = sourceDirectory.trim() + "/" + strGroupContent;
            strGroupContent = FilenameUtils.separatorsToSystem(strGroupContent);

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

    private ClassModel parseFileContents(String filePath, boolean ignoreTestClass) {
			try {
				FileInputStream fstream = new FileInputStream(filePath);
				DataInputStream in = new DataInputStream(fstream);
	            BufferedReader br = new BufferedReader(new InputStreamReader(in));
	            ClassModel parsedClass = getClassParsingService().parseFileContents(br, ignoreTestClass);
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

    private FileManager getFileManager(){
        if(fileManager == null){
            fileManager = new FileManager();
        }
        return fileManager;
    }

}