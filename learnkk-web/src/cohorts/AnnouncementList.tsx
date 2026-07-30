import type { AnnouncementDto } from '../api/types';

/** 공지 목록 (frontend-components §2.4). 최신순. 외부 링크는 새 탭 + rel="noopener noreferrer". */
export function AnnouncementList({ announcements }: { announcements: AnnouncementDto[] }) {
  if (announcements.length === 0) {
    return (
      <p data-testid="announcement-empty" className="text-sm text-gray-500">
        등록된 공지가 없습니다.
      </p>
    );
  }
  return (
    <ul data-testid="announcement-list" className="space-y-3">
      {announcements.map((a) => (
        <li
          key={a.id}
          data-testid={`announcement-item-${a.id}`}
          className="rounded border border-gray-200 p-3"
        >
          <p className="whitespace-pre-wrap text-sm">{a.body}</p>
          {a.externalLink && (
            <a
              href={a.externalLink}
              target="_blank"
              rel="noopener noreferrer"
              data-testid={`announcement-link-${a.id}`}
              className="mt-2 inline-block text-sm text-accent hover:underline"
            >
              외부 링크 열기
            </a>
          )}
          <time className="mt-1 block text-xs text-gray-400">{a.createdAt}</time>
        </li>
      ))}
    </ul>
  );
}
