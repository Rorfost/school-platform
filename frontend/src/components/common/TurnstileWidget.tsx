import { useEffect, useRef } from "react";

declare global {
    interface Window {
        turnstile?: {
            render: (
                container: HTMLElement | string,
                options: {
                    sitekey: string;
                    action?: string;
                    callback?: (token: string) => void;
                    "error-callback"?: () => void;
                    "expired-callback"?: () => void;
                    theme?: "light" | "dark" | "auto";
                },
            ) => string;
            reset: (widgetId: string) => void;
            remove: (widgetId: string) => void;
        };
        onTurnstileLoaded?: () => void;
    }
}

interface TurnstileWidgetProps {
    action: string;
    onTokenChange: (token: string | null) => void;
    onError?: () => void;
    siteKey?: string;
    resetKey?: number;
}

export function TurnstileWidget({
    action,
    onTokenChange,
    onError,
    siteKey = import.meta.env.VITE_TURNSTILE_SITE_KEY,
    resetKey = 0,
}: TurnstileWidgetProps) {
    const containerRef = useRef<HTMLDivElement>(null);
    const widgetIdRef = useRef<string | null>(null);

    const callbacksRef = useRef({ onTokenChange, onError });

    // Keep the latest callbacks without forcing the Turnstile widget to
    // unmount and render again whenever the parent creates a new callback.

    useEffect(() => {
        if (widgetIdRef.current && window.turnstile) {
            window.turnstile.reset(widgetIdRef.current);
            callbacksRef.current.onTokenChange(null);
        }
    }, [resetKey]);

    useEffect(() => {
        callbacksRef.current = { onTokenChange, onError };
    }, [onTokenChange, onError]);

    useEffect(() => {
        if (!siteKey) {
            // In local/test development without a configured Turnstile site key,
            // we do not attempt to load the external script.
            return;
        }

        let isMounted = true;

        const renderWidget = () => {
            if (!containerRef.current || !window.turnstile || !isMounted) return;
            if (widgetIdRef.current) return;

            widgetIdRef.current = window.turnstile.render(containerRef.current, {
                sitekey: siteKey,
                action,
                callback: (token: string) => {
                    if (isMounted) {
                        callbacksRef.current.onTokenChange(token);
                    }
                },
                "error-callback": () => {
                    if (isMounted) {
                        callbacksRef.current.onTokenChange(null);
                        callbacksRef.current.onError?.();
                    }
                },
                "expired-callback": () => {
                    if (isMounted) {
                        callbacksRef.current.onTokenChange(null);
                    }
                },
                theme: "light",
            });
        };

        if (window.turnstile) {
            renderWidget();
        } else {
            const existingScript = document.querySelector('script[src*="turnstile/v0/api.js"]');
            if (!existingScript) {
                const script = document.createElement("script");
                script.src =
                    "https://challenges.cloudflare.com/turnstile/v0/api.js?onload=onTurnstileLoaded";
                script.async = true;
                script.defer = true;
                window.onTurnstileLoaded = () => {
                    renderWidget();
                };
                document.head.appendChild(script);
            } else {
                const prevOnload = window.onTurnstileLoaded;
                window.onTurnstileLoaded = () => {
                    prevOnload?.();
                    renderWidget();
                };
            }
        }

        return () => {
            isMounted = false;
            if (widgetIdRef.current && window.turnstile) {
                try {
                    window.turnstile.remove(widgetIdRef.current);
                } catch {
                    // Ignore removal failures during unmount
                }
                widgetIdRef.current = null;
            }
        };
    }, [action, siteKey]);

    if (!siteKey) {
        return null;
    }

    return (
        <div
            ref={containerRef}
            className="my-3 flex justify-center min-h-[65px]"
            aria-label="સુરક્ષા ચકાસણી"
        />
    );
}
