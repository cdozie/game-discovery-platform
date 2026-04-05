export function parseRatingValue(value?: number | string | null): number | null {
    if (value === null || value === undefined || value === '') {
        return null;
    }

    const parsedValue = Number(value);
    return Number.isFinite(parsedValue) ? parsedValue : null;
}

export function formatTenPointRating(value?: number | string | null, emptyLabel = 'Not rated yet'): string {
    const numericValue = parseRatingValue(value);
    if (numericValue === null) {
        return emptyLabel;
    }

    return `${numericValue.toFixed(1)}/10`;
}

export function getRatingVisuals(value?: number | string | null) {
    const numericValue = parseRatingValue(value);
    if (numericValue === null) {
        return {
            background: 'rgba(148, 163, 184, 0.12)',
            border: 'rgba(100, 116, 139, 0.24)',
            text: '#334155',
            icon: '#64748b',
            label: 'Unrated'
        };
    }

    if (numericValue >= 10) {
        return {
            background: 'linear-gradient(135deg, rgba(20, 184, 166, 0.22), rgba(59, 130, 246, 0.3), rgba(250, 204, 21, 0.24))',
            border: 'rgba(14, 116, 144, 0.4)',
            text: '#0f172a',
            icon: '#0f766e',
            label: 'Perfect'
        };
    }

    if (numericValue >= 9.3) {
        return {
            background: 'linear-gradient(135deg, rgba(245, 158, 11, 0.2), rgba(234, 88, 12, 0.28))',
            border: 'rgba(217, 119, 6, 0.42)',
            text: '#7c2d12',
            icon: '#d97706',
            label: 'Critical Acclaim'
        };
    }

    if (numericValue >= 8.5) {
        return {
            background: 'linear-gradient(135deg, rgba(34, 197, 94, 0.2), rgba(16, 185, 129, 0.28))',
            border: 'rgba(5, 150, 105, 0.38)',
            text: '#14532d',
            icon: '#059669',
            label: 'Excellent'
        };
    }

    if (numericValue >= 8.0) {
        return {
            background: 'linear-gradient(135deg, rgba(59, 130, 246, 0.18), rgba(14, 165, 233, 0.24))',
            border: 'rgba(37, 99, 235, 0.34)',
            text: '#1e3a8a',
            icon: '#2563eb',
            label: 'Great'
        };
    }

    if (numericValue >= 7.5) {
        return {
            background: 'linear-gradient(135deg, rgba(16, 185, 129, 0.16), rgba(132, 204, 22, 0.22))',
            border: 'rgba(34, 197, 94, 0.34)',
            text: '#166534',
            icon: '#16a34a',
            label: 'Good'
        };
    }

    if (numericValue >= 6.0) {
        return {
            background: 'linear-gradient(135deg, rgba(250, 204, 21, 0.16), rgba(249, 115, 22, 0.18))',
            border: 'rgba(217, 119, 6, 0.3)',
            text: '#854d0e',
            icon: '#ca8a04',
            label: 'Average'
        };
    }

    if (numericValue >= 4.5) {
        return {
            background: 'linear-gradient(135deg, rgba(251, 146, 60, 0.18), rgba(249, 115, 22, 0.22))',
            border: 'rgba(234, 88, 12, 0.32)',
            text: '#7c2d12',
            icon: '#ea580c',
            label: 'Below Average'
        };
    }

    if (numericValue >= 3.0) {
        return {
            background: 'linear-gradient(135deg, rgba(248, 113, 113, 0.16), rgba(239, 68, 68, 0.22))',
            border: 'rgba(220, 38, 38, 0.3)',
            text: '#991b1b',
            icon: '#dc2626',
            label: 'Poor'
        };
    }

    return {
        background: 'linear-gradient(135deg, rgba(127, 29, 29, 0.22), rgba(185, 28, 28, 0.28))',
        border: 'rgba(153, 27, 27, 0.36)',
        text: '#fee2e2',
        icon: '#fca5a5',
        label: 'Rough'
    };
}
