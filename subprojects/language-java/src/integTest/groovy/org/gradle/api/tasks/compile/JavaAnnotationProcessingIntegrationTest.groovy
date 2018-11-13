/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.api.tasks.compile

import org.gradle.api.JavaVersion
import org.gradle.api.internal.tasks.compile.CompileWithAnnotationProcessingBuildOperationType.Result.AnnotationProcessorDetails
import org.gradle.integtests.fixtures.AbstractIntegrationSpec
import org.gradle.integtests.fixtures.AvailableJavaHomes
import org.gradle.language.fixtures.CompileWithAnnotationProcessingBuildOperationsFixture
import org.gradle.language.fixtures.HelperProcessorFixture
import org.gradle.util.TextUtil
import spock.lang.Issue
import spock.lang.Unroll

class JavaAnnotationProcessingIntegrationTest extends AbstractIntegrationSpec {

    def fixture = new HelperProcessorFixture()
    def operations = new CompileWithAnnotationProcessingBuildOperationsFixture(executer, testDirectoryProvider)

    def annotationProjectDir = file("annotation")
    def processorProjectDir = file("processor")

    def setup() {
        settingsFile << """
            include "annotation"
            include "processor"
        """

        buildFile << """
            apply plugin: 'java'
        """

        annotationProjectDir.file("build.gradle") << """
            apply plugin: "java"
        """

        processorProjectDir.file("build.gradle") << """
            apply plugin: "java"
            dependencies {
                compile project(':annotation')
            }
        """

        // A library class used by processor at runtime, but not the generated classes
        fixture.writeSupportLibraryTo(processorProjectDir)

        // The processor and annotation
        fixture.writeApiTo(annotationProjectDir)
        fixture.writeAnnotationProcessorTo(processorProjectDir)

        // The class that is the target of the processor
        file('src/main/java/TestApp.java') << '''
            @Helper
            class TestApp { 
                public static void main(String[] args) {
                    System.out.println(new TestAppHelper().getValue()); // generated class
                }
            }
        '''
    }

    def "can specify generated source directory"() {
        buildFile << """
            dependencies {
                compileOnly project(":annotation")
                annotationProcessor project(":processor")
            }
            compileJava.options.annotationProcessorGeneratedSourcesDirectory = file("build/generated-sources")
        """

        expect:
        succeeds "compileJava"
        file("build/generated-sources/TestAppHelper.java").text == 'class TestAppHelper {    String getValue() { return "greetings"; }}'
    }

    def "generated sources are cleaned up on full compilations"() {
        given:
        buildFile << """
            dependencies {
                compileOnly project(":annotation")
                annotationProcessor project(":processor")
            }
            compileJava.options.annotationProcessorGeneratedSourcesDirectory = file("build/generated-sources")
        """
        succeeds "compileJava"

        when:
        buildFile << """compileJava.options.annotationProcessorPath = files()"""
        fails("compileJava")

        then:
        file("build/generated-sources/TestAppHelper.java").assertDoesNotExist()
    }

    def "can model annotation processor arguments"() {
        buildFile << """                                                       
            class HelperAnnotationProcessor implements CommandLineArgumentProvider {
                @Input
                String message
                
                HelperAnnotationProcessor(String message) {
                    this.message = message
                }
                
                @Override
                List<String> asArguments() {
                    ["-Amessage=\${message}".toString()]
                }
            }
            
            dependencies {
                compileOnly project(":annotation")
                annotationProcessor project(":processor")
            }
            
            compileJava.options.compilerArgumentProviders << new HelperAnnotationProcessor("fromOptions")
        """

        when:
        run "compileJava"

        then:
        file("build/classes/java/main/TestAppHelper.java").text == 'class TestAppHelper {    String getValue() { return "fromOptions"; }}'
    }

    def "processors in the compile classpath are ignored"() {

        buildFile << """
            dependencies {
                compile project(":processor")
            }
        """

        when:
        fails('compileJava')

        then:
        failureCauseContains('Compilation failed')
        file('build/classes/java/main/TestAppHelper.class').assertDoesNotExist()
        operations[':compileJava'].failure.contains('Compilation failed')
    }

    def "empty processor path overrides processors in the compile classpath, and no deprecation warning is emitted"() {
        buildFile << """
            dependencies {
                compile project(":processor")
            }
            
            compileJava {
              options.annotationProcessorPath = files()
            }
        """

        file('src/main/java/TestApp.java').text = '''
            @Helper
            class TestApp {
                public static void main(String[] args) {
                    System.out.println("Hello world!");
                }
            }
        '''

        expect:
        succeeds "compileJava"
        !file('build/classes/java/main/TestAppHelper.class').exists()
    }

    def "empty custom processor configuration overrides processors in the compile classpath, and no deprecation warning is emitted"() {
        buildFile << """
            configurations {
                apt
            }
            
            dependencies {
                compile project(":processor")
            }
            
            compileJava {
              options.annotationProcessorPath = configurations.apt
            }
        """

        file('src/main/java/TestApp.java').text = '''
            @Helper
            class TestApp {
                public static void main(String[] args) {
                    System.out.println("Hello world!");
                }
            }
        '''

        expect:
        succeeds "compileJava"
        !file('build/classes/java/main/TestAppHelper.class').exists()
    }

    def "processors in the compile classpath don't emit deprecation warning if processing is disabled"() {
        buildFile << """
            dependencies {
                compile project(":processor")
            }
            compileJava {
                options.compilerArgs << "-proc:none"
            }
        """
        removeUseOfGeneratedClass()

        expect:
        succeeds "compileJava"
        !file('build/classes/java/main/TestAppHelper.class').exists()
        operations[':compileJava'].result.annotationProcessorDetails == []
    }

    def "no code generation when annotation processing is disabled"() {
        buildFile << """
            dependencies {
                compileOnly project(":annotation")
                annotationProcessor project(":processor")
            }
            compileJava {
                options.compilerArgs << "-proc:none"
            }
        """
        removeUseOfGeneratedClass()

        expect:
        succeeds "compileJava"
        !file('build/classes/java/main/TestAppHelper.class').exists()
        with(operations[':compileJava'].result.annotationProcessorDetails as List<AnnotationProcessorDetails>) {
            size() == 1
            first().className == 'HelperProcessor'
            first().executionTimeInMillis == 0
        }
    }

    def "explicit -processor option overrides automatic detection"() {
        buildFile << """
            dependencies {
                compileOnly project(":annotation")
                annotationProcessor project(":processor")
            }
            compileJava.options.compilerArgs << "-processor" << "unknown.Processor"
        """

        expect:
        fails("compileJava")
        failure.assertHasErrorOutput("Annotation processor 'unknown.Processor' not found")
    }

    @Issue("https://github.com/gradle/gradle/issues/5448")
    def "can add classes directory as source"() {
        // This is sometimes done for IDE support.
        // We should deprecate this behaviour, since output directories are added as inputs.
        buildFile << """
            dependencies {
                compileOnly project(":annotation")
                annotationProcessor project(":processor")
            }
            sourceSets.main.java.srcDir("build/classes/java/main")
        """

        expect:
        succeeds "compileJava"

        when:
        file('src/main/java/TestApp.java').text = '''
            @Helper
            class TestApp { 
                public static void main(String[] args) {
                    System.out.println(new TestAppHelper().getValue() + "Changed!"); // generated class
                }
            }
        '''
        then:
        succeeds "compileJava"
    }

    @Unroll
    def "wraps processing in build operation (fork=#fork)"() {
        given:
        buildFile << """
            dependencies {
                compileOnly project(":annotation")
                annotationProcessor project(":processor")
            }
        """
        [buildFile, annotationProjectDir.file("build.gradle"), processorProjectDir.file("build.gradle")].each { buildFile ->
            buildFile << """
                compileJava {
                    options.fork = $fork
                    options.forkOptions.executable = '${TextUtil.escapeString(AvailableJavaHomes.getJdk(JavaVersion.current()).javacExecutable)}'
                }
            """
        }

        when:
        succeeds "compileJava"

        then:
        with(operations[':annotation:compileJava']) {
            it.displayName == 'Invoke compiler for :annotation:compileJava'
            it.result.annotationProcessorDetails == (fork ? null : [])
        }
        with(operations[':processor:compileJava']) {
            it.displayName == 'Invoke compiler for :processor:compileJava'
            it.result.annotationProcessorDetails == (fork ? null : [])
        }
        with(operations[':compileJava']) {
            it.displayName == 'Invoke compiler for :compileJava'
            def details = it.result.annotationProcessorDetails as List<AnnotationProcessorDetails>
            if (fork) {
                details == null
            } else {
                with(details) {
                    size() == 1
                    first().className == 'HelperProcessor'
                    first().executionTimeInMillis >= 0
                }
            }
        }

        where:
        fork << [true, false]
    }

    private void removeUseOfGeneratedClass() {
        file('src/main/java/TestApp.java').text = '''
            @Helper
            class TestApp {
                public static void main(String[] args) {
                    System.out.println("Hello world!");
                }
            }
        '''
    }

}
