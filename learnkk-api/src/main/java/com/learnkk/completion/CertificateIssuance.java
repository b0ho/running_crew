package com.learnkk.completion;

/**
 * 발급 대상 수료증 데이터(멘티 id + 저장된 이미지 경로).
 *
 * <p>비트랜잭션 오케스트레이션(CompletionService)에서 이미지 생성·저장 후 만들어 트랜잭션 빈(CompletionWriter)으로 넘기는 값 객체다. JSON
 * 직렬화 대상이 아니므로 dto 패키지가 아닌 도메인 패키지에 둔다.
 */
public record CertificateIssuance(Long menteeId, String imagePath) {}
