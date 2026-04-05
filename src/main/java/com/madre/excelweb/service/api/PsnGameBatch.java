package com.madre.excelweb.service.api;

import com.madre.excelweb.service.CatalogImportGame;

import java.util.List;

public record PsnGameBatch(List<CatalogImportGame> games, int totalResults, int start, int size) {
}
