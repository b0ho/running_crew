package com.learnkk.attendance;

import org.springframework.core.io.Resource;

/**
 * 증빙 다운로드 스트리밍 핸들 (business-logic-model.md §4, security-design.md §2).
 *
 * <p>컨트롤러가 스트리밍 응답을 구성하는 데 필요한 리소스·정확한 Content-Type·안전한 다운로드 파일명·크기를 담는다. JSON 직렬화 대상이 아니므로 dto
 * 패키지가 아닌 도메인 패키지에 둔다.
 */
public record EvidenceDownload(Resource resource, String mimeType, String filename, long size) {}
