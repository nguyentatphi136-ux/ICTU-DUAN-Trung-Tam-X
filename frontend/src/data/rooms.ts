// Phòng học vật lý và phòng học trực tuyến (S3-09). Khi tích hợp: GET/POST/PUT/DELETE /rooms, PATCH /classes/:code { room }.
import { CLASSES, isOpen } from './classes';
import { persisted } from './store';

export type RoomKind = 'Vật lý' | 'Trực tuyến';
export type Room = {
  name: string;
  kind: RoomKind;
  /** Vị trí với phòng vật lý, liên kết với phòng trực tuyến. */
  where: string;
  /** Số chỗ; bỏ trống là không giới hạn (chỉ phòng trực tuyến). */
  capacity?: number;
};

export const ROOM_KINDS: RoomKind[] = ['Vật lý', 'Trực tuyến'];

export const ROOMS: Room[] = persisted('rooms', [
  { name: 'P.301', kind: 'Vật lý', where: 'Tầng 3, cơ sở Cầu Giấy', capacity: 30 },
  { name: 'P.302', kind: 'Vật lý', where: 'Tầng 3, cơ sở Cầu Giấy', capacity: 20 },
  { name: 'P.401', kind: 'Vật lý', where: 'Tầng 4, cơ sở Cầu Giấy', capacity: 40 },
  { name: 'Lab 1', kind: 'Vật lý', where: 'Tầng 2, cơ sở Thanh Xuân', capacity: 25 },
  { name: 'Lab 2', kind: 'Vật lý', where: 'Tầng 2, cơ sở Thanh Xuân', capacity: 25 },
  { name: 'Meet WEB', kind: 'Trực tuyến', where: 'meet.google.com/tms-web-k16' },
  { name: 'Zoom 01', kind: 'Trực tuyến', where: 'zoom.us/j/842 5510 3367', capacity: 100 },
  { name: 'Zoom 02', kind: 'Trực tuyến', where: 'zoom.us/j/917 2204 6158', capacity: 100 },
]);

export const capacityText = (r: Room) => (r.capacity ? `${r.capacity} ${r.kind === 'Vật lý' ? 'chỗ' : 'người'}` : 'Không giới hạn');
/** Lớp đang chạy xếp vào phòng này. */
export const classesIn = (name: string) => CLASSES.filter((k) => isOpen(k) && k.room === name);
export const findRoom = (name?: string) => ROOMS.find((r) => r.name === name);
/** Liên kết bấm được: thêm https:// nếu thiếu, bỏ khoảng trắng trong mã phòng. */
export const roomHref = (where: string) => (/^https?:\/\//.test(where) ? where : 'https://' + where.replace(/\s+/g, ''));
