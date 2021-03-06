package com.dmelnyk.workinukraine.ui.search.business;

import com.dmelnyk.workinukraine.models.RequestModel;

import java.util.List;

import io.reactivex.Completable;
import io.reactivex.Observable;

/**
 * Created by d264 on 6/25/17.
 */

public interface ISearchInteractor {
    /**
     * Returns list of RequestModel items
     * Request format in single string: "request / city"
     * @return The Observable that emits list of search requests
     */
    Observable<List<RequestModel>> getRequests();


    /**
     * Saves request and returns -1 if error happened.
     * @param request The search request in "request / city" format
     */
    Completable saveRequest(String request);

    /**
     * Removes request
     * @param request Te search request in "request / city" format
     */
    void removeRequest(String request);

    Completable clearAllRequests();

    Completable editRequest(String previousRequest, String newRequest);
}
