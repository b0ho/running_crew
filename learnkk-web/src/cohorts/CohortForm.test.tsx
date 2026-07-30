import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { CohortForm } from './CohortForm';

describe('CohortForm', () => {
  it('제목이 비어 있으면 클라이언트 검증 오류이고 onSubmit 미호출', async () => {
    const onSubmit = jest.fn();
    render(<CohortForm submitLabel="개설" submitting={false} onSubmit={onSubmit} />);

    await userEvent.click(screen.getByTestId('cohort-submit'));

    expect(screen.getByTestId('cohort-form-error')).toHaveTextContent('제목');
    expect(onSubmit).not.toHaveBeenCalled();
  });

  it('종료일이 시작일보다 이르면 검증 오류', async () => {
    const onSubmit = jest.fn();
    render(<CohortForm submitLabel="개설" submitting={false} onSubmit={onSubmit} />);

    await userEvent.type(screen.getByTestId('cohort-title'), '자바 멘토링');
    await userEvent.type(screen.getByTestId('cohort-start-date'), '2026-03-01');
    await userEvent.type(screen.getByTestId('cohort-end-date'), '2026-01-01');
    await userEvent.click(screen.getByTestId('cohort-submit'));

    expect(screen.getByTestId('cohort-form-error')).toHaveTextContent('종료일');
    expect(onSubmit).not.toHaveBeenCalled();
  });

  it('유효한 입력이면 정규화된 요청으로 onSubmit 호출', async () => {
    const onSubmit = jest.fn();
    render(<CohortForm submitLabel="개설" submitting={false} onSubmit={onSubmit} />);

    await userEvent.type(screen.getByTestId('cohort-title'), '자바 멘토링');
    await userEvent.clear(screen.getByTestId('cohort-capacity'));
    await userEvent.type(screen.getByTestId('cohort-capacity'), '20');
    await userEvent.clear(screen.getByTestId('cohort-session-count'));
    await userEvent.type(screen.getByTestId('cohort-session-count'), '6');
    await userEvent.type(screen.getByTestId('cohort-start-date'), '2026-01-01');
    await userEvent.type(screen.getByTestId('cohort-end-date'), '2026-03-01');
    await userEvent.click(screen.getByTestId('cohort-submit'));

    expect(onSubmit).toHaveBeenCalledWith({
      title: '자바 멘토링',
      description: null,
      capacity: 20,
      startDate: '2026-01-01',
      endDate: '2026-03-01',
      sessionCount: 6,
    });
  });

  it('서버 에러 메시지를 표시한다', () => {
    render(
      <CohortForm
        submitLabel="수정 저장"
        submitting={false}
        serverError="종료된 코호트는 수정할 수 없습니다"
        onSubmit={jest.fn()}
      />,
    );
    expect(screen.getByTestId('cohort-form-error')).toHaveTextContent('종료된 코호트');
  });
});
