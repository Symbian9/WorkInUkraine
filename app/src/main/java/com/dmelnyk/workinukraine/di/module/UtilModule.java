package com.dmelnyk.workinukraine.di.module;

import android.content.Context;

import com.dmelnyk.workinukraine.helpers.ImageUtils;
import com.dmelnyk.workinukraine.helpers.CityUtils;
import com.dmelnyk.workinukraine.helpers.NetUtils;

import dagger.Module;
import dagger.Provides;

/**
 * Created by dmitry on 28.03.17.
 */

@Module
public class UtilModule {

    @Provides
    ImageUtils provideImageUtils() {
        return new ImageUtils();
    }

    @Provides
    NetUtils provideNetUtils() {
        return new NetUtils();
    }

    @Provides
    CityUtils provideCityUtils(Context context) { return new CityUtils(context);
    }
}