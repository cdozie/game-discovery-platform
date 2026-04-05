package com.madre.excelweb.service.api;

import com.madre.excelweb.service.CatalogImportGame;

import java.util.List;

public record IgdbGameBatch(List<CatalogImportGame> games, int start, int size) {
}
