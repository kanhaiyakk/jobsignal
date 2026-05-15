package com.jobsignal.scraper.client;

import com.jobsignal.scraper.model.RawListing;

import java.util.List;

public interface RemoteOkClient {

    List<RawListing> fetchLatestListings();
}