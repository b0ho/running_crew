import { useEffect } from 'react';

export type ToastVariant = 'success' | 'info' | 'error';

export interface ToastMessage {
  text: string;
  variant: ToastVariant;
}

const VARIANT_CLASSES: Record<ToastVariant, string> = {
  success: 'bg-green-600 text-white',
  info: 'bg-accent text-white',
  error: 'bg-red-600 text-white',
};

/**
 * 토스트 알림 (frontend-components §2.1, 접근성 §4).
 *
 * aria-live 영역으로 스크린리더에 결과를 전달한다. 영역 컨테이너는 항상 렌더하고 내용만 교체해 라이브 리전 announce 가 안정적으로 발생하게 한다.
 * message 가 있으면 duration 후 자동으로 onDismiss 를 호출한다.
 */
export function Toast({
  message,
  onDismiss,
  duration = 4000,
}: {
  message: ToastMessage | null;
  onDismiss?: () => void;
  duration?: number;
}) {
  useEffect(() => {
    if (!message || !onDismiss) {
      return;
    }
    const timer = setTimeout(onDismiss, duration);
    return () => clearTimeout(timer);
  }, [message, onDismiss, duration]);

  return (
    <div
      role="status"
      aria-live="polite"
      data-testid="toast"
      className="pointer-events-none fixed inset-x-0 bottom-24 z-50 flex justify-center md:bottom-8"
    >
      {message && (
        <div
          data-testid={`toast-${message.variant}`}
          className={`pointer-events-auto rounded px-4 py-2 text-sm shadow-lg ${VARIANT_CLASSES[message.variant]}`}
        >
          {message.text}
        </div>
      )}
    </div>
  );
}
