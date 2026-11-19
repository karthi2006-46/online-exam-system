package com.lms.dto;

public class MaterialViewProgressRequest {

    private Integer currentPage;
    private Integer totalPages;

    private Double currentTime;
    private Double duration;

    private Boolean playing;
    private Boolean reachedBottom;

    public Integer getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(Integer currentPage) {
        this.currentPage = currentPage;
    }

    public Integer getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(Integer totalPages) {
        this.totalPages = totalPages;
    }

    public Double getCurrentTime() {
        return currentTime;
    }

    public void setCurrentTime(Double currentTime) {
        this.currentTime = currentTime;
    }

    public Double getDuration() {
        return duration;
    }

    public void setDuration(Double duration) {
        this.duration = duration;
    }

    public Boolean getPlaying() {
        return playing;
    }

    public void setPlaying(Boolean playing) {
        this.playing = playing;
    }

    public Boolean getReachedBottom() {
        return reachedBottom;
    }

    public void setReachedBottom(Boolean reachedBottom) {
        this.reachedBottom = reachedBottom;
    }
}
