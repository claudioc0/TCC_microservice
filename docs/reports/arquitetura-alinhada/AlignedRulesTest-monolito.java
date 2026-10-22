package com.pucpr.tcc.monolith.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.EvaluationResult;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/** Conjunto ALINHADO de regras, idêntico nos dois lados do experimento (medição temporária). */
class AlignedRulesTest {

    static final String ROOT = "com.pucpr.tcc.monolith";
    /** Domínios analisados neste código e, para cada um, os pacotes dos OUTROS domínios. */
    static final String[] DOMAINS = {"com.pucpr.tcc.monolith.orders", "com.pucpr.tcc.monolith.products", "com.pucpr.tcc.monolith.users", "com.pucpr.tcc.monolith.reports"};
    static final String[] ALL_DOMAIN_ROOTS = {"com.pucpr.tcc.monolith.orders", "com.pucpr.tcc.monolith.products", "com.pucpr.tcc.monolith.users", "com.pucpr.tcc.monolith.reports"};

    @Test
    void alignedRules() {
        JavaClasses c = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS).importPackages(ROOT);
        report("A1", noClasses().that().resideInAPackage("..controller..")
                .should().dependOnClassesThat().resideInAPackage("..repository.."), c);
        report("A2", noClasses().that().resideInAPackage("..service..")
                .should().dependOnClassesThat().resideInAPackage("..controller.."), c);
        report("A3", methods().that().areDeclaredInClassesThat().resideInAPackage("..controller..")
                .and().arePublic().should(notExposeJpaEntityAsReturnType()), c);
        report("A4", noClasses().that().areAnnotatedWith(RestController.class)
                .should().dependOnClassesThat().areAnnotatedWith("jakarta.persistence.Entity"), c);
        report("A5", List.of(
                classes().that().areAnnotatedWith(RestController.class).should().haveSimpleNameEndingWith("Controller"),
                classes().that().areAnnotatedWith(Service.class).should().haveSimpleNameEndingWith("Service"),
                classes().that().areAnnotatedWith(Repository.class).should().haveSimpleNameEndingWith("Repository")), c);
        report("A6", classes().that().resideInAPackage("..repository..")
                .should().onlyBeAccessed().byClassesThat().resideInAPackage("..service.."), c);
        List<ArchRule> a7a = new ArrayList<>(), a7b = new ArrayList<>();
        for (String d : DOMAINS) {
            String[] others = Arrays.stream(ALL_DOMAIN_ROOTS).filter(o -> !o.equals(d)).toArray(String[]::new);
            String[] othersInternals = Arrays.stream(others)
                    .flatMap(o -> java.util.stream.Stream.of(o + ".entity..", o + ".repository..")).toArray(String[]::new);
            String[] othersAll = Arrays.stream(others).map(o -> o + "..").toArray(String[]::new);
            a7a.add(noClasses().that().resideInAnyPackage(d + ".service..", d + ".repository..")
                    .should().dependOnClassesThat().resideInAnyPackage(othersInternals));
            a7b.add(noClasses().that().resideInAPackage(d + "..")
                    .should().dependOnClassesThat().resideInAnyPackage(othersAll));
        }
        report("A7a", a7a, c);
        report("A7b", a7b, c);
    }

    private static void report(String id, ArchRule rule, JavaClasses c) { report(id, List.of(rule), c); }

    private static void report(String id, List<ArchRule> rules, JavaClasses c) {
        int n = 0;
        for (ArchRule r : rules) {
            EvaluationResult res = r.allowEmptyShould(true).evaluate(c);
            for (String det : res.getFailureReport().getDetails()) {
                n++;
                System.out.println("ALIGNED-DETAIL|" + id + "|" + det);
            }
        }
        System.out.println("ALIGNED|" + id + "|" + n);
    }

    private static ArchCondition<JavaMethod> notExposeJpaEntityAsReturnType() {
        return new ArchCondition<>("retornar um DTO, nunca uma entidade @Entity") {
            @Override
            public void check(JavaMethod method, ConditionEvents events) {
                Type t = method.reflect().getGenericReturnType();
                if (t instanceof ParameterizedType p) {
                    for (Type a : p.getActualTypeArguments()) {
                        if (a instanceof Class<?> k && java.util.Arrays.stream(k.getAnnotations()).anyMatch(an -> an.annotationType().getName().equals("jakarta.persistence.Entity"))) {
                            events.add(SimpleConditionEvent.violated(method, method.getFullName() + " retorna " + k.getSimpleName()));
                        }
                    }
                }
            }
        };
    }
}
