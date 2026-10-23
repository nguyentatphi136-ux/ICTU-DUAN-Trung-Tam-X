import type { ReactNode } from 'react';

// Hiển thị mô tả có định dạng (Markdown rút gọn) mà không dùng innerHTML: # tiêu đề, - hoặc 1. danh sách,
// **đậm**, _nghiêng_, __gạch chân__, `mã`, [liên kết](https://...). Dùng cho mô tả bài tập, thông báo lớp.

function inline(text: string, key: string): ReactNode[] {
  const out: ReactNode[] = [];
  const re = /\*\*(.+?)\*\*|__(.+?)__|_(.+?)_|`(.+?)`|\[(.+?)\]\((https?:\/\/[^\s)]+)\)/g;
  let last = 0;
  let m: RegExpExecArray | null;
  let i = 0;
  while ((m = re.exec(text))) {
    if (m.index > last) out.push(text.slice(last, m.index));
    const k = `${key}-${i++}`;
    if (m[1]) out.push(<b key={k}>{m[1]}</b>);
    else if (m[2]) out.push(<u key={k}>{m[2]}</u>);
    else if (m[3]) out.push(<i key={k}>{m[3]}</i>);
    else if (m[4]) out.push(<code key={k}>{m[4]}</code>);
    else
      out.push(
        <a key={k} href={m[6]} target="_blank" rel="noreferrer">
          {m[5]}
        </a>,
      );
    last = re.lastIndex;
  }
  if (last < text.length) out.push(text.slice(last));
  return out;
}

export function Markdown({ text }: { text: string }) {
  const blocks: ReactNode[] = [];
  const lines = text.split('\n');
  for (let i = 0; i < lines.length; i++) {
    const line = lines[i].trim();
    if (!line) continue;
    const ul = /^[-•*]\s+/;
    const ol = /^\d+\.\s+/;
    if (ul.test(line) || ol.test(line)) {
      const ordered = ol.test(line);
      const items: string[] = [];
      while (i < lines.length && (ordered ? ol : ul).test(lines[i].trim())) items.push(lines[i++].trim().replace(ordered ? ol : ul, ''));
      i--;
      const li = items.map((t, j) => <li key={j}>{inline(t, `${i}-${j}`)}</li>);
      blocks.push(ordered ? <ol key={i}>{li}</ol> : <ul key={i}>{li}</ul>);
    } else if (line.startsWith('## ')) blocks.push(<h5 key={i}>{inline(line.slice(3), String(i))}</h5>);
    else if (line.startsWith('# ')) blocks.push(<h4 key={i}>{inline(line.slice(2), String(i))}</h4>);
    else blocks.push(<p key={i}>{inline(line, String(i))}</p>);
  }
  return <div className="md">{blocks}</div>;
}
