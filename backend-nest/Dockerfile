# Bước 1: cài thư viện và biên dịch TypeScript sang dist/
FROM node:22-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

# Bước 2: ảnh chạy, chỉ có thư viện cần khi chạy và mã đã biên dịch
FROM node:22-alpine
WORKDIR /app
ENV NODE_ENV=production
COPY package*.json ./
RUN npm ci --omit=dev
COPY --from=build /app/dist ./dist
EXPOSE 3000
CMD ["node", "dist/main.js"]
