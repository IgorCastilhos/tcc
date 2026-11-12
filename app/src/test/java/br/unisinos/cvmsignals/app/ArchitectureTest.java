package br.unisinos.cvmsignals.app;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Garante o núcleo da Clean Architecture (Seção 4.2): domínio e camada de
 * aplicação de todos os bounded contexts permanecem livres de framework —
 * as dependências apontam sempre para dentro.
 */
@AnalyzeClasses(packages = "br.unisinos.cvmsignals", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    private static final String[] FRAMEWORK_PACKAGES = {
            "org.springframework..",
            "org.springframework.ai..",
            "jakarta.persistence..",
            "org.apache.kafka..",
            "org.apache.tika..",
            "com.fasterxml.jackson.."
    };

    @ArchTest
    static final ArchRule dominioPuro = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAnyPackage(FRAMEWORK_PACKAGES)
            .because("o domínio conhece apenas o JDK");

    @ArchTest
    static final ArchRule aplicacaoPura = noClasses()
            .that().resideInAPackage("..application..")
            .should().dependOnClassesThat().resideInAnyPackage(FRAMEWORK_PACKAGES)
            .because("casos de uso dependem apenas de portas, nunca de infraestrutura");

    @ArchTest
    static final ArchRule dominioNaoDependeDeAplicacao = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAnyPackage("..application..", "..infrastructure..")
            .because("dependências apontam para dentro");
}
