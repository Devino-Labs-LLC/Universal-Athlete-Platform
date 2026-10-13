package com.devinolabs.uap.marketplace.infrastructure.web;

import com.devinolabs.uap.marketplace.application.ListingStatus;

public record ListingStatusWriteRequest(ListingStatus status) {
}
