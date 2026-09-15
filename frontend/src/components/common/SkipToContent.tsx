export function SkipToContent({
  targetId = "main-content",
  label = "મુખ્ય વિષયવસ્તુ પર જાઓ",
}: {
  targetId?: string;
  label?: string;
}) {
  return (
    <a
      href={`#${targetId}`}
      className="sr-only focus:not-sr-only focus:fixed focus:top-3 focus:left-3 focus:z-50 focus:rounded-md focus:bg-blue-900 focus:px-4 focus:py-2.5 focus:text-sm focus:font-medium focus:text-white focus:shadow-lg focus:outline-none focus:ring-2 focus:ring-white"
    >
      {label}
    </a>
  );
}
