package com.madre.excelweb.service.api;

import com.madre.excelweb.jpa.entity.GameData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.RetryContext;
import org.springframework.retry.support.RetryTemplate;

import java.util.Iterator;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class PsnApiIterator implements Iterator<List<GameData>> {

    public static final Integer MAX_DATA = 8192;
    private static final String URL_TEMPLATE = "/STORE-MSF77008-ALLGAMES?size=%s&start=%s&gameContentType=games&sort=release_date&direction=desc"
            .formatted(MAX_DATA, "%s");
    private final PsnApiReader psnApiReader;
    private final RetryTemplate retryTemplate;
    private Integer currentOffset = 0;
    private List<GameData> nextSetOfData;

    @Override
    public boolean hasNext() {
        nextSetOfData = retryTemplate.execute(this::getNextSetOfData);
        return nextSetOfData != null && !nextSetOfData.isEmpty();
    }

    @Override
    public List<GameData> next() {
        return nextSetOfData;
    }

    private List<GameData> getNextSetOfData(RetryContext retryContext) {
        final int retryCount = retryContext.getRetryCount() + 1;

        final String nextPageLink = generateNextPageQuery(currentOffset);

        log.info("Getting next set of data from {}: Attempt #{}", nextPageLink, retryCount);
        try {

            List<GameData> data = psnApiReader.getGames(nextPageLink);

            log.info("Successfully retrieved next set of data from {} on Attempt #{}", nextPageLink, retryCount);

            currentOffset += data.size();

            return data;
        } catch (Exception e) {
            log.error("Error getting next set of data on attempt #{}", retryCount, e);
            throw e;
        }

    }

    private String generateNextPageQuery(Integer offset) {
        return String.format(URL_TEMPLATE, offset);
    }


}
