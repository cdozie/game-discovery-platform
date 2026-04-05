package com.madre.excelweb.service;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CatalogImportGame {
    String externalId;
    String name;
    String description;
    String categories;
    String platforms;
    String price;
    String url;
    String releaseDate;
    String providerName;
    String imageUrl;
}
