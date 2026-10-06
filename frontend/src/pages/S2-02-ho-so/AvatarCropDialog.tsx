import { useEffect, useState } from 'react';
import { Icon } from '../../components/Icon';
import { Modal } from '../../components/ui';

const BOX = 320;
const FRAME = 220;
const OUT = 256;

// S2-03. Cắt ảnh vuông ở giữa khung, thu phóng bằng thanh trượt, xem trước bản 96px và 40px.
// ponytail: chưa kéo được vị trí ảnh, chỉ cắt ở giữa; thêm kéo khi người dùng cần chọn vùng khác.
export function AvatarCropDialog({ file, onClose, onSave }: { file: File; onClose: () => void; onSave: (dataUrl: string) => void }) {
  const [url, setUrl] = useState('');
  const [img, setImg] = useState<HTMLImageElement | null>(null);
  const [zoom, setZoom] = useState(1);

  useEffect(() => {
    const u = URL.createObjectURL(file);
    const i = new Image();
    i.onload = () => setImg(i);
    i.src = u;
    setUrl(u);
    return () => URL.revokeObjectURL(u);
  }, [file]);

  const scale = img ? (BOX / Math.min(img.naturalWidth, img.naturalHeight)) * zoom : 1;
  const imgStyle = img ? { width: img.naturalWidth * scale, height: img.naturalHeight * scale } : undefined;
  // Bản xem trước: cùng ảnh, phóng sao cho khung 220px vừa đúng đường kính.
  const preview = (size: number) => ({ backgroundImage: `url(${url})`, backgroundSize: img ? `${(img.naturalWidth * scale * size) / FRAME}px auto` : 'cover', width: size, height: size });

  function save() {
    if (!img) return;
    const side = FRAME / scale;
    const canvas = document.createElement('canvas');
    canvas.width = canvas.height = OUT;
    canvas.getContext('2d')!.drawImage(img, (img.naturalWidth - side) / 2, (img.naturalHeight - side) / 2, side, side, 0, 0, OUT, OUT);
    onSave(canvas.toDataURL('image/jpeg', 0.9));
  }

  return (
    <Modal onClose={onClose} label="Cắt ảnh đại diện" wide>
      <div className="crop-head">
        <h3>Cắt ảnh đại diện</h3>
        <button type="button" className="icon-btn" aria-label="Đóng" onClick={onClose}>
          <Icon name="x" size={20} />
        </button>
      </div>
      <div className="crop">
        <div className="crop-box">
          {img && <img src={url} alt="" style={imgStyle} />}
          <span className="crop-frame" />
        </div>
        <div className="crop-side">
          <b>Xem trước</b>
          <div className="crop-prev">
            <span style={preview(96)} />
            <span style={preview(40)} />
          </div>
          <p className="hint">Bản lớn 96px và bản thu nhỏ 40px</p>
          <div className="crop-file">
            <b>{file.name}</b>
            <span className="hint">
              {file.type === 'image/png' ? 'PNG' : 'JPG'} · {(file.size / 1024 / 1024).toLocaleString('vi-VN', { maximumFractionDigits: 1 })} MB
            </span>
          </div>
        </div>
      </div>
      <label className="crop-zoom">
        <b>Thu phóng</b>
        <input type="range" min={1} max={3} step={0.01} value={zoom} onChange={(e) => setZoom(Number(e.target.value))} />
      </label>
      <div className="acts">
        <button type="button" className="btn" style={{ width: 100 }} onClick={onClose}>
          Huỷ
        </button>
        <button type="button" className="btn primary" style={{ width: 130 }} disabled={!img} onClick={save}>
          Lưu ảnh
        </button>
      </div>
    </Modal>
  );
}
