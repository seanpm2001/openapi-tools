/*
 *  Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.ballerinalang.openapi.validator;

import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.syntax.tree.AnnotationNode;
import io.ballerina.compiler.syntax.tree.ExpressionNode;
import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.ListConstructorExpressionNode;
import io.ballerina.compiler.syntax.tree.MappingConstructorExpressionNode;
import io.ballerina.compiler.syntax.tree.MappingFieldNode;
import io.ballerina.compiler.syntax.tree.MetadataNode;
import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeList;
import io.ballerina.compiler.syntax.tree.SeparatedNodeList;
import io.ballerina.compiler.syntax.tree.ServiceDeclarationNode;
import io.ballerina.compiler.syntax.tree.SpecificFieldNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.SyntaxTree;
import io.ballerina.projects.Document;
import io.ballerina.projects.DocumentId;
import io.ballerina.projects.Module;
import io.ballerina.projects.ModuleId;
import io.ballerina.projects.Package;
import io.ballerina.projects.Project;
import io.ballerina.projects.ProjectKind;
import io.ballerina.tools.diagnostics.Diagnostic;
import io.ballerina.tools.diagnostics.DiagnosticInfo;
import io.ballerina.tools.diagnostics.DiagnosticSeverity;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import org.ballerinalang.openapi.validator.diagnostics.OpenAPIDiagnostics;
import org.ballerinalang.openapi.validator.error.MissingFieldInBallerinaType;
import org.ballerinalang.openapi.validator.error.MissingFieldInJsonSchema;
import org.ballerinalang.openapi.validator.error.OneOfTypeValidation;
import org.ballerinalang.openapi.validator.error.OpenapiServiceValidationError;
import org.ballerinalang.openapi.validator.error.ResourceValidationError;
import org.ballerinalang.openapi.validator.error.TypeMismatch;
import org.ballerinalang.openapi.validator.error.ValidationError;
import org.ballerinalang.util.diagnostic.DiagnosticLog;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * This model used to filter and validate all the operations according to the given filter and filter the service
 * resource in the resource file.
 */
public class ServiceValidator {
    private static DiagnosticLog dLog;
    private static OpenAPI openAPI;
    private static SyntaxTree syntaxTree;
    private static SemanticModel semanticModel;
    private static Project project;
    private static List<Diagnostic> validations = new ArrayList<>();

    public ServiceValidator() {
    }

    /**
     * This method use to extract project details and generate diagnostic.
     * @param project   project instance related to package
     * @return Diagnostic List
     */
    public static List<Diagnostic> validateResourceFunctions(Project project)
            throws IOException, OpenApiValidatorException {
        List<FunctionDefinitionNode> functions = new ArrayList<>();
        DiagnosticSeverity kind = DiagnosticSeverity.ERROR;
        Filters filters = new Filters(kind);
        extractProjectDetails(project);

        ModulePartNode modulePartNode = syntaxTree.rootNode();
        for (Node node : modulePartNode.members()) {
            SyntaxKind syntaxKind = node.kind();
            // Load a listen_declaration for the server part in the yaml spec
            if (syntaxKind.equals(SyntaxKind.SERVICE_DECLARATION)) {
                ServiceDeclarationNode serviceDeclarationNode = (ServiceDeclarationNode) node;
                // Check annotation is available 
                Optional<MetadataNode> metadata = serviceDeclarationNode.metadata();
                MetadataNode openApi  = metadata.orElseThrow();
                if (!openApi.annotations().isEmpty()) {
                    NodeList<AnnotationNode> annotations = openApi.annotations();
                    for (AnnotationNode annotationNode: annotations) {
                        Node annotationRefNode = annotationNode.annotReference();
                        if (annotationRefNode.toString().trim().equals("openapi:ServiceInfo")) {
                            Optional<MappingConstructorExpressionNode> mappingConstructorExpressionNode =
                                    annotationNode.annotValue();
                            MappingConstructorExpressionNode exprNode = mappingConstructorExpressionNode.orElseThrow();
                            SeparatedNodeList<MappingFieldNode> fields = exprNode.fields();
                            //Filter annotation attributes
                            if (!fields.isEmpty()) {
                                kind = extractOpenAPIAnnotation(project, kind, filters, fields);
                            }
                        }
                    }
                    // Summaries functions
                    NodeList<Node> members = serviceDeclarationNode.members();
                    Iterator<Node> iterator = members.iterator();
                    while (iterator.hasNext()) {
                        Node next = iterator.next();
                        if (next instanceof FunctionDefinitionNode) {
                            functions.add((FunctionDefinitionNode) next);
                        }
                    }
                    // Make resourcePath summery
                    Map<String, ResourcePathSummary> resourcePathSummaryMap =
                            ResourceWithOperation.summarizeResources(functions);
                    //  Filter openApi operation according to given filters
                    List<OpenAPIPathSummary> openAPIPathSummaries = ResourceWithOperation
                            .filterOpenapi(openAPI, filters);

                    //  Check all the filtered operations are available at the service file
                    List<OpenapiServiceValidationError> openApiMissingServiceMethod =
                            ResourceWithOperation.checkOperationsHasFunctions(openAPIPathSummaries, resourcePathSummaryMap);

                    //  Generate errors for missing resource in service file
                    if (!openApiMissingServiceMethod.isEmpty()) {
                        for (OpenapiServiceValidationError openApiMissingError: openApiMissingServiceMethod) {
                            if (openApiMissingError.getServiceOperation() == null) {
//                                dLog.logDiagnostic(kind, serviceNode.getPosition(),
//                                        ErrorMessages.unimplementedOpenAPIPath(openApiMissingError.getServicePath()));
                                String[] error =
                                        ErrorMessages.unimplementedOpenAPIPath(openApiMissingError.getServicePath());
                                DiagnosticInfo diagnosticInfo = new DiagnosticInfo(error[0], error[1], kind);
                                OpenAPIDiagnostics diagnostics =
                                        new OpenAPIDiagnostics(openApiMissingError.getPosition(), diagnosticInfo,
                                                error[1]);
                                validations.add(diagnostics);
                            } else {
                                String[] error =
                                        ErrorMessages.unimplementedOpenAPIOperationsForPath(openApiMissingError.
                                                        getServiceOperation(),
                                                openApiMissingError.getServicePath());
                                DiagnosticInfo diagnosticInfo = new DiagnosticInfo(error[0], error[1], kind);
                                OpenAPIDiagnostics diagnostics =
                                        new OpenAPIDiagnostics(openApiMissingError.getPosition(), diagnosticInfo,
                                                error[1]);
                                validations.add(diagnostics);
//                                dLog.logDiagnostic(kind, serviceNode.getPosition(),
//                                        ErrorMessages.unimplementedOpenAPIOperationsForPath(openApiMissingError.
//                                                        getServiceOperation(),
//                                                openApiMissingError.getServicePath()));
                            }
                        }
                        // Clean the undocumented openapi contract functions
                        openAPIPathSummaries = ResourceWithOperation.removeUndocumentedPath(openAPIPathSummaries,
                                        openApiMissingServiceMethod);
                    }
                    // Check all the documented resource functions are in openapi contract
                    List<ResourceValidationError> resourceValidationErrors =
                            ResourceWithOperation.checkResourceHasOperation(openAPIPathSummaries, resourcePathSummaryMap);
                    // Clean the undocumented resources from the list
                    if (!resourcePathSummaryMap.isEmpty()) {
                        createListResourcePathSummary(resourceValidationErrors, resourcePathSummaryMap);
                    }

                    // Resource against to operation
                    for (Map.Entry<String, ResourcePathSummary> resourcePath: resourcePathSummaryMap.entrySet()) {
                        for (OpenAPIPathSummary openApiPath : openAPIPathSummaries) {
                            if ((resourcePath.getKey().equals(openApiPath.getPath())) &&
                                    (!resourcePath.getValue().getMethods().isEmpty())) {
                                Map<String, ResourceMethod> resourceMethods = resourcePath.getValue().getMethods();
                                for (Map.Entry<String, ResourceMethod> method: resourceMethods.entrySet()) {
                                    for (Map.Entry<String, Operation> operation: openApiPath.getOperations().entrySet()) {
                                        if (method.getKey().equals(operation.getKey())) {
                                            List<ValidationError> postErrors =
                                                    ResourceValidator.validateResourceAgainstOperation(operation.getValue(),
                                                            method.getValue(), semanticModel, syntaxTree);
                                            generateDlogMessage(kind, dLog, resourcePath.getValue(), method, postErrors);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return validations;
    }

    private static void extractProjectDetails(Project project) {

        //Travers and filter service
        //Take package name for project
        Package packageName = project.currentPackage();
        DocumentId docId;
        Document doc;
        if (project.kind().equals(ProjectKind.BUILD_PROJECT)) {
            docId = project.documentId(project.sourceRoot());
            ModuleId moduleId = docId.moduleId();
            doc = project.currentPackage().module(moduleId).document(docId);
        } else {
            // Take module instance for traversing the syntax tree
            Module currentModule = packageName.getDefaultModule();
            Iterator<DocumentId> documentIterator = currentModule.documentIds().iterator();

            docId = documentIterator.next();
            doc = currentModule.document(docId);
        }
        syntaxTree = doc.syntaxTree();
        semanticModel =  project.currentPackage().getCompilation().getSemanticModel(docId.moduleId());
    }

    private static DiagnosticSeverity extractOpenAPIAnnotation(Project project, DiagnosticSeverity kind,
                                                               Filters filters,
                                                               SeparatedNodeList<MappingFieldNode> fields)
            throws IOException {

        for (MappingFieldNode fieldNode: fields) {
            if (fieldNode instanceof SpecificFieldNode) {
                SpecificFieldNode specificFieldNode = (SpecificFieldNode) fieldNode;
                Optional<ExpressionNode> expressionNode = specificFieldNode.valueExpr();
                //Handle openapi contract path if path is empty return exceptions.
                ExpressionNode openAPIAnnotation = expressionNode.orElseThrow();
                if (specificFieldNode.fieldName().toString().trim().equals("contract")) {
                    Path sourceRoot = project.sourceRoot();
                    Path openapiPath = Paths.get(openAPIAnnotation.toString().replaceAll("\"","").trim());
                    Path relativePath;
                    if (Paths.get(openapiPath.toString()).isAbsolute()) {
                        relativePath = Paths.get(openapiPath.toString());
                    } else {
                        File file = new File(sourceRoot.toString());
                        File parentFolder = new File(file.getParent());
                        File openapiContract = new File(parentFolder, openapiPath.toString());
                        relativePath = Paths.get(openapiContract.getCanonicalPath());
                    }
                    if (Files.exists(relativePath)) {
                        try {
                            openAPI = ServiceValidator.parseOpenAPIFile(relativePath.toString());
                        } catch (OpenApiValidatorException e) {
                            e.printStackTrace();
                        }
                    } else {
                        String[] error = ErrorMessages.contactFileMissinginPath();
                        DiagnosticInfo diagnosticInfo = new DiagnosticInfo(error[0], error[1], DiagnosticSeverity.ERROR);
                        OpenAPIDiagnostics diagnostics = new OpenAPIDiagnostics(fieldNode.location(), diagnosticInfo,
                                        error[1]);
                        validations.add(diagnostics);
                    }
                } else if (specificFieldNode.fieldName().toString().trim().equals("failOnErrors")) {
                    String failOnErrors = openAPIAnnotation.toString();
                    if (failOnErrors.trim().equals("false")) {
                        kind = DiagnosticSeverity.WARNING;
                        filters.setKind(kind);
                    }
                } else if (openAPIAnnotation instanceof ListConstructorExpressionNode) {
                    ListConstructorExpressionNode list = (ListConstructorExpressionNode) openAPIAnnotation;
                    String attributeName = specificFieldNode.fieldName().toString().trim();
                    switch (attributeName) {
                        case "tags":
                            List<String> tags = setFilters(list);
                            filters.setTag(tags);
                            break;
                        case "excludeTags":
                            List<String> eTags = setFilters(list);
                            filters.setExcludeTag(eTags);
                            break;
                        case "operatoins":
                        case "excludeOperations":
                            break;
                        default:
                            break;
                    }
                    //handle filtervalues
                }
            }
        }
        return kind;
    }

    private static List<String> setFilters(ListConstructorExpressionNode list) {

        SeparatedNodeList<Node> expressions = list.expressions();
        Iterator<Node> iterator = expressions.iterator();
        List<String> tags = new ArrayList<>();
        while (iterator.hasNext()) {
            Node item = iterator.next();
            tags.add(item.toString());
        }
        return tags;
    }

    /**
     * Fix the list with openAPIPathSummary by matching resources that documented for validating.
     * @param openAPIPathSummaries      summary list with OpenAPI Path
     * @param resourcePathSummaryList   summary list with resourcePath
     */
    private static void createListOperations(List<OpenAPIPathSummary> openAPIPathSummaries,
                                             List<ResourcePathSummary> resourcePathSummaryList) {

        Iterator<ResourcePathSummary> resourcePathSummaryIterator = resourcePathSummaryList.iterator();
        while (resourcePathSummaryIterator.hasNext()) {
            boolean isExit = false;
            ResourcePathSummary resourcePathSummary = resourcePathSummaryIterator.next();
            for (OpenAPIPathSummary apiPathSummary: openAPIPathSummaries) {
                isExit = true;
                if (!(resourcePathSummary.getMethods().isEmpty()) && !(apiPathSummary.getOperations().isEmpty())
                        && (resourcePathSummary.getPath().equals(apiPathSummary.getPath()))) {
                    Iterator<Map.Entry<String, ResourceMethod>> methods =
                            resourcePathSummary.getMethods().entrySet().iterator();
                    while (methods.hasNext()) {
                        boolean isMethodExit = false;
                        Map.Entry<String, ResourceMethod> reMethods = methods.next();
                        Map<String, Operation> operations = apiPathSummary.getOperations();
                        for (Map.Entry<String, Operation> operation: operations.entrySet()) {
                            if (reMethods.getKey().equals(operation.getKey())) {
                                isMethodExit = true;
                                break;
                            }
                        }
                        if (!isMethodExit) {
                            methods.remove();
                        }
                    }
                }
                break;
            }
            if (!isExit) {
                resourcePathSummaryIterator.remove();
            }
        }
    }

    /**
     *  Fix the list with ResourcePathSummary for validate by removing undocumented path and method.
     * @param resourceMissingPathMethod     list with missing Path and methods
     * @param resourcePathSummaryList       list with all documented services in ballerina file
     */

    private static void createListResourcePathSummary(List<ResourceValidationError> resourceMissingPathMethod,
                                                      Map<String, ResourcePathSummary> resourcePathSummaryList) {

        Iterator<Map.Entry<String, ResourcePathSummary>>
                resourcePSIterator = resourcePathSummaryList.entrySet().iterator();
        while (resourcePSIterator.hasNext()) {
            ResourcePathSummary resourcePathSummary = (ResourcePathSummary) resourcePSIterator.next();
            if (!resourceMissingPathMethod.isEmpty()) {
                for (ResourceValidationError resourceValidationError: resourceMissingPathMethod) {
                    if (resourcePathSummary.getPath().equals(resourceValidationError.getResourcePath())) {
                        if ((!resourcePathSummary.getMethods().isEmpty())
                                && (resourceValidationError.getresourceMethod() != null)) {
                            Map<String, ResourceMethod> resourceMethods = resourcePathSummary.getMethods();
                            resourceMethods.entrySet().removeIf(resourceMethod -> resourceMethod.getKey()
                                    .equals(resourceValidationError.getresourceMethod()));
                        } else if (resourceValidationError.getresourceMethod() == null) {
                            resourcePSIterator.remove();
                        }
                    }
                }
            }
        }
    }

    /**
     *  This for generate Dlog message with relevant type of errors.
     * @param kind                  message type ned to display
     * @param dLog                  diagnosticLog
     * @param resourcePathSummary   current validate ResourcePath Object
     * @param method                validate method
     * @param postErrors            list of validationErrors
     */
    private static void generateDlogMessage(DiagnosticSeverity kind, DiagnosticLog dLog,
                                            ResourcePathSummary resourcePathSummary,
                                            Map.Entry<String, ResourceMethod> method,
                                            List<ValidationError> postErrors) {

        if (!postErrors.isEmpty()) {
            for (ValidationError postErr : postErrors) {
                if (postErr instanceof TypeMismatch) {
                    generateTypeMisMatchDlog(kind, dLog, resourcePathSummary, method, postErr);
                } else if (postErr instanceof MissingFieldInJsonSchema) {
                    String[] error =
                            ErrorMessages.undocumentedFieldInRecordParam(postErr.getFieldName(),
                                    ((MissingFieldInJsonSchema) postErr).getRecordName(),
                                    method.getKey(), resourcePathSummary.getPath());
                    DiagnosticInfo diagnosticInfo = new DiagnosticInfo(error[0], error[1], kind);
                    OpenAPIDiagnostics diagnostics =
                            new OpenAPIDiagnostics(method.getValue().getMethodPosition(), diagnosticInfo,
                                    error[1]);
                    validations.add(diagnostics);
                } else if (postErr instanceof OneOfTypeValidation) {
                    if (!(((OneOfTypeValidation) postErr).getBlockErrors()).isEmpty()) {
                        List<ValidationError> oneOferrorlist =
                                ((OneOfTypeValidation) postErr).getBlockErrors();
                        for (ValidationError oneOfvalidation : oneOferrorlist) {
                            if (oneOfvalidation instanceof TypeMismatch) {
                                generateTypeMisMatchDlog(kind, dLog, resourcePathSummary, method, oneOfvalidation);
                            } else if (oneOfvalidation instanceof MissingFieldInJsonSchema) {
                                String[] error =
                                        ErrorMessages.undocumentedFieldInRecordParam(oneOfvalidation.getFieldName(),
                                                ((MissingFieldInJsonSchema) oneOfvalidation).getRecordName(),
                                                method.getKey(), resourcePathSummary.getPath());
                                DiagnosticInfo diagnosticInfo = new DiagnosticInfo(error[0], error[1], kind);
                                OpenAPIDiagnostics diagnostics =
                                        new OpenAPIDiagnostics(method.getValue().getMethodPosition(), diagnosticInfo,
                                                error[1]);
                                validations.add(diagnostics);
//                                dLog.logDiagnostic(kind, method.getValue().getMethodPosition(),
//                                        ErrorMessages.undocumentedFieldInRecordParam(oneOfvalidation.getFieldName(),
//                                                ((MissingFieldInJsonSchema) oneOfvalidation).getRecordName(),
//                                                method.getKey(), resourcePathSummary.getPath()));
                            }
                        }
                    }
                } else if (!(postErr instanceof MissingFieldInBallerinaType)) {
                    String[] error =
                            ErrorMessages.undocumentedResourceParameter(postErr.getFieldName(),
                                    method.getKey(), resourcePathSummary.getPath());
                    DiagnosticInfo diagnosticInfo = new DiagnosticInfo(error[0], error[1], kind);
                    OpenAPIDiagnostics diagnostics =
                            new OpenAPIDiagnostics(postErr.getParameterPos(), diagnosticInfo,
                                    error[1]);
                    validations.add(diagnostics);
//                    dLog.logDiagnostic(kind, postErr.getParameterPos(),
//                            ErrorMessages.undocumentedResourceParameter(postErr.getFieldName(),
//                            method.getKey(), resourcePathSummary.getPath()));
                }
            }
        }
    }

    /**
     *  This for finding out the kind of TypeMisMatching.
     * @param kind                  message type to need to display
     * @param dLog                  Dlog
     * @param resourcePathSummary   current validating resourcePath
     * @param method                current validating method
     * @param postErr               TypeMisMatchError type validation error
     */
    private static void generateTypeMisMatchDlog(DiagnosticSeverity kind, DiagnosticLog dLog,
                                                 ResourcePathSummary resourcePathSummary,
                                                 Map.Entry<String, ResourceMethod> method, ValidationError postErr) {

        if (postErr instanceof TypeMismatch) {
            if (((TypeMismatch) postErr).getRecordName() != null) {
                String[] error =
                        ErrorMessages.typeMismatchingRecord(postErr.getFieldName(),
                                ((TypeMismatch) postErr).getRecordName(),
                                TypeSymbolToJsonValidatorUtil.convertEnumTypetoString((
                                        (TypeMismatch) postErr).getTypeJsonSchema()),
                                TypeSymbolToJsonValidatorUtil.convertEnumTypetoString(((
                                        TypeMismatch) postErr).getTypeBallerinaType()), method.getKey(),
                                resourcePathSummary.getPath());
                DiagnosticInfo diagnosticInfo = new DiagnosticInfo(error[0], error[1], kind);
                OpenAPIDiagnostics diagnostics =
                        new OpenAPIDiagnostics(method.getValue().getMethodPosition(), diagnosticInfo,
                                error[1]);
                validations.add(diagnostics);
            } else {
                String[] error =
                        ErrorMessages.typeMismatching(postErr.getFieldName(),
                                TypeSymbolToJsonValidatorUtil.convertEnumTypetoString
                                        (((TypeMismatch) postErr).getTypeJsonSchema()),
                                TypeSymbolToJsonValidatorUtil.convertEnumTypetoString
                                        (((TypeMismatch) postErr).getTypeBallerinaType()), method.getKey(),
                                resourcePathSummary.getPath());
                DiagnosticInfo diagnosticInfo = new DiagnosticInfo(error[0], error[1], kind);
                OpenAPIDiagnostics diagnostics =
                        new OpenAPIDiagnostics(method.getValue().getMethodPosition(), diagnosticInfo,
                                error[1]);
                validations.add(diagnostics);
            }
        }
    }

    /**
     * Parse and get the {@link OpenAPI} for the given OpenAPI contract.
     *
     * @param definitionURI     URI for the OpenAPI contract
     * @return {@link OpenAPI}  OpenAPI model
     * @throws OpenApiValidatorException in case of exception
     */
    public static OpenAPI parseOpenAPIFile(String definitionURI) throws OpenApiValidatorException, IOException {
        Path contractPath = Paths.get(definitionURI);
        ParseOptions parseOptions = new ParseOptions();
        parseOptions.setResolve(true);
        parseOptions.setResolveFully(true);
        // Enable this if you need to create new schema from references
//        parseOptions.setFlatten(true);

        if (!Files.exists(contractPath)) {
            throw new OpenApiValidatorException(ErrorMessages.invalidFilePath(definitionURI)[1]);
        }
        if (!(definitionURI.endsWith(".yaml") || definitionURI.endsWith(".json"))) {
            throw new OpenApiValidatorException(ErrorMessages.invalidFile()[1]);
        }
        String openAPIFileContent = Files.readString(Paths.get(definitionURI));
        SwaggerParseResult parseResult = new OpenAPIV3Parser().readContents(openAPIFileContent, null, parseOptions);
        OpenAPI api = parseResult.getOpenAPI();
        if (api == null) {
            throw new OpenApiValidatorException(ErrorMessages.parserException(definitionURI)[1]);
        }
        return api;
    }

    public static List<Diagnostic> getValidations() {

        return validations;
    }
}
