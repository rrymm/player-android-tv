package io.github.xwz.tv.adapters;

import android.support.v17.leanback.widget.ImageCardView;
import android.support.v17.leanback.widget.Presenter;
import android.view.ViewGroup;

import io.github.xwz.tv.models.IEpisodeModel;
import io.github.xwz.tv.views.EpisodeCardView;

public class EpisodePresenter extends Presenter {
    private static final String TAG = "EpisodePresenter";

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent) {
        ImageCardView cardView = new ImageCardView(parent.getContext());
        cardView.setFocusable(true);
        cardView.setFocusableInTouchMode(true);
        return new EpisodeCardView(parent.getContext(), cardView);
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, Object item) {
        ((EpisodeCardView) viewHolder).setEpisode((IEpisodeModel) item);
    }

    @Override
    public void onUnbindViewHolder(ViewHolder viewHolder) {
    }
}
