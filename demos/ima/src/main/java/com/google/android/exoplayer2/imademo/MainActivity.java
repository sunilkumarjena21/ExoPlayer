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

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ui.PlayerView;

/**
 * Main Activity for the IMA plugin demo. {@link ExoPlayer} objects are created by
 * {@link PlayerManager}, which this class instantiates.
 */
public final class MainActivity extends Activity {

  private PlayerView playerView;
  private PlayerManager player;
  // The container for the ad's UI.
  private ViewGroup mAdUiContainer;
  // The play button to trigger the ad request.
  private View mPlayButton;
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main_activity);
    playerView = findViewById(R.id.player_view);
    mAdUiContainer = findViewById(R.id.videoPlayerWithAdPlayback);
    mPlayButton = findViewById(R.id.playButton);
    player = new PlayerManager(this);
  }

  @Override
  public void onResume() {
    super.onResume();
    player.init(this, playerView,mAdUiContainer,mPlayButton);
  }

  @Override
  public void onPause() {
    super.onPause();
    player.reset();
  }

  @Override
  public void onDestroy() {
    player.release();
    super.onDestroy();
  }

}
