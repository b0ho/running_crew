import { useEffect, useState } from 'react';
import { ApiError } from '../api/ApiClient';
import { completionApi } from '../api/completionApi';

type ResultState = 'loading' | 'certified' | 'not-certified' | 'error';

/**
 * 수료 결과 배너 (US-12, 멘티).
 *
 * 종료된 코호트의 멘티에게 수료/미수료를 색+텍스트로 병기해 표시한다(§4). 수료 시 수료증(PNG) 다운로드 버튼을 제공한다. 수료 여부는 서버가 판정하며(클라이언트
 * 조작 불가), 수료증 조회(GET /certificate)가 성공하면 수료, 404 면 미수료로 판정한다.
 */
export function CompletionResult({ cohortId }: { cohortId: number }) {
  const [state, setState] = useState<ResultState>('loading');
  const [blob, setBlob] = useState<Blob | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  useEffect(() => {
    let active = true;
    completionApi
      .getCertificate(cohortId)
      .then((b) => {
        if (!active) return;
        setBlob(b);
        setState('certified');
      })
      .catch((err) => {
        if (!active) return;
        if (err instanceof ApiError && err.code === 'NOT_FOUND') {
          setState('not-certified');
        } else {
          setErrorMessage(
            err instanceof ApiError ? err.message : '수료 결과를 불러오지 못했습니다',
          );
          setState('error');
        }
      });
    return () => {
      active = false;
    };
  }, [cohortId]);

  function handleDownload() {
    if (!blob) return;
    const url = URL.createObjectURL(blob);
    const anchor = document.createElement('a');
    anchor.href = url;
    anchor.download = `certificate-${cohortId}.png`;
    document.body.appendChild(anchor);
    anchor.click();
    document.body.removeChild(anchor);
    URL.revokeObjectURL(url);
  }

  if (state === 'loading') {
    return (
      <p data-testid="completion-result-loading" className="text-sm text-gray-500">
        수료 결과를 불러오는 중...
      </p>
    );
  }

  if (state === 'error') {
    return (
      <p role="alert" data-testid="completion-result-error" className="text-sm text-red-600">
        {errorMessage}
      </p>
    );
  }

  if (state === 'certified') {
    return (
      <section
        data-testid="completion-result-certified"
        className="rounded border border-green-300 bg-green-50 p-4"
      >
        <p className="text-sm font-semibold text-green-800">수료를 축하합니다!</p>
        <p className="mt-1 text-sm text-green-700">수료증을 내려받을 수 있습니다.</p>
        <button
          type="button"
          data-testid="certificate-download"
          onClick={handleDownload}
          aria-label="수료증 이미지 다운로드"
          className="mt-3 rounded bg-green-600 px-4 py-2 text-sm text-white hover:bg-green-700"
        >
          수료증 다운로드
        </button>
      </section>
    );
  }

  // not-certified
  return (
    <section
      data-testid="completion-result-not-certified"
      className="rounded border border-gray-300 bg-gray-50 p-4"
    >
      <p className="text-sm font-semibold text-gray-800">미수료</p>
      <p className="mt-1 text-sm text-gray-600">
        출석률(인증 회차)이 수료 기준(80%)에 도달하지 못했습니다.
      </p>
    </section>
  );
}
