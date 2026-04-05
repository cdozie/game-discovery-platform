function parseDisplayDate(dateValue?: string | null): Date | null {
    if (!dateValue) {
        return null;
    }

    const normalized = dateValue.trim();
    const dateOnlyMatch = normalized.match(/^(\d{4})-(\d{2})-(\d{2})$/);
    if (dateOnlyMatch) {
        const [, year, month, day] = dateOnlyMatch;
        return new Date(Number(year), Number(month) - 1, Number(day), 12, 0, 0, 0);
    }

    const parsedDate = new Date(normalized);
    return Number.isNaN(parsedDate.getTime()) ? null : parsedDate;
}

export function formatDisplayDate(dateValue?: string | null,
                                  options: Intl.DateTimeFormatOptions = {
                                      month: 'long',
                                      day: 'numeric',
                                      year: 'numeric'
                                  },
                                  fallback = 'Unknown') {
    if (!dateValue) {
        return fallback;
    }

    const parsedDate = parseDisplayDate(dateValue);
    if (!parsedDate) {
        return dateValue;
    }

    return parsedDate.toLocaleDateString(undefined, options);
}
