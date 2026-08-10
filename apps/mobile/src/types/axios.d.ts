import 'axios';

declare module 'axios' {
  export interface InternalAxiosRequestConfig {
    __uapRetried?: boolean;
  }
}

export {};
