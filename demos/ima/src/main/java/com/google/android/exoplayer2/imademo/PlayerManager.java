/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.android.exoplayer2.imademo;

import static com.google.ads.interactivemedia.v3.internal.adr.requestAds;
import static com.google.ads.interactivemedia.v3.internal.adr.requestNextAdBreak;
import static com.google.android.exoplayer2.mediacodec.MediaCodecInfo.TAG;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import com.google.ads.interactivemedia.v3.api.AdDisplayContainer;
import com.google.ads.interactivemedia.v3.api.AdErrorEvent;
import com.google.ads.interactivemedia.v3.api.AdEvent;
import com.google.ads.interactivemedia.v3.api.AdsLoader;
import com.google.ads.interactivemedia.v3.api.AdsManager;
import com.google.ads.interactivemedia.v3.api.AdsManagerLoadedEvent;
import com.google.ads.interactivemedia.v3.api.AdsRequest;
import com.google.ads.interactivemedia.v3.api.ImaSdkFactory;
import com.google.ads.interactivemedia.v3.api.ImaSdkSettings;
import com.google.ads.interactivemedia.v3.api.player.ContentProgressProvider;
import com.google.ads.interactivemedia.v3.api.player.VideoProgressUpdate;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.C.ContentType;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.ext.ima.ImaAdsLoader;
import com.google.android.exoplayer2.metadata.Metadata;
import com.google.android.exoplayer2.metadata.MetadataOutput;
import com.google.android.exoplayer2.metadata.id3.GeobFrame;
import com.google.android.exoplayer2.metadata.id3.Id3Frame;
import com.google.android.exoplayer2.metadata.id3.TextInformationFrame;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.source.ads.AdsMediaSource;
import com.google.android.exoplayer2.source.dash.DashMediaSource;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.source.smoothstreaming.SsMediaSource;
import com.google.android.exoplayer2.text.TextOutput;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;

/**
 * Manages the {@link ExoPlayer}, the IMA plugin and all video playback.
 */
/* package */ final class PlayerManager implements AdsMediaSource.MediaSourceFactory, AdEvent.AdEventListener,
    AdErrorEvent.AdErrorListener, MetadataOutput,
    Player.EventListener {

  //private final ImaAdsLoader adsLoader;
  private final DataSource.Factory dataSourceFactory;

  // private SimpleExoPlayer player;
  private long contentPosition;

  // The video player.
  private SimpleExoPlayer mVideoPlayer;

  // Factory class for creating SDK objects.
  private ImaSdkFactory mSdkFactory;

  // The AdsLoader instance exposes the requestAds method.
  private AdsLoader mAdsLoader;

  // AdsManager exposes methods to control ad playback and listen to ad events.
  private AdsManager mAdsManager;

  // Whether an ad is displayed.
  private boolean mIsAdDisplayed;

  MediaSource contentMediaSource;

  public PlayerManager(Context context) {
    dataSourceFactory =
        new DefaultDataSourceFactory(
            context, Util.getUserAgent(context, context.getString(R.string.application_name)));
  }

  public void init(Context context, PlayerView playerView, ViewGroup mAdUiContainer,
      View mPlayButton) {

    // Create an AdsLoader.
    mSdkFactory = ImaSdkFactory.getInstance();
    AdDisplayContainer adDisplayContainer = mSdkFactory.createAdDisplayContainer();
    adDisplayContainer.setAdContainer(mAdUiContainer);
    ImaSdkSettings settings = mSdkFactory.createImaSdkSettings();
    settings.setAutoPlayAdBreaks(false);
    mAdsLoader = mSdkFactory.createAdsLoader(
        context, settings, adDisplayContainer);

    String adTag = context.getString(R.string.ad_tag_url);
    requestAds(adTag);

    // Add listeners for when ads are loaded and for errors.
    mAdsLoader.addAdErrorListener(this);
    mAdsLoader.addAdsLoadedListener(new AdsLoader.AdsLoadedListener() {
      @Override
      public void onAdsManagerLoaded(AdsManagerLoadedEvent adsManagerLoadedEvent) {
        // Ads were successfully loaded, so get the AdsManager instance. AdsManager has
        // events for ad playback and errors.
        mAdsManager = adsManagerLoadedEvent.getAdsManager();

        // Attach event and error event listeners.
        mAdsManager.addAdErrorListener(PlayerManager.this);
        mAdsManager.addAdEventListener(PlayerManager.this);
        mAdsManager.init();
      }
    });

    // Create a player instance.
    mVideoPlayer = ExoPlayerFactory.newSimpleInstance(context);
    //mAdsLoader.setPlayer(mVideoPlayer);
    playerView.setPlayer(mVideoPlayer);

    // This is the MediaSource representing the content media (i.e. not the ad).
    String contentUrl = context.getString(R.string.content_url);
    contentMediaSource = buildMediaSource(Uri.parse(contentUrl));

 /*   // Compose the content media source into a new AdsMediaSource with both ads and content.
    MediaSource mediaSourceWithAds =
        new AdsMediaSource(
            contentMediaSource, *//* adMediaSourceFactory= *//* this, adsLoader, playerView);*/
    mVideoPlayer.addMetadataOutput(this);
    mVideoPlayer.addListener(this);
    // Prepare the player with the source.
    mVideoPlayer.seekTo(contentPosition);
    mVideoPlayer.prepare(contentMediaSource);
    mVideoPlayer.setPlayWhenReady(true);


  }


  /**
   * Request video ads from the given VAST ad tag.
   *
   * @param adTagUrl URL of the ad's VAST XML
   */
  private void requestAds(String adTagUrl) {
    // Create the ads request.
    AdsRequest request = mSdkFactory.createAdsRequest();
    request.setAdTagUrl(adTagUrl);
    request.setContentProgressProvider(new ContentProgressProvider() {
      @Override
      public VideoProgressUpdate getContentProgress() {
        if (mIsAdDisplayed || mVideoPlayer == null || mVideoPlayer.getDuration() <= 0) {
          return VideoProgressUpdate.VIDEO_TIME_NOT_READY;
        }
        return new VideoProgressUpdate(mVideoPlayer.getCurrentPosition(),
            mVideoPlayer.getDuration());
      }
    });

    // Request the ad. After the ad is loaded, onAdsManagerLoaded() will be called.
    mAdsLoader.requestAds(request);
  }

  public void reset() {
    if (mVideoPlayer != null) {
      contentPosition = mVideoPlayer.getContentPosition();
      mVideoPlayer.release();
      mVideoPlayer = null;
      //mAdsLoader.setPlayer(null);
    }
  }

  public void release() {
    if (mVideoPlayer != null) {
      mVideoPlayer.release();
      mVideoPlayer = null;
    }
    //mAdsLoader.release();
  }

  // MediaSourceFactory implementation.

  @Override
  public MediaSource createMediaSource(Uri uri) {
    return buildMediaSource(uri);
  }

  @Override
  public int[] getSupportedTypes() {
    // IMA does not support Smooth Streaming ads.
    return new int[]{C.TYPE_DASH, C.TYPE_HLS, C.TYPE_OTHER};
  }

  // Internal methods.

  private MediaSource buildMediaSource(Uri uri) {
    @ContentType int type = Util.inferContentType(uri);
    switch (type) {
      case C.TYPE_DASH:
        return new DashMediaSource.Factory(dataSourceFactory).createMediaSource(uri);
      case C.TYPE_SS:
        return new SsMediaSource.Factory(dataSourceFactory).createMediaSource(uri);
      case C.TYPE_HLS:
        return new HlsMediaSource.Factory(dataSourceFactory).createMediaSource(uri);
      case C.TYPE_OTHER:
        return new ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(uri);
      default:
        throw new IllegalStateException("Unsupported type: " + type);
    }
  }


  @Override
  public void onAdEvent(AdEvent adEvent) {
    Log.i(TAG, "Event: " + adEvent.getType());

    // These are the suggested event types to handle. For full list of all ad event
    // types, see the documentation for AdEvent.AdEventType.
    switch (adEvent.getType()) {
      case LOADED:
        // AdEventType.LOADED will be fired when ads are ready to be played.
        // AdsManager.start() begins ad playback. This method is ignored for VMAP or
        // ad rules playlists, as the SDK will automatically start executing the
        // playlist.
        mAdsManager.start();
        break;
      case CONTENT_PAUSE_REQUESTED:
        // AdEventType.CONTENT_PAUSE_REQUESTED is fired immediately before a video
        // ad is played.
        mIsAdDisplayed = true;
        mVideoPlayer.stop();
        break;
      case CONTENT_RESUME_REQUESTED:
        // AdEventType.CONTENT_RESUME_REQUESTED is fired when the ad is completed
        // and you should start playing your content.
        mIsAdDisplayed = false;
        mVideoPlayer.setPlayWhenReady(true);
        break;
      case ALL_ADS_COMPLETED:
        if (mAdsManager != null) {
          mAdsManager.destroy();
          mAdsManager = null;
        }
        break;
      default:
        break;
    }
  }

  @Override
  public void onAdError(AdErrorEvent adErrorEvent) {
    Log.e(TAG, "Ad Error: " + adErrorEvent.getError().getMessage());
    mVideoPlayer.setPlayWhenReady(true);
  }

  @Override
  public void onMetadata(Metadata metadata) {
    //Log.d("Sunil","Inside advert!!");
    for (int i = 0; i < metadata.length(); i++) {
      Metadata.Entry entry = metadata.get(i);
      if (entry instanceof GeobFrame) {
        GeobFrame geobFrame = (GeobFrame) entry;
        Log.d(TAG, String.format("%s: mimeType=%s, filename=%s, description=%s",
            geobFrame.id, geobFrame.mimeType, geobFrame.filename, geobFrame.description));
        if (geobFrame.filename.contains("ad") && android.text.TextUtils
            .isDigitsOnly(String.valueOf(geobFrame.filename.charAt(2)))) {
          Log.d("Sunil","Inside advert!!");
          mAdsManager.start();
        }
      }
    }
  }

}
