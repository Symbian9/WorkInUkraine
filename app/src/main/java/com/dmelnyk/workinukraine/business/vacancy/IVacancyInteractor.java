package com.dmelnyk.workinukraine.business.vacancy;

import android.support.annotation.StringDef;

import com.dmelnyk.workinukraine.data.VacancyModel;
import com.dmelnyk.workinukraine.ui.vacancy.core.VacancyCardViewAdapter;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;
import java.util.Map;

import io.reactivex.Completable;
import io.reactivex.Observable;

/**
 * Created by d264 on 7/28/17.
 */

public interface IVacancyInteractor {

    String VACANCIES_ALL = "all";
    String VACANCIES_FAVORITE = "FAVORITE";
    String VACANCIES_RECENT = "RECENT";
    String VACANCIES_NEW = "FRESH";
    String VACANCIES_SITE_HH = "HEADHUNTERSUA";
    String VACANCIES_SITE_JU = "JOBSUA";
    String VACANCIES_SITE_RU = "RABOTAUA";
    String VACANCIES_SITE_WN = "WORKNEWINFO";
    String VACANCIES_SITE_WU = "WORKUA";
    String DATA_OTHER_TABS = "data for favorite, new and recent tabs";
    String DATA_TAB_SITES = "data to tab sites";

    @StringDef({ VACANCIES_ALL, VACANCIES_FAVORITE, VACANCIES_NEW, VACANCIES_RECENT,
            VACANCIES_SITE_HH, VACANCIES_SITE_JU, VACANCIES_SITE_RU,
            VACANCIES_SITE_WN, VACANCIES_SITE_WU})
    @Retention(RetentionPolicy.CLASS)
    public @interface VacancyResource {}

    Observable<Map<String, Map<String, List<VacancyModel>>>> getAllVacancies(String request);

    /**
     * Returns a list of vacancies
     * @param request Vacancy request in format "request / city"
     * @param table Table where to get data
     * @return Observable List of vacancies
     */
    Observable<List<VacancyModel>> getVacancies(String request, String table);

    /**
     * Removes or saves to favorite vacancy
     * @param vacancy Vacancy that should be treated
     * @param operation Type of operation that should be done (save/remove to FAVORITE table)
     * @return Completable? the result of operation.
     */
    Completable onPopupMenuClicked(VacancyModel vacancy,
                                   @VacancyCardViewAdapter.VacancyPopupMenuType int operation);

}