package com.raffler.app.interfaces;

import com.raffler.app.models.News;

import java.util.List;

/**
 * Created by Ghost on 23/8/2017.
 */

public interface NewsValueListener {
    void onUpdatedNewsList(List<News> newsList);
}