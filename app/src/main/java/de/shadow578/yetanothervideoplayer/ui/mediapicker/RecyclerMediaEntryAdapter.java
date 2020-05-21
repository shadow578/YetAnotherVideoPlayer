package de.shadow578.yetanothervideoplayer.ui.mediapicker;

import android.content.Context;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.media.ThumbnailUtils;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import de.shadow578.yetanothervideoplayer.ui.mediapicker.views.MediaCardView;
import de.shadow578.yetanothervideoplayer.util.Logging;

/**
 * RecyclerView Adapter for a list of {@link MediaEntry}s
 */
public class RecyclerMediaEntryAdapter extends RecyclerView.Adapter<RecyclerMediaEntryAdapter.MediaCardViewHolder>
{
    /**
     * Context this adapter is in
     */
    private final Context context;

    /**
     * the media entries this adapter will show
     */
    private final List<MediaEntry> mediaEntries;

    /**
     * Create a new media entry adapter for a recylcer view
     * @param context the context ot work in
     * @param mediaEntries the media entries to adapt
     */
    public RecyclerMediaEntryAdapter(@NonNull Context context, @NonNull List<MediaEntry> mediaEntries)
    {
        this.context = context;
        this.mediaEntries = mediaEntries;
    }

    /**
     * Creates a new, empty view for the recyclerView. Is initialized later in onBindViewHolder()
     *
     * @param parent   the parent view
     * @param viewType the type of this view?
     * @return the new, but blank view
     */
    @NonNull
    @Override
    public MediaCardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
    {
        return MediaCardViewHolder.createBlankCard(context, parent);
    }

    /**
     * Set the data of a view in the recyclerview
     *
     * @param viewHolder the ViewHolder that wraps the view
     * @param index      the index of the data in our internal list to use for setting the data of the view
     */
    @Override
    public void onBindViewHolder(@NonNull MediaCardViewHolder viewHolder, int index)
    {
        //check index is in bounds
        if (index < 0 || index >= mediaEntries.size())
        {
            Logging.logE("RecyclerMediaEntryAdapter.onBindViewHolder(): index was out of bounds! index: %d, size: %d", index, mediaEntries.size());
            return;
        }

        //set view data from entry
        viewHolder.setFromMediaEntry(context, mediaEntries.get(index));
    }

    /**
     * @return How many Items this adapter has
     */
    @Override
    public int getItemCount()
    {
        return mediaEntries.size();
    }

    /**
     * ViewHolder for a MediaCardView
     */
    static class MediaCardViewHolder extends RecyclerView.ViewHolder
    {
        /**
         * Inflates a new media card in the given context, and sets the parent
         *
         * @param ctx    the context to use for inflation
         * @param parent the parent to set
         * @return the new view holder that wraps the created view
         */
        static MediaCardViewHolder createBlankCard(@NonNull Context ctx, @NonNull ViewGroup parent)
        {
            //create new view instance
            MediaCardView view = new MediaCardView(ctx);
            //parent.addView(view);
            view.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            return new MediaCardViewHolder(view);
        }

        /**
         * The media card this view holder wraps
         */
        private final MediaCardView mediaCard;

        MediaCardViewHolder(@NonNull MediaCardView view)
        {
            super(view);
            mediaCard = view;
        }

        /**
         * Set the values shown in the wrapped media card from a media entry
         *
         * @param context the context to use for extracting a thumbnail
         * @param entry   the media entry that provides the values to be shown
         */
        void setFromMediaEntry(@NonNull Context context, @NonNull MediaEntry entry)
        {
            //set the title and duration
            mediaCard.setMediaTitle(entry.getTitle());
            mediaCard.setMediaDuration(entry.getDuration());

            //set resolution, or hide it if we dont know
            if (entry.getKind() == MediaEntry.MediaKind.VIDEO && entry.getVideoResolution() != null)
            {
                mediaCard.setShowMediaResolution(true);
                mediaCard.setMediaResolution(entry.getVideoResolution().getWidth(), entry.getVideoResolution().getHeight());
            }
            else
            {
                mediaCard.setShowMediaResolution(false);
            }

            //get the thumbnail
            MediaMetadataRetriever metadataRetriever = new MediaMetadataRetriever();
            metadataRetriever.setDataSource(context, entry.getUri());
            Bitmap frame = metadataRetriever.getFrameAtTime();
            if (frame != null)
            {
                mediaCard.setMediaThumbnail(frame);
            }
        }
    }
}
