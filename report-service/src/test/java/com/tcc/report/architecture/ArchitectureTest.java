package com.tcc.report.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RestController;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class ArchitectureTest {

    private static JavaClasses importedClasses;

    @BeforeAll
    static void setup() {
        importedClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.tcc.report");
    }

    @Test
    @DisplayName("Convenção de nomenclatura por camada")
    void namingConventionShouldBeRespected() {
        classes().that().areAnnotatedWith(RestController.class)
                .should().haveSimpleNameEndingWith("Controller")
                .check(importedClasses);
        classes().that().areAnnotatedWith(Service.class)
                .should().haveSimpleNameEndingWith("Service")
                .check(importedClasses);
    }

    @Test
    @DisplayName("Apenas Service pode acessar o client HTTP do order-service")
    void onlyServiceShouldAccessOrderClient() {
        ArchRule rule = classes()
                .that().resideInAPackage("..client..")
                .should().onlyBeAccessed().byClassesThat().resideInAnyPackage("..service..", "..client..");
        rule.check(importedClasses);
    }

    @Test
    @DisplayName("[fronteira] Este serviço não deve depender de classes de outro contexto (order/product/user)")
    void shouldNotDependOnOtherServicesPackages() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("com.tcc.report..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "com.tcc.order..", "com.tcc.product..", "com.tcc.user..");
        rule.check(importedClasses);
    }
}
