package com.learnkk.arch;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import jakarta.persistence.Entity;
import org.springframework.web.bind.annotation.RestController;

/**
 * ArchUnit 경계 검증 (INV-1 / NFR-7).
 *
 * <p>컨트롤러 메서드는 JPA {@code @Entity} 타입을 반환하지 않는다(DTO 경계 강제).
 */
@AnalyzeClasses(
    packages = "com.learnkk",
    importOptions = {ImportOption.DoNotIncludeTests.class})
class ArchitectureTest {

  @ArchTest
  static final com.tngtech.archunit.lang.ArchRule 컨트롤러는_엔티티를_반환하지_않는다 =
      methods()
          .that()
          .areDeclaredInClassesThat()
          .areAnnotatedWith(RestController.class)
          .and()
          .arePublic()
          .should(notReturnJpaEntity());

  private static ArchCondition<JavaMethod> notReturnJpaEntity() {
    return new ArchCondition<>("컨트롤러 반환 타입이 @Entity 를 노출하지 않아야 한다") {
      @Override
      public void check(JavaMethod method, ConditionEvents events) {
        JavaClass rawReturnType = method.getRawReturnType();
        if (rawReturnType.isAnnotatedWith(Entity.class)) {
          events.add(
              SimpleConditionEvent.violated(
                  method,
                  String.format(
                      "%s.%s() 가 JPA 엔티티 %s 를 반환합니다",
                      method.getOwner().getSimpleName(),
                      method.getName(),
                      rawReturnType.getSimpleName())));
        }
      }
    };
  }
}
