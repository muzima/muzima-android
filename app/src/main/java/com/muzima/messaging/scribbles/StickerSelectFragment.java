package com.muzima.messaging.scribbles;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.muzima.R;
import com.muzima.messaging.mms.GlideApp;
import com.muzima.messaging.mms.GlideRequests;

public class StickerSelectFragment extends Fragment implements LoaderManager.LoaderCallbacks<String[]> {

    private RecyclerView recyclerView;
    private GlideRequests            glideRequests;
    private String                   assetDirectory;
    private StickerSelectionListener listener;

    public static StickerSelectFragment newInstance(String assetDirectory) {
        StickerSelectFragment fragment = new StickerSelectFragment();

        Bundle args = new Bundle();
        args.putString("assetDirectory", assetDirectory);
        fragment.setArguments(args);

        return fragment;
    }

    @Nullable
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,@Nullable Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.scribble_select_sticker_fragment, container, false);
        this.recyclerView = view.findViewById(R.id.stickers_recycler_view);

        return view;
    }

    @Override
    public void onActivityCreated(Bundle bundle) {
        super.onActivityCreated(bundle);

        this.glideRequests  = GlideApp.with(this);
        this.assetDirectory = getArguments().getString("assetDirectory");

        getLoaderManager().initLoader(0, null, this);
        this.recyclerView.setLayoutManager(new GridLayoutManager(getActivity(), 3));
    }

    @Override
    public Loader<String[]> onCreateLoader(int id, Bundle args) {
        return new StickerLoader(getActivity(), assetDirectory);
    }

    @Override
    public void onLoadFinished(Loader<String[]> loader, String[] data) {
        recyclerView.setAdapter(new StickersAdapter(getActivity(), glideRequests, data));
    }

    @Override
    public void onLoaderReset(Loader<String[]> loader) {
        recyclerView.setAdapter(null);
    }

    public void setListener(StickerSelectionListener listener) {
        this.listener = listener;
    }

    class StickersAdapter extends RecyclerView.Adapter<StickersAdapter.StickerViewHolder> {

        private final GlideRequests glideRequests;
        private final String[]       stickerFiles;
        private final LayoutInflater layoutInflater;

        StickersAdapter(@NonNull Context context, @NonNull GlideRequests glideRequests, @NonNull String[] stickerFiles) {
            this.glideRequests  = glideRequests;
            this.stickerFiles   = stickerFiles;
            this.layoutInflater = LayoutInflater.from(context);
        }

        @Override
        public StickerViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new StickerViewHolder(layoutInflater.inflate(R.layout.scribble_sticker_item, parent, false));
        }

        @Override
        public void onBindViewHolder(StickerViewHolder holder, int position) {
            holder.fileName = stickerFiles[position];

            glideRequests.load(Uri.parse("file:///android_asset/" + holder.fileName))
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .into(holder.image);
        }

        @Override
        public int getItemCount() {
            return stickerFiles.length;
        }

        @Override
        public void onViewRecycled(StickerViewHolder holder) {
            super.onViewRecycled(holder);
            glideRequests.clear(holder.image);
        }

        private void onStickerSelected(String fileName) {
            if (listener != null) listener.onStickerSelected(fileName);
        }

        class StickerViewHolder extends RecyclerView.ViewHolder {

            private String fileName;
            private ImageView image;

            StickerViewHolder(View itemView) {
                super(itemView);
                image = itemView.findViewById(R.id.sticker_image);
                itemView.setOnClickListener(view -> {
                    int pos = getAdapterPosition();
                    if (pos >= 0) {
                        onStickerSelected(fileName);
                    }
                });
            }
        }
    }

    interface StickerSelectionListener {
        void onStickerSelected(String name);
    }
}
