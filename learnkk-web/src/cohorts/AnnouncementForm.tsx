import { useState } from 'react';
import type { FormEvent } from 'react';
import type { AnnouncementCreateRequest } from '../api/types';

/**
 * 공지 작성 폼 (frontend-components §2.4). body 필수, externalLink 선택(URL).
 *
 * 서버 검증(R-U2-16/17, @SafeExternalUrl)이 권위이며 클라이언트 검증은 보조.
 */
export function AnnouncementForm({
  submitting,
  serverError,
  onSubmit,
}: {
  submitting: boolean;
  serverError?: string | null;
  onSubmit: (req: AnnouncementCreateRequest) => void;
}) {
  const [body, setBody] = useState('');
  const [externalLink, setExternalLink] = useState('');
  const [clientError, setClientError] = useState<string | null>(null);

  function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setClientError(null);
    if (!body.trim()) {
      setClientError('공지 본문을 입력하세요');
      return;
    }
    onSubmit({ body: body.trim(), externalLink: externalLink.trim() || null });
    setBody('');
    setExternalLink('');
  }

  const error = clientError ?? serverError ?? null;

  return (
    <form onSubmit={handleSubmit} data-testid="announcement-form" noValidate className="space-y-3">
      <div>
        <label htmlFor="announcement-body" className="block text-sm font-medium">
          공지 본문
        </label>
        <textarea
          id="announcement-body"
          data-testid="announcement-body"
          value={body}
          onChange={(e) => setBody(e.target.value)}
          aria-describedby={error ? 'announcement-error' : undefined}
          className="mt-1 w-full rounded border border-gray-300 px-3 py-2"
        />
      </div>
      <div>
        <label htmlFor="announcement-link" className="block text-sm font-medium">
          외부 링크 (선택)
        </label>
        <input
          id="announcement-link"
          data-testid="announcement-link"
          type="url"
          placeholder="https://..."
          value={externalLink}
          onChange={(e) => setExternalLink(e.target.value)}
          className="mt-1 w-full rounded border border-gray-300 px-3 py-2"
        />
      </div>
      {error && (
        <p id="announcement-error" role="alert" data-testid="announcement-error" className="text-sm text-red-600">
          {error}
        </p>
      )}
      <button
        type="submit"
        data-testid="announcement-submit"
        disabled={submitting}
        className="rounded bg-accent px-4 py-2 text-white hover:bg-accent-hover disabled:opacity-50"
      >
        {submitting ? '등록 중...' : '공지 등록'}
      </button>
    </form>
  );
}
