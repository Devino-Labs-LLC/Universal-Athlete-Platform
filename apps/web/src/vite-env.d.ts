/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_UAP_ENV?: string;
  readonly VITE_UAP_API_BASE_URL?: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}
