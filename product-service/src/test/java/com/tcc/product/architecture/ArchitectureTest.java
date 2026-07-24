package com.tcc.product.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import jakarta.persistence.Entity;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Repository;
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
                .importPackages("com.tcc.product");
    }

    @Test
    @DisplayName("Controllers não devem acessar Repositories diretamente")
    void controllersShouldNotAccessRepositoriesDirectly() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..controller..")
                .should().dependOnClassesThat().resideInAPackage("..repository..");
        rule.check(importedClasses);
    }

    @Test
    @DisplayName("Controllers não devem depender de entidades JPA")
    void controllersShouldNotDependOnEntities() {
        ArchRule rule = noClasses()
                .that().areAnnotatedWith(RestController.class)
                .should().dependOnClassesThat().areAnnotatedWith(Entity.class);
        rule.check(importedClasses);
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
        classes().that().areAnnotatedWith(Repository.class)
                .should().haveSimpleNameEndingWith("Repository")
                .check(importedClasses);
    }

    @Test
    @DisplayName("Repositories só devem ser acessados por Services")
    void repositoriesShouldOnlyBeAccessedByServices() {
        ArchRule rule = classes()
                .that().resideInAPackage("..repository..")
                .should().onlyBeAccessed().byClassesThat().resideInAPackage("..service..");
        rule.check(importedClasses);
    }
}
