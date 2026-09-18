import { getRequestConfig } from 'next-intl/server';
import { cookies } from 'next/headers';

export const locales = ['en', 'sw'] as const;
export type Locale = (typeof locales)[number];
export const defaultLocale: Locale = 'en';

export default getRequestConfig(async ({ locale }) => {
  return {
    locale,
    messages: (await import(`./messages/${locale}.json`)).default,
  };
});
