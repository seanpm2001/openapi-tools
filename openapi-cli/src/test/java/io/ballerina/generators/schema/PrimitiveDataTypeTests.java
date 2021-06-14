/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.ballerina.generators.schema;

import io.ballerina.compiler.syntax.tree.SyntaxTree;
import io.ballerina.generators.BallerinaSchemaGenerator;
import io.ballerina.generators.common.TestUtils;
import io.ballerina.openapi.exception.BallerinaOpenApiException;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Tests for the primitive data type.
 */
public class PrimitiveDataTypeTests {
    private static final Path RES_DIR = Paths.get("src/test/resources/generators/schema").toAbsolutePath();
    SyntaxTree syntaxTree;
    private ByteArrayOutputStream outContent;

    @BeforeTest
    public void setUp() {
        outContent = new ByteArrayOutputStream();
        System.setErr(new PrintStream(outContent));
    }

    @Test(description = "Generate single record")
    public void generateScenario01() throws IOException, BallerinaOpenApiException {
        Path definitionPath = RES_DIR.resolve("swagger/scenario01.yaml");
        syntaxTree = BallerinaSchemaGenerator.generateSyntaxTree(definitionPath);
        TestUtils.compareGeneratedSyntaxTreewithExpectedSyntaxTree("schema/ballerina/schema01.bal", syntaxTree);
    }

    @Test(description = "Generate multiple record")
    public void generateScenario02() throws IOException, BallerinaOpenApiException {
        Path definitionPath = RES_DIR.resolve("swagger/scenario02.yaml");
        syntaxTree = BallerinaSchemaGenerator.generateSyntaxTree(definitionPath);
        TestUtils.compareGeneratedSyntaxTreewithExpectedSyntaxTree("schema/ballerina/schema02.bal", syntaxTree);
    }

    @Test(description = "Scenario for missing DataType", enabled = false)
    public void generateMissingDatatype() throws IOException, BallerinaOpenApiException {
        Path definitionPath = RES_DIR.resolve("swagger/missDataType.yaml");
        syntaxTree = BallerinaSchemaGenerator.generateSyntaxTree(definitionPath);
        String expected = "Unsupported OAS data type." + System.getProperty("line.separator");
        Assert.assertTrue(outContent.toString().contains(expected));
    }

    @AfterTest
    public void clean() {
        System.setErr(null);
    }
}
