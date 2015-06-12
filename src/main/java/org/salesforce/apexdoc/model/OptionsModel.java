package org.salesforce.apexdoc.model;


import com.lexicalscope.jewel.cli.Option;

public interface OptionsModel {

    @Option(shortName = "s", longName = "sourceDirectory", description = "The folder location which contains your apex .cls classes")
    String getSourceDirectory();

    @Option(shortName = "t", longName = "targetDirectory", description = "Optional. Specifies your target folder where documentation will be generated.", defaultToNull=true)
    String getTargetDirectory();

    @Option(shortName = "h", longName = "homeFile", description = "Optional. Specifies the html file that contains the contents for the home page's content area.", defaultToNull=true)
    String getHomeFile();

    @Option(shortName = "a", longName = "authorFile", description = "Optional. Specifies the text file that contains project information for the documentation header.", defaultToNull=true)
    String getAuthorFile();

    @Option(shortName = "p", longName = "scope", description = "Optional. Semicolon separated list of scopes to document.  Defaults to 'global;public'.", defaultToNull=true)
    String getScope();

    @Option(shortName = "i", longName = "ignoreTests", description = "Optional. If set, will ignore all test classes.")
    boolean ignoreTests();

    @Option(shortName = "c", longName = "cssFile", description = "Optional. Specifies the file that contains any custom css.")
    String getCssFile();

}
