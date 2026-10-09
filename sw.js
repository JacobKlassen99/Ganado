// Service Worker para Registrar Ganado PWA
// Cache name versioning
const CACHE_NAME = 'ganado-app-v1.0.1';

// Recursos estáticos mínimos requeridos para la app shell
const STATIC_ASSETS = [
  './',
  './index.html',
  './styles.css',
  './app.js',
  './manifest.json',
  './favicon.png',
  './icons/icon.svg',
  './icons/icon-192.png',
  './icons/icon-512.png',
  './icons/icon-maskable-192.png',
  './icons/icon-maskable-512.png',
  './icons/apple-touch-icon.png'
];

// Instalación: Precarga el shell estático
self.addEventListener('install', (event) => {
  event.waitUntil(
    caches.open(CACHE_NAME).then((cache) => {
      return cache.addAll(STATIC_ASSETS);
    })
  );
  // NO llamamos self.skipWaiting() aquí de forma incondicional;
  // esperamos el mensaje SKIP_WAITING enviado por la UI cuando el usuario pulsa "Actualizar ahora"
});

// Activación: Limpia cachés antiguas
self.addEventListener('activate', (event) => {
  event.waitUntil(
    caches.keys().then((keys) => {
      return Promise.all(
        keys.map((key) => {
          if (key !== CACHE_NAME) {
            return caches.delete(key);
          }
        })
      );
    }).then(() => self.clients.claim())
  );
});

// Recepción de mensaje de la aplicación para activar actualización
self.addEventListener('message', (event) => {
  if (event.data && event.data.type === 'SKIP_WAITING') {
    self.skipWaiting();
  }
});

// Estrategia de peticiones (Fetch)
self.addEventListener('fetch', (event) => {
  const url = new URL(event.request.url);

  // 1. IMPORTANTE: Las peticiones a Google Apps Script (script.google.com o googleapis)
  // deben ir SIEMPRE directas a red (network-only) sin cachearse para garantizar
  // que los datos de ganado y pesajes siempre estén en tiempo real y no se afecten.
  if (url.hostname.includes('script.google.com') ||
      url.hostname.includes('google.com') ||
      url.hostname.includes('googleapis.com')) {
    event.respondWith(fetch(event.request));
    return;
  }

  // 2. Para peticiones no GET (POST de nuevo ganado o nuevo pesaje), ir directas a red
  if (event.request.method !== 'GET') {
    event.respondWith(fetch(event.request));
    return;
  }

  // 3. Para la navegación y archivos estáticos de la app: Network-First con fallback a Caché
  // Esto asegura que si hay nueva versión publicada en Netlify, el navegador la obtenga de inmediato,
  // y si no hay conexión a internet, funcione con la caché.
  event.respondWith(
    fetch(event.request)
      .then((networkResponse) => {
        if (networkResponse && networkResponse.status === 200) {
          const responseToCache = networkResponse.clone();
          caches.open(CACHE_NAME).then((cache) => {
            cache.put(event.request, responseToCache);
          });
        }
        return networkResponse;
      })
      .catch(() => {
        return caches.match(event.request).then((cachedResponse) => {
          if (cachedResponse) {
            return cachedResponse;
          }
          if (event.request.mode === 'navigate') {
            return caches.match('./index.html');
          }
          return new Response('Sin conexión', { status: 503, statusText: 'Offline' });
        });
      })
  );
});
