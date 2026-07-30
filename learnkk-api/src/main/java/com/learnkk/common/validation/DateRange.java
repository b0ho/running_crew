package com.learnkk.common.validation;

import java.time.LocalDate;

/**
 * 기간(시작일·종료일)을 노출하는 요청 타입 계약.
 *
 * <p>record 컴포넌트 접근자({@code startDate()}, {@code endDate()})가 그대로 이 인터페이스를 충족한다. {@link
 * EndDateAfterStartDate} 클래스 레벨 제약이 이 계약을 통해 두 필드를 교차 검증한다(R-U2-04).
 */
public interface DateRange {

  LocalDate startDate();

  LocalDate endDate();
}
