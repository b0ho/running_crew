import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { AnnouncementForm } from './AnnouncementForm';

describe('AnnouncementForm', () => {
  it('본문이 비어 있으면 검증 오류이고 onSubmit 미호출', async () => {
    const onSubmit = jest.fn();
    render(<AnnouncementForm submitting={false} onSubmit={onSubmit} />);

    await userEvent.click(screen.getByTestId('announcement-submit'));

    expect(screen.getByTestId('announcement-error')).toHaveTextContent('본문');
    expect(onSubmit).not.toHaveBeenCalled();
  });

  it('본문과 외부 링크를 정규화해 onSubmit 호출', async () => {
    const onSubmit = jest.fn();
    render(<AnnouncementForm submitting={false} onSubmit={onSubmit} />);

    await userEvent.type(screen.getByTestId('announcement-body'), '2회차 화상 미팅 안내');
    await userEvent.type(screen.getByTestId('announcement-link'), 'https://meet.example.com/room');
    await userEvent.click(screen.getByTestId('announcement-submit'));

    expect(onSubmit).toHaveBeenCalledWith({
      body: '2회차 화상 미팅 안내',
      externalLink: 'https://meet.example.com/room',
    });
  });

  it('외부 링크 없이 본문만 있으면 externalLink 는 null', async () => {
    const onSubmit = jest.fn();
    render(<AnnouncementForm submitting={false} onSubmit={onSubmit} />);

    await userEvent.type(screen.getByTestId('announcement-body'), '공지 본문');
    await userEvent.click(screen.getByTestId('announcement-submit'));

    expect(onSubmit).toHaveBeenCalledWith({ body: '공지 본문', externalLink: null });
  });
});
