import { useRef, useState } from 'react';
import { ACCEPT_ATTR } from './fileConstraints';

/**
 * 파일 드롭존 (frontend-components §2.3, 접근성 §4).
 *
 * 드래그앤드롭과 함께 키보드 접근 가능한 파일 선택 input 을 항상 제공한다(드롭존 단독 금지). 업로드 중에는 비활성화한다.
 */
export function FileDropzone({
  onFile,
  disabled = false,
  testId = 'file-dropzone',
}: {
  onFile: (file: File) => void;
  disabled?: boolean;
  testId?: string;
}) {
  const inputRef = useRef<HTMLInputElement>(null);
  const [dragOver, setDragOver] = useState(false);

  function handleFiles(files: FileList | null) {
    if (disabled || !files || files.length === 0) {
      return;
    }
    onFile(files[0]);
  }

  return (
    <div
      data-testid={testId}
      onDragOver={(e) => {
        e.preventDefault();
        if (!disabled) setDragOver(true);
      }}
      onDragLeave={() => setDragOver(false)}
      onDrop={(e) => {
        e.preventDefault();
        setDragOver(false);
        handleFiles(e.dataTransfer.files);
      }}
      className={`rounded border border-dashed p-4 text-center text-sm ${
        dragOver ? 'border-accent bg-accent/5' : 'border-gray-300'
      } ${disabled ? 'opacity-50' : ''}`}
    >
      <p className="text-gray-600">증빙 파일을 여기로 끌어다 놓거나 아래 버튼으로 선택하세요 (jpg/png/pdf, 최대 10MB)</p>
      {/* 키보드 접근 대체 input — 라벨과 연결 */}
      <label className="mt-2 inline-block cursor-pointer rounded bg-accent px-3 py-1 text-white hover:bg-accent-hover">
        파일 선택
        <input
          ref={inputRef}
          type="file"
          accept={ACCEPT_ATTR}
          disabled={disabled}
          data-testid={`${testId}-input`}
          className="sr-only"
          onChange={(e) => {
            handleFiles(e.target.files);
            // 동일 파일 재선택도 change 이벤트가 발생하도록 값 초기화
            e.target.value = '';
          }}
        />
      </label>
    </div>
  );
}
