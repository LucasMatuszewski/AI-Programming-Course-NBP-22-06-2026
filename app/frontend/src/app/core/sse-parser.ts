export async function* parseSseStream(body: ReadableStream<Uint8Array>): AsyncGenerator<string> {
  const reader = body.getReader();
  const decoder = new TextDecoder();
  let buffer = '';
  try {
    while (true) {
      const { done, value } = await reader.read();
      if (done) break;
      buffer += decoder.decode(value, { stream: true });
      const frames = buffer.split('\n\n');
      buffer = frames.pop() ?? '';
      for (const frame of frames) {
        if (!frame.trim()) continue;
        const lines = frame.split('\n');
        const eventLine = lines.find(l => l.startsWith('event:'));
        const dataLine = lines.find(l => l.startsWith('data:'));
        const eventType = eventLine?.slice('event:'.length).trim();
        const data = dataLine?.slice('data:'.length).trim() ?? '';
        if (eventType === 'done') return;
        if (eventType === 'error') throw new Error(data || 'LLM_UNAVAILABLE');
        if (data) yield data;
      }
    }
  } finally {
    reader.releaseLock();
  }
}
