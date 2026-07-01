# Frontend — Motus

Client **React 18** + **Vite 5**, servi en production par Nginx (proxy `/api` → gateway).

## Développement

```bash
npm install
npm run dev
```

Configurer `VITE_API_URL=http://localhost:8080` si le gateway n'est pas proxifié (sans Docker).

## Structure

```
frontend/
├── src/
│   ├── main.jsx
│   ├── App.jsx
│   ├── api.js
│   ├── components/
│   └── utils/
├── index.html
├── package.json
├── vite.config.js
└── nginx.conf          # utilisé par le Dockerfile
```
