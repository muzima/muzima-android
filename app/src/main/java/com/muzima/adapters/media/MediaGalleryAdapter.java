package com.muzima.adapters.media;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.codewaves.stickyheadergrid.StickyHeaderGridAdapter;
import com.muzima.R;
import com.muzima.messaging.customcomponents.ThumbnailView;
import com.muzima.messaging.mms.GlideRequests;
import com.muzima.messaging.mms.Slide;
import com.muzima.messaging.sqlite.database.MediaDatabase.MediaRecord;
import com.muzima.messaging.sqlite.database.loaders.BucketedThreadMediaLoader;
import com.muzima.messaging.sqlite.database.loaders.BucketedThreadMediaLoader.BucketedThreadMedia;
import com.muzima.utils.MediaUtil;

import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class MediaGalleryAdapter extends StickyHeaderGridAdapter {

    @SuppressWarnings("unused")
    private static final String TAG = MediaGalleryAdapter.class.getSimpleName();

    private final Context context;
    private final GlideRequests glideRequests;
    private final Locale locale;
    private final ItemClickListener itemClickListener;
    private final Set<MediaRecord> selected;

    private BucketedThreadMediaLoader.BucketedThreadMedia media;

    private static class ViewHolder extends StickyHeaderGridAdapter.ItemViewHolder {
        ThumbnailView imageView;
        View selectedIndicator;

        ViewHolder(View v) {
            super(v);
            imageView = v.findViewById(R.id.image);
            selectedIndicator = v.findViewById(R.id.selected_indicator);
        }
    }

    private static class HeaderHolder extends StickyHeaderGridAdapter.HeaderViewHolder {
        TextView textView;

        HeaderHolder(View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.text);
        }
    }

    public MediaGalleryAdapter(@NonNull Context context,
                               @NonNull GlideRequests glideRequests,
                               BucketedThreadMedia media,
                               Locale locale,
                               ItemClickListener clickListener) {
        this.context = context;
        this.glideRequests = glideRequests;
        this.locale = locale;
        this.media = media;
        this.itemClickListener = clickListener;
        this.selected = new HashSet<>();
    }

    public void setMedia(BucketedThreadMedia media) {
        this.media = media;
    }

    @Override
    public StickyHeaderGridAdapter.HeaderViewHolder onCreateHeaderViewHolder(ViewGroup parent, int headerType) {
        return new HeaderHolder(LayoutInflater.from(context).inflate(R.layout.media_overview_gallery_item_header, parent, false));
    }

    @Override
    public ItemViewHolder onCreateItemViewHolder(ViewGroup parent, int itemType) {
        return new ViewHolder(LayoutInflater.from(context).inflate(R.layout.media_overview_gallery_item, parent, false));
    }

    @Override
    public void onBindHeaderViewHolder(StickyHeaderGridAdapter.HeaderViewHolder viewHolder, int section) {
        ((HeaderHolder) viewHolder).textView.setText(media.getName(section, locale));
    }

    @Override
    public void onBindItemViewHolder(ItemViewHolder viewHolder, int section, int offset) {
        MediaRecord mediaRecord = media.get(section, offset);
        ThumbnailView thumbnailView = ((ViewHolder) viewHolder).imageView;
        View selectedIndicator = ((ViewHolder) viewHolder).selectedIndicator;
        Slide slide = MediaUtil.getSlideForAttachment(context, mediaRecord.getAttachment());

        if (slide != null) {
            thumbnailView.setImageResource(glideRequests, slide, false, false);
        }

        thumbnailView.setOnClickListener(view -> itemClickListener.onMediaClicked(mediaRecord));
        thumbnailView.setOnLongClickListener(view -> {
            itemClickListener.onMediaLongClicked(mediaRecord);
            return true;
        });

        selectedIndicator.setVisibility(selected.contains(mediaRecord) ? View.VISIBLE : View.GONE);
    }

    @Override
    public int getSectionCount() {
        return media.getSectionCount();
    }

    @Override
    public int getSectionItemCount(int section) {
        return media.getSectionItemCount(section);
    }

    public void toggleSelection(@NonNull MediaRecord mediaRecord) {
        if (!selected.remove(mediaRecord)) {
            selected.add(mediaRecord);
        }
        notifyDataSetChanged();
    }

    public int getSelectedMediaCount() {
        return selected.size();
    }

    @NonNull
    public Collection<MediaRecord> getSelectedMedia() {
        return new HashSet<>(selected);
    }

    public void clearSelection() {
        selected.clear();
        notifyDataSetChanged();
    }

    public void selectAllMedia() {
        for (int section = 0; section < media.getSectionCount(); section++) {
            for (int item = 0; item < media.getSectionItemCount(section); item++) {
                selected.add(media.get(section, item));
            }
        }
        this.notifyDataSetChanged();
    }

    public interface ItemClickListener {
        void onMediaClicked(@NonNull MediaRecord mediaRecord);

        void onMediaLongClicked(MediaRecord mediaRecord);
    }
}
