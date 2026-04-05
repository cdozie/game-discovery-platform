package com.madre.excelweb.service.util;

import com.madre.excelweb.jpa.entity.GameData;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class CatalogGroupingUtils {
    private static final Set<String> PLATFORM_CONNECTOR_TOKENS = Set.of("and");
    private static final Set<List<String>> TRAILING_PLATFORM_SUFFIXES = Set.of(
            List.of("ps5"),
            List.of("ps4"),
            List.of("ps3"),
            List.of("ps", "vita"),
            List.of("playstation", "5"),
            List.of("playstation", "4"),
            List.of("playstation", "3"),
            List.of("playstation", "vita"),
            List.of("xbox", "one"),
            List.of("xbox", "series"),
            List.of("xbox", "series", "x", "s"),
            List.of("nintendo", "switch"),
            List.of("pc"),
            List.of("pc", "microsoft", "windows"),
            List.of("microsoft", "windows"),
            List.of("windows")
    );
    private static final Set<String> PROVIDER_NOISE_TOKENS = Set.of(
            "co", "company", "corp", "corporation", "inc", "incorporated", "llc", "ltd", "limited",
            "games", "game", "studio", "studios", "interactive", "publishing", "publisher", "entertainment",
            "digital", "communications", "software", "media", "records",
            "gmbh", "srl", "sl", "sa", "ou", "pty", "t", "a", "z", "o",
            "of", "the"
    );

    private CatalogGroupingUtils() {
    }

    public static String buildGroupKey(final GameData gameData) {
        return buildGroupKey(
                gameData == null ? null : gameData.getName(),
                gameData == null ? null : gameData.getProviderName(),
                gameData == null ? null : gameData.getDescription()
        );
    }

    public static String buildGroupKey(final String name,
                                       final String providerName,
                                       final String description) {
        final String canonicalValue = Stream.of(name, providerName, description)
                .map(CatalogGroupingUtils::normalizeKeyPart)
                .collect(Collectors.joining("|"));
        return sha256Hex(canonicalValue);
    }

    public static String buildNormalizedTitleKey(final GameData gameData) {
        return buildNormalizedTitleKey(gameData == null ? null : gameData.getName());
    }

    public static String buildNormalizedTitleKey(final String name) {
        final String normalizedName = normalizeKeyPart(name);
        if (normalizedName.isBlank()) {
            return "";
        }

        final List<String> tokens = new ArrayList<>(List.of(normalizedName.split(" ")));
        trimTrailingPlatformTokens(tokens);
        return String.join(" ", tokens).trim();
    }

    public static List<String> parseDelimitedValues(final String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }

        return Stream.of(value.split(","))
                .map(String::trim)
                .filter(entry -> !entry.isBlank())
                .distinct()
                .toList();
    }

    public static String mergeDelimitedValues(final String... values) {
        return mergeDelimitedValues(Stream.of(values)
                .flatMap(value -> parseDelimitedValues(value).stream())
                .toList());
    }

    public static String mergeDelimitedValues(final Collection<String> values) {
        final Set<String> mergedValues = new LinkedHashSet<>();
        values.stream()
                .filter(value -> value != null && !value.isBlank())
                .flatMap(value -> parseDelimitedValues(value).stream())
                .forEach(mergedValues::add);

        return String.join(", ", mergedValues);
    }

    public static String mergePlatformValues(final String... values) {
        return mergePlatformValues(Stream.of(values)
                .flatMap(value -> parseDelimitedValues(value).stream())
                .toList());
    }

    public static String mergePlatformValues(final Collection<String> values) {
        final Set<String> mergedValues = new LinkedHashSet<>();
        values.stream()
                .filter(value -> value != null && !value.isBlank())
                .flatMap(value -> parseDelimitedValues(value).stream())
                .map(CatalogGroupingUtils::canonicalPlatformLabel)
                .filter(value -> !value.isBlank())
                .forEach(mergedValues::add);

        return String.join(", ", mergedValues);
    }

    public static String canonicalizePlatformValue(final String platform) {
        return canonicalPlatformLabel(platform);
    }

    public static String pickPreferredText(final String... values) {
        return Stream.of(values)
                .filter(value -> value != null && !value.isBlank())
                .max((left, right) -> Integer.compare(left.trim().length(), right.trim().length()))
                .orElse("");
    }

    public static String firstNonBlank(final String... values) {
        return Stream.of(values)
                .filter(value -> value != null && !value.isBlank())
                .findFirst()
                .orElse("");
    }

    public static boolean containsDelimitedValue(final String delimitedValues, final String candidate) {
        if (candidate == null || candidate.isBlank()) {
            return false;
        }

        return parseDelimitedValues(delimitedValues).stream()
                .anyMatch(value -> value.equalsIgnoreCase(candidate.trim()));
    }

    public static String pickPreferredTitle(final String... values) {
        return Stream.of(values)
                .filter(value -> value != null && !value.isBlank())
                .min(Comparator
                        .comparingInt(CatalogGroupingUtils::titleNoisePenalty)
                        .thenComparingInt(value -> value.trim().length())
                        .thenComparing(String::compareToIgnoreCase))
                .orElse("");
    }

    public static boolean shouldMergeByTitleIdentity(final GameData leftGame, final GameData rightGame) {
        if (leftGame == null || rightGame == null) {
            return false;
        }

        final String leftTitleKey = firstNonBlank(leftGame.getNormalizedTitleKey(), buildNormalizedTitleKey(leftGame));
        final String rightTitleKey = firstNonBlank(rightGame.getNormalizedTitleKey(), buildNormalizedTitleKey(rightGame));
        if (leftTitleKey.isBlank() || !leftTitleKey.equals(rightTitleKey)) {
            return false;
        }

        final int titleTokenCount = titleTokenCount(leftTitleKey);
        final boolean numericIdentity = hasNumericIdentityToken(leftTitleKey);
        final boolean providerMatch = providersComparable(leftGame.getProviderName(), rightGame.getProviderName());
        final boolean platformMatch = platformsOverlap(leftGame.getPlatforms(), rightGame.getPlatforms());
        final boolean platformFamilyMatch = platformFamiliesOverlap(leftGame.getPlatforms(), rightGame.getPlatforms());
        final boolean releaseMatch = releaseYearsMatch(leftGame.getReleaseDate(), rightGame.getReleaseDate());
        final boolean releaseNearMatch = releaseYearsWithin(leftGame.getReleaseDate(), rightGame.getReleaseDate(), 3);
        final boolean titleVariantNoise = hasTitleVariantNoise(leftGame.getName()) || hasTitleVariantNoise(rightGame.getName());
        final boolean crossSourcePlayStationPair = isCrossSourcePlayStationPair(leftGame.getCatalogSources(), rightGame.getCatalogSources());

        if (numericIdentity && providerMatch && catalogSourcesOverlap(leftGame.getCatalogSources(), rightGame.getCatalogSources())) {
            return true;
        }

        if (providerMatch && (platformMatch || platformFamilyMatch || releaseNearMatch) && (titleVariantNoise || titleTokenCount >= 2)) {
            return true;
        }

        if (crossSourcePlayStationPair && platformFamilyMatch) {
            if (numericIdentity) {
                return true;
            }
            if (titleVariantNoise) {
                return true;
            }
            if (providerMatch) {
                return true;
            }
            if (titleTokenCount >= 2) {
                return true;
            }
            if (platformMatch && releaseNearMatch) {
                return true;
            }
        }

        int confidenceSignals = 0;
        if (providerMatch) {
            confidenceSignals++;
        }
        if (platformMatch) {
            confidenceSignals++;
        }
        if (releaseMatch) {
            confidenceSignals++;
        }
        if (titleVariantNoise) {
            confidenceSignals++;
        }
        return confidenceSignals >= 2;
    }

    private static String normalizeKeyPart(final String value) {
        if (value == null || value.isBlank()) {
            return "";
        }

        return Normalizer.normalize(value.replace("®", "").replace("™", ""), Normalizer.Form.NFKD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", " ")
                .trim()
                .replaceAll("\\s+", " ");
    }

    private static void trimTrailingPlatformTokens(final List<String> tokens) {
        boolean removedToken = true;
        while (removedToken && !tokens.isEmpty()) {
            removedToken = false;
            while (!tokens.isEmpty() && PLATFORM_CONNECTOR_TOKENS.contains(tokens.get(tokens.size() - 1))) {
                tokens.remove(tokens.size() - 1);
                removedToken = true;
            }

            for (List<String> suffixTokens : TRAILING_PLATFORM_SUFFIXES) {
                if (endsWith(tokens, suffixTokens)) {
                    for (int index = 0; index < suffixTokens.size(); index++) {
                        tokens.remove(tokens.size() - 1);
                    }
                    removedToken = true;
                    break;
                }
            }
        }
    }

    private static boolean endsWith(final List<String> tokens, final List<String> suffixTokens) {
        if (tokens.size() < suffixTokens.size()) {
            return false;
        }

        for (int index = 0; index < suffixTokens.size(); index++) {
            final String token = tokens.get(tokens.size() - suffixTokens.size() + index);
            if (!token.equals(suffixTokens.get(index))) {
                return false;
            }
        }
        return true;
    }

    private static boolean hasStrongTitleIdentity(final String titleKey) {
        if (titleKey == null || titleKey.isBlank()) {
            return false;
        }

        final List<String> tokens = List.of(titleKey.split(" "));
        return hasNumericIdentityToken(titleKey) || tokens.size() >= 2;
    }

    private static int titleTokenCount(final String titleKey) {
        if (titleKey == null || titleKey.isBlank()) {
            return 0;
        }
        return (int) Stream.of(titleKey.split(" "))
                .filter(token -> !token.isBlank())
                .count();
    }

    private static boolean hasNumericIdentityToken(final String titleKey) {
        return titleKey != null && titleKey.chars().anyMatch(Character::isDigit);
    }

    private static int titleNoisePenalty(final String value) {
        final String trimmed = value == null ? "" : value.trim();
        if (trimmed.isBlank()) {
            return Integer.MAX_VALUE;
        }

        int penalty = 0;
        if (!buildNormalizedTitleKey(trimmed).equals(normalizeKeyPart(trimmed))) {
            penalty += 10;
        }
        penalty += (int) trimmed.chars().filter(ch -> ch == '®' || ch == '™').count() * 2;
        penalty += (int) trimmed.chars().filter(ch -> ch == '(' || ch == ')' || ch == '&' || ch == '/').count();
        return penalty;
    }

    private static boolean providersComparable(final String leftProvider, final String rightProvider) {
        final String leftNormalized = normalizeProviderIdentity(leftProvider);
        final String rightNormalized = normalizeProviderIdentity(rightProvider);
        if (leftNormalized.isBlank() || rightNormalized.isBlank()) {
            return false;
        }

        if (leftNormalized.equals(rightNormalized)) {
            return true;
        }

        final Set<String> leftTokens = new HashSet<>(List.of(leftNormalized.split(" ")));
        final Set<String> rightTokens = new HashSet<>(List.of(rightNormalized.split(" ")));
        return leftTokens.containsAll(rightTokens) || rightTokens.containsAll(leftTokens);
    }

    private static String normalizeProviderIdentity(final String providerName) {
        final List<String> tokens = List.of(normalizeKeyPart(providerName).split(" "));
        return tokens.stream()
                .filter(token -> !token.isBlank())
                .filter(token -> !PROVIDER_NOISE_TOKENS.contains(token))
                .collect(Collectors.joining(" "));
    }

    private static boolean platformsOverlap(final String leftPlatforms, final String rightPlatforms) {
        final Set<String> leftNormalizedPlatforms = parseDelimitedValues(leftPlatforms).stream()
                .map(CatalogGroupingUtils::normalizePlatformLabel)
                .filter(value -> !value.isBlank())
                .collect(Collectors.toSet());

        if (leftNormalizedPlatforms.isEmpty()) {
            return false;
        }

        return parseDelimitedValues(rightPlatforms).stream()
                .map(CatalogGroupingUtils::normalizePlatformLabel)
                .filter(value -> !value.isBlank())
                .anyMatch(leftNormalizedPlatforms::contains);
    }

    private static boolean platformFamiliesOverlap(final String leftPlatforms, final String rightPlatforms) {
        final Set<String> leftFamilies = parseDelimitedValues(leftPlatforms).stream()
                .map(CatalogGroupingUtils::platformFamily)
                .filter(value -> !value.isBlank())
                .collect(Collectors.toSet());
        if (leftFamilies.isEmpty()) {
            return false;
        }

        return parseDelimitedValues(rightPlatforms).stream()
                .map(CatalogGroupingUtils::platformFamily)
                .filter(value -> !value.isBlank())
                .anyMatch(leftFamilies::contains);
    }

    private static String canonicalPlatformLabel(final String platform) {
        final String normalized = normalizeKeyPart(platform);
        if (normalized.isBlank()) {
            return "";
        }
        return switch (normalized) {
            case "ps5", "playstation 5" -> "PS5";
            case "ps4", "ps4tm", "playstation 4" -> "PS4";
            case "ps3", "playstation 3" -> "PS3";
            case "ps vita", "playstation vita", "vita" -> "PS Vita";
            case "xbox series x s", "xbox series" -> "Xbox Series X|S";
            case "xbox one" -> "Xbox One";
            case "nintendo switch" -> "Nintendo Switch";
            case "pc", "pc microsoft windows", "microsoft windows", "windows" -> "PC";
            case "mac" -> "Mac";
            case "linux" -> "Linux";
            case "ios" -> "iOS";
            case "android" -> "Android";
            case "web browser" -> "Web";
            case "google stadia", "stadia" -> "Stadia";
            case "playstation vr2" -> "PS VR2";
            case "playstation vr" -> "PS VR";
            default -> platform == null ? "" : platform.trim();
        };
    }

    private static String normalizePlatformLabel(final String platform) {
        final String normalized = normalizeKeyPart(canonicalPlatformLabel(platform));
        if (normalized.isBlank()) {
            return "";
        }
        return switch (normalized) {
            case "ps5" -> "playstation 5";
            case "ps4" -> "playstation 4";
            case "ps3" -> "playstation 3";
            case "ps vita" -> "playstation vita";
            case "xbox series x s", "xbox series" -> "xbox series";
            case "xbox one" -> "xbox one";
            case "nintendo switch" -> "nintendo switch";
            case "pc" -> "pc";
            default -> normalized;
        };
    }

    private static String platformFamily(final String platform) {
        final String normalizedPlatform = normalizePlatformLabel(platform);
        return switch (normalizedPlatform) {
            case "playstation 5", "playstation 4", "playstation 3", "playstation vita" -> "playstation";
            case "xbox series", "xbox one" -> "xbox";
            case "pc", "mac", "linux" -> "computer";
            case "nintendo switch" -> "nintendo";
            case "android", "ios" -> "mobile";
            case "web" -> "web";
            case "stadia" -> "cloud";
            default -> normalizedPlatform;
        };
    }

    private static boolean releaseYearsMatch(final String leftReleaseDate, final String rightReleaseDate) {
        final Integer leftYear = extractReleaseYear(leftReleaseDate);
        final Integer rightYear = extractReleaseYear(rightReleaseDate);
        return leftYear != null && leftYear.equals(rightYear);
    }

    private static boolean releaseYearsWithin(final String leftReleaseDate, final String rightReleaseDate, final int toleranceYears) {
        final Integer leftYear = extractReleaseYear(leftReleaseDate);
        final Integer rightYear = extractReleaseYear(rightReleaseDate);
        return leftYear != null && rightYear != null && Math.abs(leftYear - rightYear) <= toleranceYears;
    }

    private static Integer extractReleaseYear(final String releaseDate) {
        final String normalized = releaseDate == null ? "" : releaseDate.trim();
        if (normalized.isBlank()) {
            return null;
        }

        final java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("(19|20)\\d{2}").matcher(normalized);
        Integer year = null;
        while (matcher.find()) {
            year = Integer.parseInt(matcher.group());
        }
        return year;
    }

    private static boolean catalogSourcesOverlap(final String leftCatalogSources, final String rightCatalogSources) {
        final Set<String> leftSources = parseDelimitedValues(leftCatalogSources).stream()
                .map(String::toUpperCase)
                .collect(Collectors.toSet());
        if (leftSources.isEmpty()) {
            return false;
        }

        return parseDelimitedValues(rightCatalogSources).stream()
                .map(String::toUpperCase)
                .anyMatch(leftSources::contains);
    }

    private static boolean isCrossSourcePlayStationPair(final String leftCatalogSources, final String rightCatalogSources) {
        return (containsDelimitedValue(leftCatalogSources, "PLAYSTATION_STORE")
                && containsDelimitedValue(rightCatalogSources, "IGDB_PC_XBOX"))
                || (containsDelimitedValue(leftCatalogSources, "IGDB_PC_XBOX")
                && containsDelimitedValue(rightCatalogSources, "PLAYSTATION_STORE"));
    }

    private static boolean hasTitleVariantNoise(final String title) {
        if (title == null || title.isBlank()) {
            return false;
        }
        return !buildNormalizedTitleKey(title).equals(normalizeKeyPart(title))
                || title.indexOf('®') >= 0
                || title.indexOf('™') >= 0;
    }

    private static String sha256Hex(final String input) {
        try {
            final MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            final byte[] digest = messageDigest.digest(input.getBytes(StandardCharsets.UTF_8));
            final StringBuilder stringBuilder = new StringBuilder(digest.length * 2);
            for (byte currentByte : digest) {
                stringBuilder.append(String.format("%02x", currentByte));
            }
            return stringBuilder.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 digest is not available.", exception);
        }
    }
}
