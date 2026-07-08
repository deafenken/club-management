# ---- Build Stage ----
FROM node:20-alpine AS build
WORKDIR /app
# Copy package files first for better layer caching
COPY package.json package-lock.json ./
# Use the npmmirror registry (faster/more reliable from CN networks)
RUN npm config set registry https://registry.npmmirror.com && npm ci
# Copy source and build
COPY . .
RUN npm run build

# ---- Runtime Stage ----
FROM nginx:alpine
# Remove default nginx config
RUN rm /etc/nginx/conf.d/default.conf
# Copy our custom nginx config
COPY nginx.conf /etc/nginx/conf.d/default.conf
# Copy built static files
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
