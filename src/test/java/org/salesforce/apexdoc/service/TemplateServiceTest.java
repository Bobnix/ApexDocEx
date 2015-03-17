package org.salesforce.apexdoc.service;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.testng.annotations.Test;

import java.io.StringWriter;

public class TemplateServiceTest {

    @Test
    public void testConstructor(){
        final VelocityEngine mock = Mockito.mock(VelocityEngine.class);

        new TemplateService(){
            @Override
            protected VelocityEngine getNewVelocityEngine() {
                return mock;
            }
        };

        Mockito.verify(mock).setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath");
        Mockito.verify(mock).setProperty("classpath.resource.loader.class", ClasspathResourceLoader.class.getName());
        Mockito.verify(mock).init();
    }

    @Test
    public void testCreateLink(){
        final VelocityEngine mockEngine = Mockito.mock(VelocityEngine.class);
        Template mockTemplate = Mockito.mock(Template.class);
        Mockito.when(mockEngine.getTemplate("templates/pageLinks.vm")).thenReturn(mockTemplate);

        TemplateService service = new TemplateService(){
            @Override
            protected VelocityEngine getNewVelocityEngine() {
                return mockEngine;
            }
        };

        VelocityContext context = new VelocityContext();
        service.createLinks(context);

        Mockito.verify(mockTemplate).merge(Matchers.eq(context), Matchers.any(StringWriter.class));
    }

    @Test
    public void testCreatePageWrapper(){
        final VelocityEngine mockEngine = Mockito.mock(VelocityEngine.class);
        Template mockTemplate = Mockito.mock(Template.class);
        Mockito.when(mockEngine.getTemplate("templates/pageWrapper.vm")).thenReturn(mockTemplate);

        TemplateService service = new TemplateService(){
            @Override
            protected VelocityEngine getNewVelocityEngine() {
                return mockEngine;
            }
        };

        VelocityContext context = new VelocityContext();
        service.createPageWrapper(context);

        Mockito.verify(mockTemplate).merge(Matchers.eq(context), Matchers.any(StringWriter.class));
    }

    @Test
    public void testCreateClassPage(){
        final VelocityEngine mockEngine = Mockito.mock(VelocityEngine.class);
        Template mockTemplate = Mockito.mock(Template.class);
        Mockito.when(mockEngine.getTemplate("templates/classPage.vm")).thenReturn(mockTemplate);

        TemplateService service = new TemplateService(){
            @Override
            protected VelocityEngine getNewVelocityEngine() {
                return mockEngine;
            }
        };

        VelocityContext context = new VelocityContext();
        service.createClassPage(context);

        Mockito.verify(mockTemplate).merge(Matchers.eq(context), Matchers.any(StringWriter.class));
    }


}
