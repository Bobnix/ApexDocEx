package org.salesforce.apexdoc.service;

import java.io.StringWriter;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.apache.velocity.tools.generic.EscapeTool;

public class TemplateService {
	
	private VelocityEngine velocity;
	
	public TemplateService(){
		velocity = getNewVelocityEngine();
		velocity.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath"); 
		velocity.setProperty("classpath.resource.loader.class", ClasspathResourceLoader.class.getName());
		velocity.init();
	}
	
	public String createLinks(VelocityContext context){
		return processTemplate("templates/pageLinks.vm", context);
	}
	
	public String createPageWrapper(VelocityContext context){
		return processTemplate("templates/pageWrapper.vm", context);
	}
	
	public String createClassPage(VelocityContext context){
		return processTemplate("templates/classPage.vm", context);
	}
	
	private String processTemplate(String template, VelocityContext context){
        context.put("esc", new EscapeTool());
        
		Template temp = velocity.getTemplate(template);
 
        // Fetch template into a StringWriter
        StringWriter writer = new StringWriter();
        temp.merge( context, writer );
        
        return writer.toString();
	}

    protected VelocityEngine getNewVelocityEngine(){
        return new VelocityEngine();
    }
	
}
