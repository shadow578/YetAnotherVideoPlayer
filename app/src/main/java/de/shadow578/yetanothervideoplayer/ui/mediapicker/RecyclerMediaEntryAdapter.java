package de.shadow578.yetanothervideoplayer.ui.mediapicker;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import java.io.IOException;
import java.util.List;

import de.shadow578.yetanothervideoplayer.R;
import de.shadow578.yetanothervideoplayer.ui.mediapicker.views.MediaCardView;
import de.shadow578.yetanothervideoplayer.util.Logging;

/**
 * RecyclerView Adapter for a list of {@link MediaEntry}s
 */
public class RecyclerMediaEntryAdapter extends RecyclerView.Adapter<RecyclerMediaEntryAdapter.MediaCardViewHolder>
{
    /**
     * Click listener for clicks on {@link MediaCardView}s created by the {@link RecyclerMediaEntryAdapter}
     */
    public interface CardClickListener
    {
        /**
         * Called when the media card was clicked
         *
         * @param cardMedia the media entry of the card
         */
        void onMediaCardClicked(MediaEntry cardMedia);
    }

    /**
     * Context this adapter is in
     */
    @NonNull
    private final Context context;

    /**
     * the media entries this adapter will show
     */
    @NonNull
    private final List<MediaEntry> mediaEntries;

    /**
     * Click listener for clicks on media cards
     */
    @Nullable
    private final CardClickListener clickListener;

    /**
     * default thumbnail to use when no actual thumbnail was found
     */
    @Nullable
    private Drawable placeholderThumbnail;

    /**
     * Create a new media entry adapter for a recylcer view
     *
     * @param context       the context ot work in
     * @param mediaEntries  the media entries to adapt
     * @param clickListener the click listener that is called when a media card is clicked
     */
    RecyclerMediaEntryAdapter(@NonNull Context context, @NonNull List<MediaEntry> mediaEntries, @Nullable CardClickListener clickListener)
    {
        this.context = context;
        this.mediaEntries = mediaEntries;
        this.clickListener = clickListener;
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
        final MediaEntry entry = mediaEntries.get(index);
        viewHolder.setFromMediaEntry(context, entry, placeholderThumbnail);

        //set click listener of card
        viewHolder.mediaCard.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                if (clickListener != null)
                    clickListener.onMediaCardClicked(entry);
            }
        });
    }

    /**
     * Sets the placeholder thumbnail
     *
     * @param placeholderThumbnail the placeholder thumbnail to use
     */
    void setPlaceholderThumbnail(@Nullable Drawable placeholderThumbnail)
    {
        this.placeholderThumbnail = placeholderThumbnail;
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
            //inflate the entries layout from xml
            View view = LayoutInflater.from(ctx).inflate(R.layout.mediapicker_recycler_entry_layout, parent, false);

            //find media card in view
            MediaCardView cardView = view.findViewById(R.id.mediapicker_recycler_entry_card);

            //create a new view holder
            return new MediaCardViewHolder(view, cardView);
        }

        /**
         * The media card this view holder wraps
         */
        private final MediaCardView mediaCard;

        MediaCardViewHolder(@NonNull View view, @NonNull MediaCardView card)
        {
            super(view);
            mediaCard = card;
        }

        /**
         * Set the values shown in the wrapped media card from a media entry
         *
         * @param context              the context to use for extracting a thumbnail
         * @param entry                the media entry that provides the values to be shown
         * @param placeholderThumbnail the default (placeholder) thumbnail to use until (or if no) real thumbnail was loaded. null just shows a blank thumbnail by default or while loading
         */
        void setFromMediaEntry(@NonNull Context context, @NonNull MediaEntry entry, @Nullable Drawable placeholderThumbnail)
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

            //clear thumbnail first
            mediaCard.setMediaThumbnail(placeholderThumbnail);

            //set thumbnail
            AsyncLoadThumbnailTask.Parameters params = new AsyncLoadThumbnailTask.Parameters(context, entry, mediaCard);
            new AsyncLoadThumbnailTask().execute(params);
        }

        /**
         * Async task for loading thumbnails for media cards
         */
        private static class AsyncLoadThumbnailTask extends AsyncTask<AsyncLoadThumbnailTask.Parameters, String, Bitmap>
        {
            /**
             * Task Parameters for the {@link AsyncLoadThumbnailTask}
             */
            private static class Parameters
            {
                @NonNull
                private final Context context;

                @NonNull
                private final MediaEntry entry;

                @NonNull
                private final MediaCardView mediaCard;

                Parameters(@NonNull Context context, @NonNull MediaEntry entry, @NonNull MediaCardView mediaCard)
                {
                    this.context = context;
                    this.entry = entry;
                    this.mediaCard = mediaCard;
                }
            }

            /**
             * own task parameters
             */
            private Parameters params;

            @Override
            protected Bitmap doInBackground(Parameters... parameters)
            {
                params = parameters[0];

                //use already loaded thumbnail
                if (params.entry.getThumbnail() != null)
                {
                    return params.entry.getThumbnail();
                }
                else
                {
                    return loadThumbnail(params.context, params.entry);
                }
            }

            @Override
            protected void onPostExecute(Bitmap thumbnail)
            {
                if (thumbnail != null)
                {
                    params.mediaCard.setMediaThumbnail(thumbnail);
                    params.entry.setThumbnail(thumbnail);
                }
            }

            /**
             * Load the thumbnail for the media entry
             *
             * @param context the context to load the thumbnail with
             * @param entry   the media entry to load the thumbnail for
             * @return the thumbnail that was loaded, or null if no thumbnail could be loaded
             */
            @Nullable
            private Bitmap loadThumbnail(@NonNull Context context, @NonNull MediaEntry entry)
            {
                //get thumbnail
                Bitmap thumbnail = null;

                //try to get thumbnail from MediaStore
                try (Cursor thumbCursor = MediaStore.Images.Thumbnails.queryMiniThumbnails(context.getContentResolver(), entry.getUri(), MediaStore.Images.Thumbnails.MINI_KIND, null))
                {
                    //check cursor is ok
                    if (thumbCursor != null && thumbCursor.getCount() > 0)
                    {
                        //move to first entry
                        thumbCursor.moveToFirst();

                        //get thumbnail uri
                        String thumbUri = thumbCursor.getString(thumbCursor.getColumnIndex(MediaStore.Images.Thumbnails.DATA));

                        if (thumbUri != null)
                        {
                            //load thumbnail as bitmap
                            thumbnail = MediaStore.Images.Media.getBitmap(context.getContentResolver(), Uri.parse(thumbUri));
                        }

                    }
                }
                catch (IOException e)
                {
                    //thumbnail from MediaStore failed :(
                    Logging.logW("failed to load thumbnail for %s from MediaStore! Will fall back to MediaMetadataResolver...", entry.toString());
                }

                //fallback to MediaMetadataRetriever for getting a thumbnail
                if (thumbnail == null)
                {
                    Logging.logD("Fallback to MediaMetadataRetriever for retrieving thumbnail for %s", entry.toString());
                    MediaMetadataRetriever metadataRetriever = new MediaMetadataRetriever();
                    metadataRetriever.setDataSource(context, entry.getUri());
                    thumbnail = metadataRetriever.getFrameAtTime();
                }

                return thumbnail;
            }
        }
    }
}
