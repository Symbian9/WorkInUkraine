package com.dmelnyk.workinukraine.ui.vacancy_list;

import android.util.Log;

import com.dmelnyk.workinukraine.business.vacancy_list.IVacancyListInteractor;
import com.dmelnyk.workinukraine.models.VacancyModel;
import com.dmelnyk.workinukraine.ui.vacancy_list.core.VacancyCardViewAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

/**
 * Created by d264 on 7/28/17.
 */

public class VacancyPresenter implements Contract.IVacancyPresenter {

    private final IVacancyListInteractor interactor;
    private Contract.IVacancyView view;

    private Disposable saveOrRemoveDisposable;
    private Disposable vacanciesDisposable;
    private Disposable disposableFavorites;

    private static Map<String, List<VacancyModel>> sDataCache;
    private static String sError;
    private static boolean sIsDisplayed;
    private static String mRequest;
    private static List<VacancyModel> sFavoriteVacanciesCache;

    public VacancyPresenter(IVacancyListInteractor interactor, String request) {
        this.interactor = interactor;
        this.mRequest = request;
        // Don't get data from db if we have saved data
        if (sDataCache == null) {
            getAllVacancies(request);
        }
    }

    @Override
    public void bindView(Contract.IVacancyView view, String request) {
        this.view = view;

        // Don't do anything after orientation changes return
        Log.e("!!!", "sIsDislpayed=" + sIsDisplayed);
        if (sIsDisplayed) return;

        // If request to database has been already received get the result in cache
        if (sDataCache != null) {
            // successful result
            Log.e("!!!", "VacancyPresenter. sDataCache=" + sDataCache);
            displayData(sDataCache);
        } else if (sError != null) {
            // error result
            view.showErrorMessage(sError);
            sError = null;
        }
    }

    @Override
    public void bindJustView(Contract.IVacancyView view) {
        this.view = view;
    }

    private void displayData(Map<String, List<VacancyModel>> vacanciesMap) {
        Map<String, Integer> tabVacancyCount = new HashMap<>();
        String[] tabTitles = null;
        int buttonTabType = 0; // 1 - only new vacancies, 2 - new and recent, 3 - only recent

        // Counting vacancies count in all tab sites
        int siteTabsCount = vacanciesMap.get(IVacancyListInteractor.DATA_ALL).size();
        // Exit from activity if no vacancies found
        if (siteTabsCount == 0) {
            view.exitActivity();
            clear();
            return;
        }

        // Counts vacancies
        int newVacanciesCount = vacanciesMap.get(IVacancyListInteractor.DATA_NEW).size();
        int recentVacanciesCount = vacanciesMap.get(IVacancyListInteractor.DATA_RECENT).size();
        int favoriteVacanciesCount = vacanciesMap.get(IVacancyListInteractor.DATA_FAVORITE).size();

        tabVacancyCount.put(IVacancyListInteractor.DATA_ALL, siteTabsCount);
        tabVacancyCount.put(IVacancyListInteractor.DATA_NEW, newVacanciesCount);
        tabVacancyCount.put(IVacancyListInteractor.DATA_RECENT, recentVacanciesCount);
        tabVacancyCount.put(IVacancyListInteractor.DATA_FAVORITE, favoriteVacanciesCount);


        if (newVacanciesCount != 0 && recentVacanciesCount == 0) {
            tabTitles = interactor.getTitles(IVacancyListInteractor.TITLE_NEW);
            buttonTabType = 1;
        }

        if (newVacanciesCount != 0 && recentVacanciesCount != 0) {
            tabTitles = interactor.getTitles(IVacancyListInteractor.TITLE_NEW_AND_RECENT);
            buttonTabType = 2;
        }

        if (newVacanciesCount == 0) {
            tabTitles = interactor.getTitles(IVacancyListInteractor.TITLE_RECENT);
            buttonTabType = 3;
        }

        view.displayTabFragment(tabTitles, tabVacancyCount, buttonTabType, vacanciesMap);
    }

    private void getAllVacancies(String request) {
        interactor.getAllVacancies(request)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(vacanciesMap -> {
                    sDataCache = new HashMap<>(vacanciesMap);
                    if (view != null) {
                        sIsDisplayed = true;
                        displayData(sDataCache);
                    }
                }, throwable -> {
                    Timber.e(throwable.getStackTrace().toString());
                    if (view != null) {
                        view.showErrorMessage(throwable.getMessage());
                    } else {
                        // if view hasn't been initialized yet save error msg to cache
                        sError = throwable.getMessage();
                    }
                });
    }

    @Override
    public void unbindView() {
        view = null;
        sIsDisplayed = false;
        // TODO: in bindView restore listeners
        if (saveOrRemoveDisposable != null) {
            saveOrRemoveDisposable.dispose();
        }
        if (vacanciesDisposable != null) {
            vacanciesDisposable.dispose();
        }
    }

    @Override
    public void onItemPopupMenuClicked(VacancyModel vacancy,
                                       @VacancyCardViewAdapter.VacancyPopupMenuType int type) {
        if (type == VacancyCardViewAdapter.MENU_SHARE) {
            view.createShareIntent(vacancy);
        } else {
            saveOrRemoveDisposable = interactor.onPopupMenuClicked(vacancy, type)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(() -> {
                        updateFavorite();
                        view.showResultingMessage(type);
                    }, throwable -> {
                        view.showErrorMessage(throwable.getMessage());
                    });
        }
    }

    // This method will update favorites after adding/removing vacancies in proper db automatically
    private void updateFavorite() {
        disposableFavorites = interactor.getFavoriteVacancies(mRequest)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(vacancies -> {
                    Log.e("999", "updateFavorites is called");
                    view.updateFavoriteTab(vacancies);
                    sFavoriteVacanciesCache = new ArrayList<VacancyModel>(vacancies);
                }, throwable -> view.showErrorMessage(throwable.getMessage()));
    }

    @Override
    public void clear() {
        sDataCache = null;
        sFavoriteVacanciesCache = null;
        sError = null;
        sIsDisplayed = false;
        interactor.clear();
    }
}