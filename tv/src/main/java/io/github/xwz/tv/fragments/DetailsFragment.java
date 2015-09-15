package io.github.xwz.tv.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Point;
import android.os.Bundle;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.HeaderItem;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.ListRowPresenter;
import android.support.v17.leanback.widget.OnItemViewClickedListener;
import android.support.v17.leanback.widget.OnItemViewSelectedListener;
import android.support.v17.leanback.widget.Presenter;
import android.support.v17.leanback.widget.Row;
import android.support.v17.leanback.widget.RowPresenter;
import android.support.v17.leanback.widget.VerticalGridView;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.github.xwz.tv.R;
import io.github.xwz.tv.Utils;
import io.github.xwz.tv.adapters.EpisodePresenter;
import io.github.xwz.tv.content.IContentManager;
import io.github.xwz.tv.models.IEpisodeModel;
import io.github.xwz.tv.views.EpisodeDetailsView;

public abstract class DetailsFragment extends android.support.v17.leanback.app.RowsFragment {

    private static final String TAG = "DetailsFragment";
    private EpisodeDetailsView mDetailView;
    private int mHeaderHeight;
    private IEpisodeModel mCurrentEpisode;
    private IEpisodeModel mLoadedEpisode;
    private ArrayObjectAdapter otherEpisodes;
    private boolean loadedOtherEpisodes = false;
    private List<String> mOtherEpisodeUrls = new ArrayList<>();

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG, "Action: " + action + ", tag: " + intent.getStringExtra(IContentManager.CONTENT_TAG));
            if (IContentManager.CONTENT_EPISODE_DONE.equals(action)) {
                updateEpisodeData(intent);
            }
            if (IContentManager.CONTENT_EPISODE_ERROR.equals(action)) {
                Utils.showToast(getActivity(), "Unable to find episode details.");
            }
        }
    };

    protected abstract IContentManager getContentManger();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        IEpisodeModel episode = (IEpisodeModel) getActivity().getIntent().getSerializableExtra(IContentManager.CONTENT_ID);
        if (episode == null) {
            episode = getEpisodeFromGlobalSearchIntent();
        }
        if (episode != null) {
            setupEpisode(episode);
        } else {
            Log.e(TAG, "No episode set.");
        }
    }

    private void setupEpisode(IEpisodeModel episode) {
        mLoadedEpisode = episode;
        setCurrentEpisode(episode);

        Point size = Utils.getDisplaySize(getActivity());
        mHeaderHeight = Math.round(size.y * 0.475f);

        setupAdapter(episode);
        setupListeners();
    }

    private IEpisodeModel getEpisodeFromGlobalSearchIntent() {
        Intent intent = getActivity().getIntent();
        String action = intent.getAction();
        if (IContentManager.GLOBAL_SEARCH_INTENT.equals(action)) {
            Log.d(TAG, "getEpisodeFromGlobalSearchIntent");
            Bundle data = intent.getExtras();
            if (data != null) {
                String href = data.getString(IContentManager.KEY_EXTRA_NAME);
                Log.d(TAG, "Search result: " +href);
                return getContentManger().getEpisode(href);
            }
            Log.w(TAG, "Unable to find href from search result");
        }
        return null;
    }

    private void setupListeners() {
        setOnItemViewClickedListener(getItemClickedListener());
        setOnItemViewSelectedListener(getItemSelectedListener());
    }

    private void setupAdapter(IEpisodeModel episode) {
        ArrayObjectAdapter adapter = new ArrayObjectAdapter(new ListRowPresenter());
        otherEpisodes = new ArrayObjectAdapter(new EpisodePresenter());
        otherEpisodes.add(0, episode);
        adapter.add(new ListRow(new HeaderItem(0, null), otherEpisodes));
        setAdapter(adapter);
    }

    private void setCurrentEpisode(IEpisodeModel episode) {
        Log.d(TAG, "Showing details: " + episode);
        if (!episode.equals(mCurrentEpisode)) {
            mCurrentEpisode = episode;
            getContentManger().fetchEpisode(episode);
            if (mDetailView != null) {
                mDetailView.setEpisode(episode);
            }
        }
    }

    private void updateEpisodeData(Intent intent) {
        String href = intent.getStringExtra(IContentManager.CONTENT_TAG);
        IEpisodeModel ep = getContentManger().getEpisode(href);
        if (ep != null) {
            if (ep.equals(mCurrentEpisode)) {
                mCurrentEpisode.merge(ep);
                mDetailView.updateEpisode(mCurrentEpisode);
            }
            if (ep.equals(mLoadedEpisode) && !loadedOtherEpisodes) {
                updateRelatedEpisodes(ep.getOtherEpisodes());
                loadedOtherEpisodes = true;
                mOtherEpisodeUrls = mLoadedEpisode.getOtherEpisodeUrls(IContentManager.OTHER_EPISODES);
                Log.d(TAG, "Other episodes:" + mOtherEpisodeUrls);
            }
        }
    }

    private void updateRelatedEpisodes(Map<String, List<IEpisodeModel>> others) {
        boolean updated = false;
        ArrayObjectAdapter adapter = (ArrayObjectAdapter) getAdapter();
        for (Map.Entry<String, List<IEpisodeModel>> list : others.entrySet()) {
            String title = list.getKey();
            Log.d(TAG, "More: " + title);
            if (IContentManager.OTHER_EPISODES.equals(title)) {
                otherEpisodes.addAll(otherEpisodes.size(), list.getValue());
            } else {
                ArrayObjectAdapter more = new ArrayObjectAdapter(new EpisodePresenter());
                more.addAll(0, list.getValue());
                adapter.add(new ListRow(new HeaderItem(0, title), more));
            }
            updated = true;
        }
        if (updated) {
            adapter.notifyArrayItemRangeChanged(0, adapter.size());
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        if (view instanceof FrameLayout) {
            insertHeader(inflater, (FrameLayout) view);
        }
        return view;
    }

    private void insertHeader(LayoutInflater inflater, FrameLayout container) {
        VerticalGridView grid = findFirstGrid(container);
        if (grid != null) {
            View header = inflater.inflate(R.layout.episode_details_view, container, false);
            View bottom = inflater.inflate(R.layout.dark_gradient, container, false);
            container.addView(header, 0);
            container.addView(bottom, 1);
            setupGridAlignment(grid);
            setupHeaderView(grid, header);
            setupGradient(bottom);
        } else {
            Utils.showToast(getActivity(), "No Grid Found!");
        }
    }

    private void setupGradient(View bottom) {
        int height = getResources().getDimensionPixelSize(R.dimen.episode_detail_padding_bottom);
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) bottom.getLayoutParams();
        lp.topMargin = mHeaderHeight;
        lp.height = height;
        bottom.setLayoutParams(lp);
    }

    private void setupHeaderView(VerticalGridView grid, View header) {
        mDetailView = new EpisodeDetailsView(getActivity(), header);
        if (mCurrentEpisode != null) {
            mDetailView.setEpisode(mCurrentEpisode);
        }
        int height = getResources().getDimensionPixelSize(R.dimen.episode_detail_padding_bottom);
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) header.getLayoutParams();
        lp.height = mHeaderHeight + height;
        header.setLayoutParams(lp);
    }

    private void setupGridAlignment(VerticalGridView grid) {
        int cardHeight = getResources().getDimensionPixelSize(R.dimen.card_height);
        int titleHeight = getResources().getDimensionPixelSize(R.dimen.lb_browse_header_height);
        grid.setWindowAlignment(VerticalGridView.WINDOW_ALIGN_LOW_EDGE);
        grid.setWindowAlignmentOffset(cardHeight + titleHeight);
        grid.setWindowAlignmentOffsetPercent(VerticalGridView.WINDOW_ALIGN_OFFSET_PERCENT_DISABLED);

        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) grid.getLayoutParams();
        lp.topMargin = mHeaderHeight;
        grid.setLayoutParams(lp);
    }

    private VerticalGridView findFirstGrid(ViewGroup container) {
        for (int i = 0, k = container.getChildCount(); i < k; i++) {
            View view = container.getChildAt(i);
            if (view instanceof VerticalGridView) {
                return (VerticalGridView) view;
            }
        }
        return null;
    }

    protected abstract OnItemViewClickedListener getItemClickedListener();

    private OnItemViewSelectedListener getItemSelectedListener() {
        return new OnItemViewSelectedListener() {
            @Override
            public void onItemSelected(Presenter.ViewHolder itemViewHolder, Object item, RowPresenter.ViewHolder rowViewHolder, Row row) {
                Log.d(TAG, "Selected item:" + item);
                if (item instanceof IEpisodeModel) {
                    setCurrentEpisode((IEpisodeModel) item);
                }
            }
        };
    }

    @Override
    public void onResume() {
        super.onResume();
        registerReceiver();
    }

    public void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(receiver);
    }

    private void registerReceiver() {
        Log.i(TAG, "Register receiver");
        IntentFilter filter = new IntentFilter();
        filter.addAction(IContentManager.CONTENT_EPISODE_START);
        filter.addAction(IContentManager.CONTENT_EPISODE_DONE);
        filter.addAction(IContentManager.CONTENT_EPISODE_ERROR);
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(receiver, filter);
    }
}
