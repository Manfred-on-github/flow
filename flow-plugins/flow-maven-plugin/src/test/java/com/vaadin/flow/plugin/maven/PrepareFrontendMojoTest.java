/*
 * Copyright 2000-2022 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 */
package com.vaadin.flow.plugin.maven;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.ReflectionUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

import com.vaadin.flow.plugin.TestUtils;
import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.InitParameters;
import com.vaadin.flow.server.frontend.FrontendTools;
import com.vaadin.flow.server.frontend.installer.NodeInstaller;

import elemental.json.Json;
import elemental.json.JsonObject;
import elemental.json.impl.JsonUtil;

import static com.vaadin.flow.plugin.maven.BuildFrontendMojoTest.assertContainsPackage;
import static com.vaadin.flow.plugin.maven.BuildFrontendMojoTest.getPackageJson;
import static com.vaadin.flow.plugin.maven.BuildFrontendMojoTest.setProject;
import static com.vaadin.flow.server.Constants.PACKAGE_JSON;
import static com.vaadin.flow.server.InitParameters.SERVLET_PARAMETER_ENABLE_DEV_SERVER;
import static com.vaadin.flow.server.InitParameters.SERVLET_PARAMETER_PRODUCTION_MODE;
import static com.vaadin.flow.server.InitParameters.SERVLET_PARAMETER_USE_V14_BOOTSTRAP;
import static com.vaadin.flow.server.Constants.VAADIN_SERVLET_RESOURCES;
import static com.vaadin.flow.server.Constants.VAADIN_WEBAPP_RESOURCES;
import static com.vaadin.flow.server.frontend.FrontendUtils.DEFAULT_FLOW_RESOURCES_FOLDER;
import static com.vaadin.flow.server.frontend.FrontendUtils.TOKEN_FILE;

public class PrepareFrontendMojoTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Rule
    public ExpectedException exception = ExpectedException.none();

    private final PrepareFrontendMojo mojo = new PrepareFrontendMojo();
    private File flowResourcesFolder;
    private String packageJson;
    private File projectBase;
    private File webpackOutputDirectory;
    private File resourceOutputDirectory;
    private File tokenFile;
    private File defaultJavaSource;
    private File defaultJavaResource;
    private File generatedTsFolder;
    private MavenProject project;

    @Before
    public void setup() throws Exception {

        projectBase = temporaryFolder.getRoot();

        tokenFile = new File(temporaryFolder.getRoot(),
                VAADIN_SERVLET_RESOURCES + TOKEN_FILE);

        project = Mockito.mock(MavenProject.class);

        List<String> packages = Arrays
                .stream(System.getProperty("java.class.path")
                        .split(File.pathSeparatorChar + ""))
                .collect(Collectors.toList());
        Mockito.when(project.getRuntimeClasspathElements())
                .thenReturn(packages);
        Mockito.when(project.getCompileClasspathElements())
                .thenReturn(Collections.emptyList());
        Mockito.when(project.getBasedir()).thenReturn(projectBase);

        flowResourcesFolder = new File(projectBase,
                "target/" + DEFAULT_FLOW_RESOURCES_FOLDER);
        packageJson = new File(projectBase, PACKAGE_JSON).getAbsolutePath();
        webpackOutputDirectory = new File(projectBase, VAADIN_WEBAPP_RESOURCES);
        resourceOutputDirectory = new File(projectBase,
                VAADIN_SERVLET_RESOURCES);
        defaultJavaSource = new File(".", "src/test/java");
        defaultJavaResource = new File(".", "src/test/resources");
        generatedTsFolder = new File(projectBase, "frontend/generated");

        ReflectionUtils.setVariableValueInObject(mojo, Constants.NPM_TOKEN,
                projectBase);
        ReflectionUtils.setVariableValueInObject(mojo,
                Constants.GENERATED_TOKEN, projectBase);
        ReflectionUtils.setVariableValueInObject(mojo, "webpackOutputDirectory",
                webpackOutputDirectory);
        ReflectionUtils.setVariableValueInObject(mojo,
                "resourceOutputDirectory", resourceOutputDirectory);
        ReflectionUtils.setVariableValueInObject(mojo, "frontendDirectory",
                new File(projectBase, "frontend"));

        ReflectionUtils.setVariableValueInObject(mojo, "openApiJsonFile",
                new File(projectBase,
                        "target/generated-resources/openapi.json"));
        ReflectionUtils.setVariableValueInObject(mojo, "applicationProperties",
                new File(projectBase,
                        "src/main/resources/application.properties"));
        ReflectionUtils.setVariableValueInObject(mojo, "javaSourceFolder",
                defaultJavaSource);
        ReflectionUtils.setVariableValueInObject(mojo, "javaResourceFolder",
                defaultJavaResource);
        ReflectionUtils.setVariableValueInObject(mojo, "generatedTsFolder",
                generatedTsFolder);

        ReflectionUtils.setVariableValueInObject(mojo, "pnpmEnable",
                Constants.ENABLE_PNPM_DEFAULT);
        ReflectionUtils.setVariableValueInObject(mojo, "requireHomeNodeExec",
                true);
        ReflectionUtils.setVariableValueInObject(mojo, "nodeVersion",
                FrontendTools.DEFAULT_NODE_VERSION);
        ReflectionUtils.setVariableValueInObject(mojo, "nodeDownloadRoot",
                NodeInstaller.DEFAULT_NODEJS_DOWNLOAD_ROOT);
        ReflectionUtils.setVariableValueInObject(mojo, "projectBasedir",
                projectBase);
        ReflectionUtils.setVariableValueInObject(mojo, "projectBuildDir",
                Paths.get(projectBase.toString(), "target").toString());

        Assert.assertTrue(flowResourcesFolder.mkdirs());
        setProject(mojo, projectBase);
    }

    @Test
    public void tokenFileShouldExist_noDevModeTokenVisible()
            throws IOException, MojoExecutionException, MojoFailureException {
        mojo.execute();
        Assert.assertTrue("No token file could be found", tokenFile.exists());

        String json = org.apache.commons.io.FileUtils
                .readFileToString(tokenFile, "UTF-8");
        JsonObject buildInfo = JsonUtil.parse(json);
        Assert.assertNull("No devMode token should be available",
                buildInfo.get(SERVLET_PARAMETER_ENABLE_DEV_SERVER));
        Assert.assertNotNull("productionMode token should be available",
                buildInfo.get(SERVLET_PARAMETER_PRODUCTION_MODE));
        Assert.assertNotNull(
                "useDeprecatedV14Bootstrapping token should be available",
                buildInfo.get(SERVLET_PARAMETER_USE_V14_BOOTSTRAP));
    }

    @Test
    public void existingTokenFile_enableDevServerShouldBeRemoved()
            throws IOException, MojoExecutionException, MojoFailureException {

        JsonObject initialBuildInfo = Json.createObject();
        initialBuildInfo.put(SERVLET_PARAMETER_PRODUCTION_MODE, false);
        initialBuildInfo.put(SERVLET_PARAMETER_USE_V14_BOOTSTRAP, false);
        initialBuildInfo.put(SERVLET_PARAMETER_ENABLE_DEV_SERVER, false);
        org.apache.commons.io.FileUtils.forceMkdir(tokenFile.getParentFile());
        org.apache.commons.io.FileUtils.write(tokenFile,
                JsonUtil.stringify(initialBuildInfo, 2) + "\n", "UTF-8");

        mojo.execute();

        String json = org.apache.commons.io.FileUtils
                .readFileToString(tokenFile, "UTF-8");
        JsonObject buildInfo = JsonUtil.parse(json);
        Assert.assertNull("No devMode token should be available",
                buildInfo.get(SERVLET_PARAMETER_ENABLE_DEV_SERVER));
        Assert.assertNotNull("productionMode token should be available",
                buildInfo.get(SERVLET_PARAMETER_PRODUCTION_MODE));
        Assert.assertNotNull(
                "useDeprecatedV14Bootstrapping token should be available",
                buildInfo.get(SERVLET_PARAMETER_USE_V14_BOOTSTRAP));
    }

    @Test
    public void writeTokenFile_devModePropertiesAreWritten()
            throws IOException, MojoExecutionException, MojoFailureException {

        mojo.execute();

        String json = org.apache.commons.io.FileUtils
                .readFileToString(tokenFile, StandardCharsets.UTF_8);
        JsonObject buildInfo = JsonUtil.parse(json);

        Assert.assertFalse(
                InitParameters.SERVLET_PARAMETER_ENABLE_PNPM
                        + "should have been written",
                buildInfo.getBoolean(
                        InitParameters.SERVLET_PARAMETER_ENABLE_PNPM));
        Assert.assertTrue(
                InitParameters.REQUIRE_HOME_NODE_EXECUTABLE
                        + "should have been written",
                buildInfo.getBoolean(
                        InitParameters.REQUIRE_HOME_NODE_EXECUTABLE));

        Assert.assertFalse(buildInfo.hasKey(
                InitParameters.SERVLET_PARAMETER_DEVMODE_OPTIMIZE_BUNDLE));
    }

    @Test
    public void mavenGoal_when_packageJsonMissing() throws Exception {
        Assert.assertFalse(FileUtils.fileExists(packageJson));
        mojo.execute();
        assertPackageJsonContent();
    }

    @Test
    public void should_keepDependencies_when_packageJsonExists()
            throws Exception {
        JsonObject json = TestUtils.getInitalPackageJson();
        json.put("dependencies", Json.createObject());
        json.getObject("dependencies").put("foo", "bar");
        FileUtils.fileWrite(packageJson, json.toJson());
        mojo.execute();
        assertPackageJsonContent();

        JsonObject packageJsonObject = getPackageJson(packageJson);
        assertContainsPackage(packageJsonObject.getObject("dependencies"),
                "foo");
    }

    @Test
    public void jarPackaging_copyProjectFrontendResources()
            throws MojoExecutionException, MojoFailureException,
            IllegalAccessException {
        Mockito.when(project.getPackaging()).thenReturn("jar");

        ReflectionUtils.setVariableValueInObject(mojo, "project", project);

        mojo.execute();

        Mockito.verify(project, Mockito.atLeastOnce()).getArtifacts();
    }

    private void assertPackageJsonContent() throws IOException {
        JsonObject packageJsonObject = getPackageJson(packageJson);

        assertContainsPackage(packageJsonObject.getObject("dependencies"),
                "@polymer/polymer");

        assertContainsPackage(packageJsonObject.getObject("devDependencies"),
                "vite", "@rollup/plugin-replace", "rollup-plugin-brotli",
                "vite-plugin-checker");
    }
}
