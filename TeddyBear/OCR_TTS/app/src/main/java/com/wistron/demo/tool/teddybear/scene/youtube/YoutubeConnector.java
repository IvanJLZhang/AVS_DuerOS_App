package com.wistron.demo.tool.teddybear.scene.youtube;

import android.content.Context;
import android.util.Log;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.Joiner;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.SearchListResponse;
import com.google.api.services.youtube.model.SearchResult;
import com.google.api.services.youtube.model.Thumbnail;
import com.google.api.services.youtube.model.Video;
import com.google.api.services.youtube.model.VideoListResponse;
import com.wistron.demo.tool.teddybear.R;
import com.wistron.demo.tool.teddybear.scene.helper.SubscriptionKey;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class YoutubeConnector {
    private YouTube youtube;
    private YouTube.Search.List query;
    Context context;
    private static VideoItem item;
    private static List<VideoItem> searchResults;
    private static String KEY;
    private static final long NUMBER_OF_VIDEOS_RETURNED = 15;

    public YoutubeConnector(Context context, String Keywords) {
        this.context = context;
        KEY = context.getString(SubscriptionKey.getYoutubeDeveloperKey());
        try {
            youtube = new YouTube.Builder(new NetHttpTransport(),
                    new JacksonFactory(), new HttpRequestInitializer() {
                @Override
                public void initialize(HttpRequest hr) throws IOException {
                }
            }).setApplicationName(context.getString(R.string.app_name)).build();
            String queryTerm = Keywords;
            Log.d("Keywords", queryTerm);
            YouTube.Search.List search = youtube.search().list("id,snippet");
            String apiKey = KEY;
            search.setKey(apiKey);
            search.setQ(queryTerm);
            search.setType("video");
            search.setFields("items(id/videoId)");
            search.setMaxResults(NUMBER_OF_VIDEOS_RETURNED);
            SearchListResponse searchResponse = search.execute();
            List<SearchResult> searchResultList = searchResponse.getItems();
            List<String> videoIds = new ArrayList<String>();

            if (searchResultList != null) {
                for (SearchResult searchResult : searchResultList) {
                    videoIds.add(searchResult.getId().getVideoId());
                    Log.d("VideoId", searchResult.getId().getVideoId());
                }
                Joiner stringJoiner = Joiner.on(',');
                String videoId = stringJoiner.join(videoIds);
                Log.d("id", videoId);
                YouTube.Videos.List listVideosRequest = youtube.videos().list("snippet,contentDetails,statistics").setId(videoId);
                listVideosRequest.setKey(KEY);
                VideoListResponse listResponse = listVideosRequest.execute();
                List<Video> videoList = listResponse.getItems();
                if (videoList != null) {
                    Print(videoList.iterator(), queryTerm);
                }
            }
        } catch (GoogleJsonResponseException e) {
            e.printStackTrace();
            Log.i("King", "There was a service error: " + e.getDetails().getCode() + " : "
                    + e.getDetails().getMessage());
        } catch (IOException e) {
            e.printStackTrace();
            Log.i("King", "There was an IO error: " + e.getCause() + " : " + e.getMessage());
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private static void Print(Iterator<Video> iteratorVideoResults, String query) {
        searchResults = new ArrayList<VideoItem>();
        if (!iteratorVideoResults.hasNext()) {
            System.out.println(" There aren't any results for your query.");
        }
        while (iteratorVideoResults.hasNext()) {
            Video singleVideo = iteratorVideoResults.next();
            String categoryId = singleVideo.getSnippet().getCategoryId();
            Category category = new Category();
            String categoryTitle = category.getCategory(categoryId);
            Thumbnail thumbnail = singleVideo.getSnippet().getThumbnails().getDefault();
            String duration = singleVideo.getContentDetails().getDuration().replace('M', ':');
            String editedDuration = duration.replaceAll("[^\\d:]", "");

            item = new VideoItem();
            item.setId(singleVideo.getId());
            item.setTitle(singleVideo.getSnippet().getTitle());
            item.setUploadedBy(singleVideo.getSnippet().getChannelTitle());
            item.setThumbnailURL(thumbnail.getUrl());
            item.setDuration(editedDuration);
            item.setViews(singleVideo.getStatistics().getViewCount().toString());
            item.setLikes(singleVideo.getStatistics().getLikeCount().toString());
            item.setCategory(categoryTitle);
            searchResults.add(item);
        }
    }

    public VideoItem getItem() {
        return item;
    }

    public List<VideoItem> getVideos() {
        return searchResults;
    }

}
