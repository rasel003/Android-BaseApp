/*
 * Copyright 2019 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.rasel.androidbaseapp.ui.counter

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.rasel.androidbaseapp.R
import com.rasel.androidbaseapp.databinding.FragmentCounterBinding
import com.rasel.androidbaseapp.util.hideSystemUi
import timber.log.Timber
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * A [Fragment] that displays a list of emails.
 */
class CounterFragment : Fragment() {

    private lateinit var binding: FragmentCounterBinding
    private val playbackStateListener: Player.Listener = playbackStateListener()
    private var player: Player? = null

    private var playWhenReady = true
    private var mediaItemIndex = 0
    private var playbackPosition = 0L

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentCounterBinding.inflate(inflater, container, false).apply {
            lifecycleOwner = viewLifecycleOwner
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val internationalDateTime = "2024-02-25T00:05:50Z"

        val timestampInstant = Instant.parse(internationalDateTime)
        val articlePublishedZonedTime =
            ZonedDateTime.ofInstant(timestampInstant, ZoneId.systemDefault())
        val dateFormatter: DateTimeFormatter =
            DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)

        // Get current Instant
        val currentZonedTime = ZonedDateTime.ofInstant(Instant.now(), ZoneId.systemDefault())
        //Convert current Instant to local time zone
        val gapInDays = Duration.between(currentZonedTime, articlePublishedZonedTime).toDays()


        //Find difference in current and published date of article
        val finalDate = when (gapInDays) {
            0L -> "Today"
            1L -> "Yesterday"
            else -> articlePublishedZonedTime.format(dateFormatter)
        }
        binding.tvLocalFormat.text = "$internationalDateTime\n = $finalDate"
    }

    override fun onStart() {
        super.onStart()
        initializePlayer()
    }

    override fun onResume() {
        super.onResume()
        activity?.hideSystemUi(binding.videoView)
        if (player == null) {
            initializePlayer()
        }
    }

    override fun onStop() {
        super.onStop()
        releasePlayer()
    }


    private fun initializePlayer() {
        // ExoPlayer implements the Player interface
        player = ExoPlayer.Builder(requireContext())
            .build()
            .also { exoPlayer ->
                binding.videoView.player = exoPlayer
                // Update the track selection parameters to only pick standard definition tracks
                exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                    .buildUpon()
                    .setMaxVideoSizeSd()
                    .build()

                val mediaItem = MediaItem.Builder()
                    .setUri(getString(R.string.media_url_dash))
                    .setMimeType(MimeTypes.APPLICATION_MPD)
                    .build()
                exoPlayer.setMediaItems(listOf(mediaItem), mediaItemIndex, playbackPosition)
                exoPlayer.playWhenReady = playWhenReady
                exoPlayer.addListener(playbackStateListener)
                exoPlayer.prepare()
            }
    }

    private fun releasePlayer() {
        player?.let { player ->
            playbackPosition = player.currentPosition
            mediaItemIndex = player.currentMediaItemIndex
            playWhenReady = player.playWhenReady
            player.removeListener(playbackStateListener)
            player.release()
        }
        player = null
    }

}

private fun playbackStateListener() = object : Player.Listener {
    override fun onPlaybackStateChanged(playbackState: Int) {
        val stateString: String = when (playbackState) {
            ExoPlayer.STATE_IDLE -> "ExoPlayer.STATE_IDLE      -"
            ExoPlayer.STATE_BUFFERING -> "ExoPlayer.STATE_BUFFERING -"
            ExoPlayer.STATE_READY -> "ExoPlayer.STATE_READY     -"
            ExoPlayer.STATE_ENDED -> "ExoPlayer.STATE_ENDED     -"
            else -> "UNKNOWN_STATE             -"
        }
        Timber.d("changed state to $stateString")
    }
}


