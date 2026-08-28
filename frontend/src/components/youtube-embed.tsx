/**
 * Uses youtube-nocookie.com, which does not set tracking cookies until the viewer
 * actually presses play. Only the video id is stored, so the URL is built here.
 */
export function YouTubeEmbed({ videoId, title }: { videoId: string; title: string }) {
  return (
    <div className="relative w-full overflow-hidden rounded-md border border-line bg-black pt-[56.25%]">
      <iframe
        className="absolute inset-0 h-full w-full"
        src={`https://www.youtube-nocookie.com/embed/${videoId}`}
        title={title}
        loading="lazy"
        allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
        allowFullScreen
      />
    </div>
  );
}
