package com.devinolabs.uap.marketplace.application;

import java.util.List;

public record ListingPage(List<ListingRecord> listings, int page, int size, long totalElements) {
}
