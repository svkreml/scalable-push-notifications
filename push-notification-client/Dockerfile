FROM node:14 AS builder
# делаем каталог 'app' текущим рабочим каталогом
WORKDIR /src
# копируем оба 'package.json' и 'package-lock.json' (если есть)
COPY package*.json ./
# устанавливаем зависимости проекта
RUN npm install
# копируем файлы и каталоги проекта в текущий рабочий каталог (т.е. в каталог 'app')
COPY . .
# собираем приложение для production с минификацией
RUN npm run build
RUN ls -l

FROM nginx:1.24 as runner
WORKDIR /usr/share/nginx/html
COPY --from=builder /src/build .
EXPOSE 80
