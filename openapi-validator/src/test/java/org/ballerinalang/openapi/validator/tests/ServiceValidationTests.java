/*
 * Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ballerinalang.openapi.validator.tests;

import io.ballerina.projects.Project;
import io.ballerina.tools.diagnostics.DiagnosticSeverity;
import io.swagger.v3.oas.models.OpenAPI;
import org.ballerinalang.openapi.validator.Filters;
import org.ballerinalang.openapi.validator.OpenApiValidatorException;
import org.ballerinalang.openapi.validator.ServiceValidator;
import org.ballerinalang.util.diagnostic.DiagnosticLog;
import org.testng.annotations.Test;
import org.wso2.ballerinalang.compiler.tree.BLangPackage;
import org.wso2.ballerinalang.compiler.tree.BLangService;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Test for serviceValidation.
 */
public class ServiceValidationTests {
    private static final Path RES_DIR = Paths.get("src/test/resources/project-based-tests/modules/serviceValidator/")
            .toAbsolutePath();
    private OpenAPI api;
    private Project project;
    private BLangPackage bLangPackage;
    private BLangService extractBLangservice;
    private List<String> tag = new ArrayList<>();
    private List<String> operation = new ArrayList<>();
    private List<String> excludeTag = new ArrayList<>();
    private List<String> excludeOperation = new ArrayList<>();
    private DiagnosticSeverity kind;
    private DiagnosticLog dLog;
    private Filters filters;


    @Test(enabled = true, description = "test for undocumented Path in contract")
    public void testUndocumentedPath() throws OpenApiValidatorException, IOException {
//        Path contractPath = RES_DIR.resolve("swagger/valid/petstore.yaml");
//        api = ServiceValidator.parseOpenAPIFile(contractPath.toString());
        project = ValidatorTest.getProject(RES_DIR.resolve("ballerina/valid/petstore.bal"));
        ServiceValidator.validateResourceFunctions(project);
    }

    @Test(enabled = false, description = "test for undocumented Method in contract")
    public void testUndocumentedMethod() throws OpenApiValidatorException, IOException {
        Path contractPath = RES_DIR.resolve("swagger/valid/petstoreMethod.yaml");
        api = ServiceValidator.parseOpenAPIFile(contractPath.toString());
        bLangPackage = ValidatorTest.getBlangPackage("serviceValidator/ballerina/valid/petstoreMethod.bal");
        extractBLangservice = ValidatorTest.getServiceNode(bLangPackage);
        kind = DiagnosticSeverity.ERROR;
//        dLog = ValidatorTest.getDiagnostic("serviceValidator/ballerina/valid/petstoreMethod.bal");
        filters = new Filters(tag, excludeTag, operation, excludeOperation, kind);
//        ServiceValidator.validateResource(api, extractBLangservice, filters, kind, dLog);
    }

    @Test(description = "test for Path parameter")
    public void testPathparameter() throws OpenApiValidatorException, IOException {
//        Path contractPath = RES_DIR.resolve("swagger/valid/petstore.yaml");
//        api = ServiceValidator.parseOpenAPIFile(contractPath.toString());
        project = ValidatorTest.getProject(RES_DIR.resolve("ballerina/valid/petstore.bal"));
        ServiceValidator.validateResourceFunctions(project);
    }

    @Test(enabled = false, description = "test for undocumented TypeMisMatch in contract")
    public void testParameterTypeMismatch() throws OpenApiValidatorException, IOException {
        Path contractPath = RES_DIR.resolve("swagger/valid/petstoreParameterTM.yaml");
        api = ServiceValidator.parseOpenAPIFile(contractPath.toString());
        bLangPackage = ValidatorTest.getBlangPackage("serviceValidator/ballerina/valid/petstoreParameterTM.bal");
        extractBLangservice = ValidatorTest.getServiceNode(bLangPackage);
        kind = DiagnosticSeverity.ERROR;
//        dLog = ValidatorTest.getDiagnostic("serviceValidator/ballerina/valid/petstoreParameterTM.bal");
        filters = new Filters(tag, excludeTag, operation, excludeOperation, kind);
//        ServiceValidator.validateResource(api, extractBLangservice, filters, kind, dLog);
    }

    @Test(enabled = false, description = "test for undocumented TypeMisMatch in contract")
    public void testRecordTypeMismatch() throws OpenApiValidatorException, IOException {
        Path contractPath = RES_DIR.resolve("swagger/invalid/petstoreRecordType.yaml");
        api = ServiceValidator.parseOpenAPIFile(contractPath.toString());
        bLangPackage = ValidatorTest.getBlangPackage("serviceValidator/ballerina/invalid/petstoreRecordType.bal");
        extractBLangservice = ValidatorTest.getServiceNode(bLangPackage);
        kind = DiagnosticSeverity.ERROR;
//        dLog = ValidatorTest.getDiagnostic("serviceValidator/ballerina/invalid/petstoreRecordType.bal");
        filters = new Filters(tag, excludeTag, operation, excludeOperation, kind);
//        ServiceValidator.validateResource(api, extractBLangservice, filters, kind, dLog);
    }
    @Test(enabled = false, description = "test for undocumented record field  in contract")
    public void testRecordFieldMiss() throws OpenApiValidatorException, IOException {
        Path contractPath = RES_DIR.resolve("swagger/invalid/petstoreRecordFieldMiss.yaml");
        api = ServiceValidator.parseOpenAPIFile(contractPath.toString());
        bLangPackage = ValidatorTest.getBlangPackage("serviceValidator/ballerina/invalid/petstoreRecordFieldMiss.bal");
        extractBLangservice = ValidatorTest.getServiceNode(bLangPackage);
        kind = DiagnosticSeverity.ERROR;
//        dLog = ValidatorTest.getDiagnostic("serviceValidator/ballerina/invalid/petstoreRecordFieldMiss.bal");
        filters = new Filters(tag, excludeTag, operation, excludeOperation, kind);
//        ServiceValidator.validateResource(api, extractBLangservice, filters, kind, dLog);
    }

    @Test(enabled = false, description = "test for undocumented path parameter  in contract")
    public void testPathParameter() throws OpenApiValidatorException, IOException {
        Path contractPath = RES_DIR.resolve("swagger/invalid/petstorePathParameter.yaml");
        api = ServiceValidator.parseOpenAPIFile(contractPath.toString());
        bLangPackage = ValidatorTest.getBlangPackage("serviceValidator/ballerina/invalid/petstorePathParameter.bal");
        extractBLangservice = ValidatorTest.getServiceNode(bLangPackage);
        kind = DiagnosticSeverity.ERROR;
//        dLog = ValidatorTest.getDiagnostic("serviceValidator/ballerina/invalid/petstorePathParameter.bal");
        filters = new Filters(tag, excludeTag, operation, excludeOperation, kind);
//        ServiceValidator.validateResource(api, extractBLangservice, filters, kind, dLog);
    }

    @Test(enabled = false, description = "test for undocumented field oneOf type record in contract")
    public void testOneofscenario_01() throws OpenApiValidatorException, IOException {
        Path contractPath = RES_DIR.resolve("swagger/invalid/oneOf.yaml");
        api = ServiceValidator.parseOpenAPIFile(contractPath.toString());
        bLangPackage = ValidatorTest.getBlangPackage("serviceValidator/ballerina/invalid/oneOf.bal");
        extractBLangservice = ValidatorTest.getServiceNode(bLangPackage);
        kind = DiagnosticSeverity.ERROR;
//        dLog = ValidatorTest.getDiagnostic("serviceValidator/ballerina/invalid/oneOf.bal");
        filters = new Filters(tag, excludeTag, operation, excludeOperation, kind);
//        ServiceValidator.validateResource(api, extractBLangservice, filters, kind, dLog);
    }

    @Test(enabled = false, description = "test for scenario 02")
    public void testOneofscenario_02() throws OpenApiValidatorException, IOException {
        Path contractPath = RES_DIR.resolve("swagger/invalid/oneOf-scenario02.yaml");
        api = ServiceValidator.parseOpenAPIFile(contractPath.toString());
        bLangPackage = ValidatorTest.getBlangPackage("serviceValidator/ballerina/invalid/oneOf-scenario02.bal");
        extractBLangservice = ValidatorTest.getServiceNode(bLangPackage);
        kind = DiagnosticSeverity.ERROR;
//        dLog = ValidatorTest.getDiagnostic("serviceValidator/ballerina/invalid/oneOf-scenario02.bal");
        filters = new Filters(tag, excludeTag, operation, excludeOperation, kind);
//        ServiceValidator.validateResource(api, extractBLangservice, filters, kind, dLog);
    }

    @Test(enabled = false, description = "test for scenario 03")
    public void testOneofscenario_03() throws OpenApiValidatorException, IOException {
        Path contractPath = RES_DIR.resolve("swagger/invalid/oneOf-scenario03.yaml");
        api = ServiceValidator.parseOpenAPIFile(contractPath.toString());
        bLangPackage = ValidatorTest.getBlangPackage("serviceValidator/ballerina/invalid/oneOf-scenario03.bal");
        extractBLangservice = ValidatorTest.getServiceNode(bLangPackage);
        kind = DiagnosticSeverity.ERROR;
//        dLog = ValidatorTest.getDiagnostic("serviceValidator/ballerina/invalid/oneOf-scenario03.bal");
        filters = new Filters(tag, excludeTag, operation, excludeOperation, kind);
//        ServiceValidator.validateResource(api, extractBLangservice, filters, kind, dLog);
    }
    /**
     * OneOf - Invalid Scenario examples
     */
    //      Scenario-01         (record)   cat - place02, mealType        | (schema) cat - place, mealType
    //      Scenario-02         (record)   cat - place, mealType, canFly  | (schema) cat - place, mealType
    //      Scenario-03         (record)   cat - place, mealType          | (schema) cat - place, mealType , mealTime
}
