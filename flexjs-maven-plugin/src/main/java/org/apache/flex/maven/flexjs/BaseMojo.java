package org.apache.flex.maven.flexjs;

import org.apache.flex.tools.FlexTool;
import org.apache.flex.tools.FlexToolGroup;
import org.apache.flex.tools.FlexToolRegistry;
import org.apache.maven.RepositoryUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.*;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.eclipse.aether.RepositorySystemSession;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

/**
 * Created by christoferdutz on 22.04.16.
 */
public abstract class BaseMojo
        extends AbstractMojo
{

    @Parameter(defaultValue = "${project}", readonly = true)
    protected MavenProject project;

    @Parameter(defaultValue="${project.build.directory}")
    protected File outputDirectory;

    @Parameter
    private Namespace[] namespaces;

    @Parameter
    private String[] includeClasses;

    @Parameter
    private String targetPlayer = "11.1";

    @Parameter
    private boolean includeSources = false;

    @Parameter
    private boolean debug = false;

    @Parameter(readonly = true, defaultValue = "${repositorySystemSession}")
    private RepositorySystemSession repositorySystemSession;

    @Component
    private ProjectDependenciesResolver projectDependenciesResolver;

    protected boolean skip() {
        return false;
    }

    protected abstract String getConfigFileName();

    protected abstract File getOutput();

    protected VelocityContext getVelocityContext() throws MojoExecutionException {
        VelocityContext context = new VelocityContext();

        List<Artifact> allLibraries = getAllLibraries();
        List<Artifact> libraries = getLibraries(allLibraries);
        List<Artifact> externalLibraries = getExternalLibraries(allLibraries);
        List<String> sourcePaths = getSourcePaths();
        context.put("libraries", libraries);
        context.put("externalLibraries", externalLibraries);
        context.put("sourcePaths", sourcePaths);
        context.put("namespaces", namespaces);
        context.put("includeClasses", includeClasses);
        context.put("targetPlayer", targetPlayer);
        context.put("includeSources", includeSources);
        context.put("debug", debug);
        context.put("output", getOutput());

        return context;
    }

    protected abstract String getToolGroupName();

    protected abstract String getFlexTool();

    @SuppressWarnings("unchecked")
    protected List<String> getSourcePaths() {
        List<String> sourcePaths = new LinkedList<String>();
        for(String sourcerPath : (List<String>) project.getCompileSourceRoots()) {
            if(new File(sourcerPath).exists()) {
                sourcePaths.add(sourcerPath);
            }
        }
        return sourcePaths;
    }

    protected List<String> getCompilerArgs(File configFile) {
        List<String> args = new LinkedList<String>();
        args.add("-load-config=" + configFile.getPath());
        return args;
    }

    public void execute()
            throws MojoExecutionException
    {
        // Skip this step if not all preconditions are met.
        if(skip()) {
            return;
        }

        // Prepare the config file.
        File configFile = new File(outputDirectory, getConfigFileName());
        VelocityEngine velocityEngine = new VelocityEngine();
        velocityEngine.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath");
        velocityEngine.setProperty("classpath.resource.loader.class", ClasspathResourceLoader.class.getName());
        velocityEngine.init();
        Template template = velocityEngine.getTemplate("config/" + getConfigFileName());
        VelocityContext context = getVelocityContext();
        FileWriter writer = null;
        try {
            writer = new FileWriter(configFile);
            template.merge(context, writer);
        } catch (IOException e) {
            throw new MojoExecutionException("Error creating config file at " + configFile.getPath());
        } finally {
            if(writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    throw new MojoExecutionException("Error creating config file at " + configFile.getPath());
                }
            }
        }

        // Get the tool group.
        FlexToolRegistry toolRegistry = new FlexToolRegistry();
        FlexToolGroup toolGroup = toolRegistry.getToolGroup(getToolGroupName());
        if(toolGroup == null) {
            throw new MojoExecutionException("Could not find tool group: " + getToolGroupName());
        }

        // Get an instance of the compiler and run the build.
        FlexTool tool = toolGroup.getFlexTool(getFlexTool());
        String[] args = getCompilerArgs(configFile).toArray(new String[0]);
        getLog().info("Executing " + getFlexTool() + " in tool group " + getToolGroupName() + " with args: " + Arrays.toString(args));
        tool.execute(args);
    }

    protected List<Artifact> getLibraries(List<Artifact> artifacts) {
        List<Artifact> libraries = new LinkedList<Artifact>();
        for(Artifact artifact : artifacts) {
            if(!"external".equalsIgnoreCase(artifact.getScope()) && includeLibrary(artifact)) {
                libraries.add(artifact);
            }
        }
        return libraries;
    }

    protected List<Artifact> getExternalLibraries(List<Artifact> artifacts) {
        List<Artifact> externalLibraries = new LinkedList<Artifact>();
        for(Artifact artifact : artifacts) {
            if("external".equalsIgnoreCase(artifact.getScope()) && includeLibrary(artifact)) {
                externalLibraries.add(artifact);
            }
        }
        return externalLibraries;
    }

    private List<Artifact> getAllLibraries() throws MojoExecutionException {
        DefaultDependencyResolutionRequest dependencyResolutionRequest =
                new DefaultDependencyResolutionRequest(project, repositorySystemSession);
        DependencyResolutionResult dependencyResolutionResult;

        try {
            dependencyResolutionResult = projectDependenciesResolver.resolve(dependencyResolutionRequest);
        } catch (DependencyResolutionException ex) {
            throw new MojoExecutionException(ex.getMessage(), ex);
        }

        List<Artifact> artifacts = new LinkedList<Artifact>();
        if (dependencyResolutionResult.getDependencyGraph() != null
                && !dependencyResolutionResult.getDependencyGraph().getChildren().isEmpty()) {
            RepositoryUtils.toArtifacts(artifacts, dependencyResolutionResult.getDependencyGraph().getChildren(),
                    Collections.singletonList(project.getArtifact().getId()), null);
        }
        return artifacts;
    }

    protected boolean includeLibrary(Artifact library) {
        return true;
    }

}
