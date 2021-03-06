package com.dmelnyk.workinukraine.utils.parsing;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import com.dmelnyk.workinukraine.application.WorkInUaApplication;
import com.dmelnyk.workinukraine.models.VacancyModel;
import com.dmelnyk.workinukraine.db.DbContract;
import com.dmelnyk.workinukraine.utils.CityUtils;
import com.dmelnyk.workinukraine.utils.NetUtils;
import com.dmelnyk.workinukraine.utils.di.DaggerUtilComponent;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import javax.inject.Inject;

/**
 * Created by dmitry on 07.03.17.
 */

public class ParserRabotaUa {
    private static final String TAG = "TAG.ParserRabotaUa";
    final int year = Calendar.getInstance().get(Calendar.YEAR);

    @Inject
    NetUtils netUtils;

    @Inject
    CityUtils cities;

    public ParserRabotaUa(Context context) {
        DaggerUtilComponent.builder()
                .applicationComponent(WorkInUaApplication.get(context).getAppComponent())
                .build()
                .inject(this);
    }

    /**
     * Parse page with given jobRequest parameters.
     * @param request - Request in format "search request / city".
     * @return list of VacancyModule's or empty ArrayList
     */
    @NonNull
    public List<VacancyModel> getJobs(String request) {
        String jobRequest = request.split(" / ")[0];
        String city = request.split(" / ")[1];

        Log.d(TAG, "started getJobs(). City = " + city + " request = " + jobRequest);

        List<VacancyModel> vacancies = new ArrayList<>();

        String cityId = cities.getCityId(CityUtils.SITE.RABOTAUA, city);
        String correctedRequest = netUtils.replaceSpacesWithPlus(jobRequest);
        String urlRequest = "https://rabota.ua/jobsearch/vacancy_list?regionId=" +
                cityId + "&keyWords=" + correctedRequest;


        String response = netUtils.getHtmlPage(urlRequest);
        if (response == null) {
            Log.e(TAG, "Server not response!");
            return vacancies;
        }

        Document doc = Jsoup.parse(response);
        Elements links = doc.getElementsByTag("div").select("tr");
        for (Element link : links) {

            String date = link.select("p[class=\"f-vacancylist-agotime f-text-light-gray fd-craftsmen\"]").text();
            String title = link.select("a[href]").text();
            String shortUrl = link.select("a[href]").attr("href");
            if (shortUrl.contains("/vacancy") && shortUrl.contains("/company")) {
                String url = "https://rabota.ua" + shortUrl;

                VacancyModel vacancyModel = VacancyModel.builder()
                        .setDate(date)
                        .setIsFavorite(false)
                        .setTimeStatus(1) // new
                        .setRequest(request)
                        .setSite(DbContract.SearchSites.SITES[2])
                        .setTitle(title)
                        .setUrl(url)
                        .build();
                vacancies.add(vacancyModel);
            }
        }

        // check if any vacancy contain needed request
        if (vacancies.size() == 20) {
            boolean isAnyVacancyContainsRequest = false;
            for (VacancyModel vacancy : vacancies) {
                if (vacancy.title().toLowerCase()
                        .contains(request.split(" / ")[0].toLowerCase())) {
                    isAnyVacancyContainsRequest = true;
                    break;
                }
            }

            if (!isAnyVacancyContainsRequest) return new ArrayList<>();
        }
        Log.d(TAG, "found " + vacancies.size() + " vacancies");
        return vacancies;
    }
}
