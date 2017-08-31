/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.function;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.maven.function.template.FunctionTemplate;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.lang.System.out;
import static javax.lang.model.SourceVersion.*;
import static org.codehaus.plexus.util.IOUtil.copy;
import static org.codehaus.plexus.util.StringUtils.isNotEmpty;

/**
 * Add new Azure Function to existing project
 */
@Mojo(name = "add")
public class AddMojo extends AbstractFunctionMojo {
    public static final String LOAD_TEMPLATES = "Step 1 of 4: Load all function templates";
    public static final String LOAD_TEMPLATES_DONE = "Successfully loaded all function templates";
    public static final String LOAD_TEMPLATES_FAIL = "Failed to load all function templates.";
    public static final String FIND_TEMPLATE = "Step 2 of 4: Find specified function template";
    public static final String FIND_TEMPLATE_DONE = "Successfully found function template: ";
    public static final String FIND_TEMPLATE_FAIL = "Function template not found: ";
    public static final String PREPARE_PARAMS = "Step 3 of 4: Prepare required parameters";
    public static final String SAVE_FILE = "Step 4 of 4: Saving function to file";
    public static final String SAVE_FILE_DONE = "Successfully saved new function at ";
    public static final String FILE_EXIST = "Function already exists at %s. Please specify a different function name.";

    //region Properties

    @Parameter(defaultValue = "${project.baseDir}", readonly = true, required = true)
    protected String baseDir;

    @Parameter(defaultValue = "${project.compileSourceRoots}", readonly = true, required = true)
    protected List<String> compileSourceRoots;

    /**
     * Package name of the new function.
     *
     * @since 0.1.0
     */
    @Parameter(property = "functions.package")
    protected String functionPackageName;

    /**
     * Name of the new function.
     *
     * @since 0.1.0
     */
    @Parameter(property = "functions.name")
    protected String functionName;

    /**
     * Template for the new function
     *
     * @since 0.1.0
     */
    @Parameter(property = "functions.template")
    protected String functionTemplate;

    //endregion

    //region Getter and Setter

    public String getFunctionPackageName() {
        return functionPackageName;
    }

    public String getFunctionName() {
        return functionName;
    }

    public String getFunctionTemplate() {
        return functionTemplate;
    }

    protected String getBaseDir() {
        return baseDir;
    }

    protected String getSourceRoot() {
        return compileSourceRoots == null || compileSourceRoots.isEmpty() ?
                Paths.get(getBaseDir(), "src", "main", "java").toString() :
                compileSourceRoots.get(0);
    }

    protected void setFunctionPackageName(String functionPackageName) {
        this.functionPackageName = StringUtils.lowerCase(functionPackageName);
    }

    protected void setFunctionName(String functionName) {
        this.functionName = StringUtils.capitalise(functionName);
    }

    protected void setFunctionTemplate(String functionTemplate) {
        this.functionTemplate = functionTemplate;
    }

    //endregion

    @Override
    protected void doExecute() throws Exception {
        final List<FunctionTemplate> templates = loadAllFunctionTemplates();

        final FunctionTemplate template = getFunctionTemplate(templates);

        final Map params = prepareRequiredParameters(template);

        final String newFunctionClass = substituteParametersInTemplate(template, params);

        saveNewFunctionToFile(newFunctionClass);
    }

    //region Load all templates

    protected List<FunctionTemplate> loadAllFunctionTemplates() throws Exception {
        getLog().info("");
        getLog().info(LOAD_TEMPLATES);

        try (final InputStream is = AddMojo.class.getResourceAsStream("/templates.json")) {
            final String templatesJsonStr = IOUtil.toString(is);
            final List<FunctionTemplate> templates = parseTemplateJson(templatesJsonStr);
            getLog().info(LOAD_TEMPLATES_DONE);
            return templates;
        } catch (Exception e) {
            getLog().error(LOAD_TEMPLATES_FAIL);
            throw e;
        }
    }

    protected List<FunctionTemplate> parseTemplateJson(final String templateJson) throws Exception {
        final FunctionTemplate[] templates = new ObjectMapper().readValue(templateJson, FunctionTemplate[].class);
        return Arrays.asList(templates);
    }

    //endregion

    //region Get function template

    protected FunctionTemplate getFunctionTemplate(final List<FunctionTemplate> templates) throws Exception {
        getLog().info("");
        getLog().info(FIND_TEMPLATE);

        assureInputFromUser("Function Template",
                getFunctionTemplate(),
                getTemplateNames(templates),
                this::setFunctionTemplate);

        return findTemplateByName(templates, getFunctionTemplate());
    }

    protected List<String> getTemplateNames(final List<FunctionTemplate> templates) {
        return templates.stream().map(t -> t.getMetadata().getName()).collect(Collectors.toList());
    }

    protected FunctionTemplate findTemplateByName(final List<FunctionTemplate> templates, final String templateName)
            throws Exception {
        getLog().info("Specified function template: " + templateName);
        final Optional<FunctionTemplate> template = templates.stream()
                .filter(t -> t.getMetadata().getName().equalsIgnoreCase(templateName))
                .findFirst();

        if (template.isPresent()) {
            getLog().info(FIND_TEMPLATE_DONE + templateName);
            return template.get();
        }

        throw new Exception(FIND_TEMPLATE_FAIL + templateName);
    }

    //endregion

    //region Prepare parameters

    protected Map<String, String> prepareRequiredParameters(final FunctionTemplate template) {
        getLog().info("");
        getLog().info(PREPARE_PARAMS);

        prepareFunctionName();

        preparePackageName();

        final Map<String, String> params = prepareTemplateParameters(template);

        params.put("functionName", getFunctionName());
        params.put("packageName", getFunctionPackageName());

        return params;
    }

    protected void prepareFunctionName() {
        getLog().info("Common parameter [Function Name]: name for both the new function and Java class");

        assureInputFromUser("Enter value for Function Name: ",
                getFunctionName(),
                str -> isNotEmpty(str) && isIdentifier(str) && !isKeyword(str),
                "Input should be a valid Java class name.",
                this::setFunctionName);

        getLog().info("Value to use: " + getFunctionName());
    }

    protected void preparePackageName() {
        getLog().info("Common parameter [Package Name]: package name of the new Java class");

        assureInputFromUser("Enter value for Package Name: ",
                getFunctionPackageName(),
                str -> isNotEmpty(str) && isName(str),
                "Input should be a valid Java package name.",
                this::setFunctionPackageName);

        getLog().info("Value to use: " + getFunctionPackageName());
    }

    protected Map<String, String> prepareTemplateParameters(final FunctionTemplate template) {
        final Map<String, String> params = new HashMap<>();

        for (final String property : template.getMetadata().getUserPrompt()) {
            getLog().info(format("Trigger specific parameter [%s]", property));

            assureInputFromUser(format("Enter value for %s: ", property),
                    null,
                    str -> isNotEmpty(str),
                    "Input should be a non-empty string.",
                    str -> params.put(property, str));

            getLog().info("Value to use: " + params.get(property));
        }

        return params;
    }

    //endregion

    //region Substitute parameters

    protected String substituteParametersInTemplate(final FunctionTemplate template, final Map<String, String> params) {
        String ret = template.getFiles().get("function.java");
        for (final Map.Entry<String, String> entry : params.entrySet()) {
            ret = ret.replace(String.format("$%s$", entry.getKey()), entry.getValue());
        }
        return ret;
    }

    //endregion

    //region Save function to file

    protected void saveNewFunctionToFile(final String newFunctionClass) throws Exception {
        getLog().info("");
        getLog().info(SAVE_FILE);

        final File packageDir = getPackageDir();

        final File targetFile = getTargetFile(packageDir);

        createPackageDirIfNotExist(packageDir);

        saveToTargetFile(targetFile, newFunctionClass);

        getLog().info(SAVE_FILE_DONE + targetFile.getAbsolutePath());
    }

    protected File getPackageDir() {
        final String sourceRoot = getSourceRoot();
        final String[] packageName = getFunctionPackageName().split("\\.");
        return Paths.get(sourceRoot, packageName).toFile();
    }

    protected File getTargetFile(final File packageDir) throws Exception {
        final String functionName = getFunctionName() + ".java";
        final File targetFile = new File(packageDir, functionName);
        if (targetFile.exists()) {
            throw new FileAlreadyExistsException(format(FILE_EXIST, targetFile.getAbsolutePath()));
        }
        return targetFile;
    }

    protected void createPackageDirIfNotExist(final File packageDir) {
        if (!packageDir.exists()) {
            packageDir.mkdirs();
        }
    }

    protected void saveToTargetFile(final File targetFile, final String newFunctionClass) throws Exception {
        try (final OutputStream os = new FileOutputStream(targetFile)) {
            copy(newFunctionClass, os);
        }
    }

    //endregion

    //region Helper methods

    protected void assureInputFromUser(final String propertyName, final String initValue, final List<String> options,
                                       final Consumer<String> setter) {
        if (options.stream().anyMatch(o -> o.equalsIgnoreCase(initValue))) {
            return;
        }

        out.printf("Choose from below options for %s.%n", propertyName);
        for (int i = 0; i < options.size(); i++) {
            out.printf("%d. %s%n", i, options.get(i));
        }

        assureInputFromUser("Enter index to use: ",
                null,
                str -> {
                    try {
                        final int index = Integer.parseInt(str);
                        return 0 <= index && index < options.size();
                    } catch (Exception e) {
                        return false;
                    }
                },
                "Invalid index.",
                str -> {
                    final int index = Integer.parseInt(str);
                    setter.accept(options.get(index));
                });
    }

    protected void assureInputFromUser(final String prompt, final String initValue,
                                       final Function<String, Boolean> validator, final String errorMessage,
                                       final Consumer<String> setter) {
        if (validator.apply(initValue)) {
            setter.accept(initValue);
            return;
        }

        final Scanner scanner = getScanner();

        while (true) {
            out.printf(prompt);
            out.flush();
            try {
                final String input = scanner.next();
                if (validator.apply(input)) {
                    setter.accept(input);
                    break;
                }
            } catch (Exception ignored) {
            }
            // Reaching here means invalid input
            getLog().warn(errorMessage);
        }
    }

    protected Scanner getScanner() {
        return new Scanner(System.in, "UTF-8");
    }

    //endregion
}
