package code.name.monkey.retromusic.adapter.playlist

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.AsyncTask
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentActivity
import androidx.navigation.findNavController
import code.name.monkey.appthemehelper.util.ATHUtil
import code.name.monkey.appthemehelper.util.TintHelper
import code.name.monkey.retromusic.EXTRA_PLAYLIST
import code.name.monkey.retromusic.R
import code.name.monkey.retromusic.adapter.base.AbsMultiSelectAdapter
import code.name.monkey.retromusic.adapter.base.MediaEntryViewHolder
import code.name.monkey.retromusic.db.PlaylistEntity
import code.name.monkey.retromusic.db.PlaylistWithSongs
import code.name.monkey.retromusic.db.SongEntity
import code.name.monkey.retromusic.db.toSongs
import code.name.monkey.retromusic.extensions.hide
import code.name.monkey.retromusic.extensions.show
import code.name.monkey.retromusic.helper.menu.PlaylistMenuHelper
import code.name.monkey.retromusic.helper.menu.SongsMenuHelper
import code.name.monkey.retromusic.interfaces.CabHolder
import code.name.monkey.retromusic.model.Playlist
import code.name.monkey.retromusic.model.Song
import code.name.monkey.retromusic.repository.PlaylistSongsLoader
import code.name.monkey.retromusic.util.AutoGeneratedPlaylistBitmap
import code.name.monkey.retromusic.util.MusicUtil
import code.name.monkey.retromusic.util.RetroColorUtil

class PlaylistAdapter(
    private val activity: FragmentActivity,
    var dataSet: List<PlaylistWithSongs>,
    private var itemLayoutRes: Int,
    cabHolder: CabHolder?
) : AbsMultiSelectAdapter<PlaylistAdapter.ViewHolder, PlaylistWithSongs>(
    activity,
    cabHolder,
    R.menu.menu_playlists_selection
) {

    init {
        setHasStableIds(true)
    }

    fun swapDataSet(dataSet: List<PlaylistWithSongs>) {
        this.dataSet = dataSet
        notifyDataSetChanged()
    }

    override fun getItemId(position: Int): Long {
        return dataSet[position].playlistEntity.playListId.toLong()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(activity).inflate(itemLayoutRes, parent, false)
        return createViewHolder(view)
    }

    fun createViewHolder(view: View): ViewHolder {
        return ViewHolder(view)
    }

    private fun getPlaylistTitle(playlist: PlaylistEntity): String {
        return if (TextUtils.isEmpty(playlist.playlistName)) "-" else playlist.playlistName
    }

    private fun getPlaylistText(playlist: PlaylistWithSongs): String {
        return MusicUtil.getPlaylistInfoString(activity, playlist.songs.toSongs())
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val playlist = dataSet[position]
        holder.itemView.isActivated = isChecked(playlist)
        holder.title?.text = getPlaylistTitle(playlist.playlistEntity)
        holder.text?.text = getPlaylistText(playlist)
        holder.image?.setImageDrawable(getIconRes())
        val isChecked = isChecked(playlist)
        if (isChecked) {
            holder.menu?.hide()
        } else {
            holder.menu?.show()
        }
        //PlaylistBitmapLoader(this, holder, playlist).execute()
    }

    private fun getIconRes(): Drawable = TintHelper.createTintedDrawable(
        activity,
        R.drawable.ic_playlist_play,
        ATHUtil.resolveColor(activity, R.attr.colorControlNormal)
    )

    override fun getItemCount(): Int {
        return dataSet.size
    }

    override fun getIdentifier(position: Int): PlaylistWithSongs? {
        return dataSet[position]
    }

    override fun getName(playlist: PlaylistWithSongs): String {
        return playlist.playlistEntity.playlistName
    }

    override fun onMultipleItemAction(menuItem: MenuItem, selection: List<PlaylistWithSongs>) {
        when (menuItem.itemId) {
            else -> SongsMenuHelper.handleMenuClick(
                activity,
                getSongList(selection),
                menuItem.itemId
            )
        }
    }

    private fun getSongList(playlists: List<PlaylistWithSongs>): List<Song> {
        val songs = mutableListOf<Song>()
        playlists.forEach {
            songs.addAll(it.songs.toSongs())
        }
        return songs
    }

    private fun getSongs(playlist: PlaylistWithSongs): List<SongEntity> =
        mutableListOf<SongEntity>().apply {
            addAll(playlist.songs)
        }

    inner class ViewHolder(itemView: View) : MediaEntryViewHolder(itemView) {
        init {
            image?.apply {
                val iconPadding =
                    activity.resources.getDimensionPixelSize(R.dimen.list_item_image_icon_padding)
                setPadding(iconPadding, iconPadding, iconPadding, iconPadding)
            }
            menu?.setOnClickListener { view ->
                val popupMenu = PopupMenu(activity, view)
                popupMenu.inflate(R.menu.menu_item_playlist)
                popupMenu.setOnMenuItemClickListener { item ->
                    PlaylistMenuHelper.handleMenuClick(activity, dataSet[layoutPosition], item)
                }
                popupMenu.show()
            }

            imageTextContainer?.apply {
                cardElevation = 0f
                setCardBackgroundColor(Color.TRANSPARENT)
            }
        }

        override fun onClick(v: View?) {
            if (isInQuickSelectMode) {
                toggleChecked(layoutPosition)
            } else {
                activity.findNavController(R.id.fragment_container).navigate(
                    R.id.playlistDetailsFragment,
                    bundleOf(EXTRA_PLAYLIST to dataSet[layoutPosition])
                )
            }
        }

        override fun onLongClick(v: View?): Boolean {
            toggleChecked(layoutPosition)
            return true
        }
    }

    class PlaylistBitmapLoader(
        private var adapter: PlaylistAdapter,
        private var viewHolder: ViewHolder,
        private var playlist: Playlist
    ) : AsyncTask<Void, Void, Bitmap>() {

        override fun doInBackground(vararg params: Void?): Bitmap {
            val songs = PlaylistSongsLoader.getPlaylistSongList(adapter.activity, playlist)
            return AutoGeneratedPlaylistBitmap.getBitmap(adapter.activity, songs, false, false)
        }

        override fun onPostExecute(result: Bitmap?) {
            super.onPostExecute(result)
            viewHolder.image?.setImageBitmap(result)
            val color = RetroColorUtil.getColor(
                RetroColorUtil.generatePalette(
                    result
                ),
                ATHUtil.resolveColor(adapter.activity, R.attr.colorSurface)
            )
            viewHolder.paletteColorContainer?.setBackgroundColor(color)
        }
    }

    companion object {
        val TAG: String = PlaylistAdapter::class.java.simpleName
    }
}
