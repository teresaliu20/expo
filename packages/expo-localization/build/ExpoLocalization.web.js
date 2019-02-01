import * as rtlDetect from 'rtl-detect';
export default {
    get isRTL() {
        return rtlDetect.isRtlLang(this.locale);
    },
    get locale() {
        let locale = navigator.language ||
            navigator['systemLanguage'] ||
            navigator['browserLanguage'] ||
            navigator['userLanguage'] ||
            this.locales[0];
        // The native format is en-US
        return locale.replace('_', '-');
    },
    get locales() {
        const { languages = [] } = navigator;
        return Array.from(languages);
    },
    get timezone() {
        const defaultTimeZone = 'Etc/UTC';
        if (typeof Intl === 'undefined') {
            return defaultTimeZone;
        }
        return Intl.DateTimeFormat().resolvedOptions().timeZone || defaultTimeZone;
    },
    get isoCurrencyCodes() {
        // TODO: Bacon: Add this - very low priority
        return [];
    },
    get country() {
        const { locale } = this;
        if (typeof locale === 'string' && locale.length) {
            const isoCountryCode = locale.substring(locale.lastIndexOf('-') + 1);
            return isoCountryCode.toUpperCase();
        }
        return undefined;
    },
    async getLocalizationAsync() {
        const { country, isoCurrencyCodes, timezone, locales, locale, isRTL } = this;
        return {
            country,
            isoCurrencyCodes,
            timezone,
            locales,
            locale,
            isRTL,
        };
    },
};
//# sourceMappingURL=ExpoLocalization.web.js.map