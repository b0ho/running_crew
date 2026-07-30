/** 증빙 파일 클라이언트 사전 검증 (frontend-components §2.3). 서버 검증(매직바이트·크기)이 최종 권위. */

export const MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024;
export const ALLOWED_MIME_TYPES = ['image/jpeg', 'image/png', 'application/pdf'] as const;
export const ALLOWED_EXTENSIONS = ['jpg', 'jpeg', 'png', 'pdf'] as const;
export const ACCEPT_ATTR = '.jpg,.jpeg,.png,.pdf,image/jpeg,image/png,application/pdf';

/**
 * 파일 형식·크기를 사전 검증한다. 통과하면 null, 실패하면 사용자용 한글 메시지를 반환한다.
 * (UX 보조 — 서버가 매직바이트까지 재검증한다.)
 */
export function validateEvidenceFile(file: File): string | null {
  const ext = file.name.includes('.')
    ? file.name.slice(file.name.lastIndexOf('.') + 1).toLowerCase()
    : '';
  const mimeOk = (ALLOWED_MIME_TYPES as readonly string[]).includes(file.type);
  const extOk = (ALLOWED_EXTENSIONS as readonly string[]).includes(ext);
  if (!mimeOk && !extOk) {
    return '허용되지 않는 파일 형식입니다 (jpg/png/pdf)';
  }
  if (file.size > MAX_FILE_SIZE_BYTES) {
    return '파일 크기는 최대 10MB 입니다';
  }
  if (file.size === 0) {
    return '빈 파일은 업로드할 수 없습니다';
  }
  return null;
}
