package com.wistron.demo.tool.teddybear.scene.youtube;

/**
 * Created by Vishwajeet on 06/03/2016.
 */
public class VideoItem {
    private String title;
    private String uploadedBy;
    private String thumbnailURL;
    private String id;
    private String duration;
    private String likes;
    private String views;
    private String category;

    @Override
    public String toString() {
        return "[ title = " + title + " ,uploadedBy= " + uploadedBy + " ,thumbnailURL= " + thumbnailURL + " , id=" +
                id + " ,likes = " + likes + ", views = " + views + ", category = " + category + " ]";
    }

    public String getViews() {
        return views;
    }

    public void setViews(String views) {
        this.views = views;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getLikes() {
        return likes;
    }

    public void setLikes(String likes) {
        this.likes = likes;
    }


    public String getId() {
        return id;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }


    public String getUploadedBy() {
        return uploadedBy;
    }

    public void setUploadedBy(String uploadedBy) {
        this.uploadedBy = uploadedBy;
    }

    public String getThumbnailURL() {
        return thumbnailURL;
    }

    public void setThumbnailURL(String thumbnail) {
        this.thumbnailURL = thumbnail;
    }

}
