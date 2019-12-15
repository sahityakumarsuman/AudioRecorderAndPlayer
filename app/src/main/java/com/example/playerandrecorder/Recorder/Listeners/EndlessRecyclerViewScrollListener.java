package com.example.playerandrecorder.Recorder.Listeners;


import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

public abstract class EndlessRecyclerViewScrollListener extends RecyclerView.OnScrollListener {

    private static final int STARTING_PAGE_INDEX = 1;

    private int visibleThreshold = 5;

    private int currentPage = 1;
    private int previousTotalItemCount = 0;
    private boolean loading = true;

    private RecyclerView.LayoutManager mLayoutManager;

    public <L extends RecyclerView.LayoutManager> EndlessRecyclerViewScrollListener(L layoutManager) {
        this.mLayoutManager = layoutManager;
        if (layoutManager instanceof StaggeredGridLayoutManager) {
            visibleThreshold = visibleThreshold * ((StaggeredGridLayoutManager) layoutManager).getSpanCount();
        } else if (layoutManager instanceof GridLayoutManager) {
            visibleThreshold = visibleThreshold * ((GridLayoutManager) layoutManager).getSpanCount();
        }
    }

    private int getLastVisibleItem(int[] lastVisibleItemPositions) {
        int maxSize = 0;
        for (int i = 0; i < lastVisibleItemPositions.length; i++) {
            if (i == 0) {
                maxSize = lastVisibleItemPositions[i];
            } else if (lastVisibleItemPositions[i] > maxSize) {
                maxSize = lastVisibleItemPositions[i];
            }
        }
        return maxSize;
    }

    @Override
    public void onScrolled(RecyclerView view, int dx, int dy) {
        int lastVisibleItemPosition = 0;
        int totalItemCount = mLayoutManager.getItemCount();

        if (mLayoutManager instanceof StaggeredGridLayoutManager) {
            int[] lastVisibleItemPositions = ((StaggeredGridLayoutManager) mLayoutManager).findLastVisibleItemPositions(null);
            // get maximum element within the list
            lastVisibleItemPosition = getLastVisibleItem(lastVisibleItemPositions);
        } else if (mLayoutManager instanceof LinearLayoutManager) {
            lastVisibleItemPosition = ((LinearLayoutManager) mLayoutManager).findLastVisibleItemPosition();
        } else if (mLayoutManager instanceof GridLayoutManager) {
            lastVisibleItemPosition = ((GridLayoutManager) mLayoutManager).findLastVisibleItemPosition();
        }


        if (totalItemCount < previousTotalItemCount) {
            this.currentPage = STARTING_PAGE_INDEX;
            this.previousTotalItemCount = totalItemCount;
            if (totalItemCount == 0) {
                this.loading = true;
            }
        }

        if (loading && (totalItemCount > previousTotalItemCount + 1)) {
            loading = false;
            previousTotalItemCount = totalItemCount;
        }


        if (!loading && (lastVisibleItemPosition + visibleThreshold) > totalItemCount && totalItemCount > visibleThreshold) {
            currentPage++;
            onLoadMore(currentPage, totalItemCount);
            loading = true;
        }
    }


    public abstract void onLoadMore(int page, int totalItemsCount);

    public void reset() {
        currentPage = 1;
        previousTotalItemCount = 0;
        loading = true;
    }
}