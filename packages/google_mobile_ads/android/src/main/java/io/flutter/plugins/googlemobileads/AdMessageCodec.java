// Copyright 2021 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.c language governing permissions and
//   limitations under the License.

package io.flutter.plugins.googlemobileads;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import io.flutter.plugin.common.StandardMessageCodec;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;

/**
 * Encodes and decodes values by reading from a ByteBuffer and writing to a ByteArrayOutputStream.
 */
class AdMessageCodec extends StandardMessageCodec {
  // The type values below must be consistent for each platform.
  static final byte VALUE_AD_SIZE = (byte) 128;
  static final byte VALUE_AD_REQUEST = (byte) 129;
  static final byte VALUE_REWARD_ITEM = (byte) 132;
  static final byte VALUE_LOAD_AD_ERROR = (byte) 133;
  static final byte VALUE_PUBLISHER_AD_REQUEST = (byte) 134;
  static final byte VALUE_INITIALIZATION_STATE = (byte) 135;
  static final byte VALUE_ADAPTER_STATUS = (byte) 136;
  static final byte VALUE_INITIALIZATION_STATUS = (byte) 137;
  static final byte VALUE_SERVER_SIDE_VERIFICATION_OPTIONS = (byte) 138;
  static final byte VALUE_ANCHORED_ADAPTIVE_BANNER_AD_SIZE = (byte) 139;
  static final byte VALUE_SMART_BANNER_AD_SIZE = (byte) 140;

  @NonNull final Context context;
  @NonNull final FlutterAdSize.AdSizeFactory adSizeFactory;

  AdMessageCodec(@NonNull Context context) {
    this.context = context;
    this.adSizeFactory = new FlutterAdSize.AdSizeFactory();
  }

  @VisibleForTesting
  AdMessageCodec(@NonNull Context context, @NonNull FlutterAdSize.AdSizeFactory adSizeFactory) {
    this.context = context;
    this.adSizeFactory = adSizeFactory;
  }

  @Override
  protected void writeValue(ByteArrayOutputStream stream, Object value) {
    if (value instanceof FlutterAdSize) {
      writeAdSize(stream, (FlutterAdSize) value);
    } else if (value instanceof FlutterAdRequest) {
      stream.write(VALUE_AD_REQUEST);
      final FlutterAdRequest request = (FlutterAdRequest) value;
      writeValue(stream, request.getKeywords());
      writeValue(stream, request.getContentUrl());
      writeValue(stream, request.getTestDevices());
      writeValue(stream, request.getNonPersonalizedAds());
    } else if (value instanceof FlutterRewardedAd.FlutterRewardItem) {
      stream.write(VALUE_REWARD_ITEM);
      final FlutterRewardedAd.FlutterRewardItem item = (FlutterRewardedAd.FlutterRewardItem) value;
      writeValue(stream, item.amount);
      writeValue(stream, item.type);
    } else if (value instanceof FlutterAd.FlutterLoadAdError) {
      stream.write(VALUE_LOAD_AD_ERROR);
      final FlutterAd.FlutterLoadAdError error = (FlutterAd.FlutterLoadAdError) value;
      writeValue(stream, error.code);
      writeValue(stream, error.domain);
      writeValue(stream, error.message);
    } else if (value instanceof FlutterPublisherAdRequest) {
      stream.write(VALUE_PUBLISHER_AD_REQUEST);
      final FlutterPublisherAdRequest request = (FlutterPublisherAdRequest) value;
      writeValue(stream, request.getKeywords());
      writeValue(stream, request.getContentUrl());
      writeValue(stream, request.getCustomTargeting());
      writeValue(stream, request.getCustomTargetingLists());
      writeValue(stream, request.getNonPersonalizedAds());
    } else if (value instanceof FlutterAdapterStatus.AdapterInitializationState) {
      stream.write(VALUE_INITIALIZATION_STATE);
      final FlutterAdapterStatus.AdapterInitializationState state =
          (FlutterAdapterStatus.AdapterInitializationState) value;
      switch (state) {
        case NOT_READY:
          writeValue(stream, "notReady");
          return;
        case READY:
          writeValue(stream, "ready");
          return;
      }
      final String message = String.format("Unable to handle state: %s", state);
      throw new IllegalArgumentException(message);
    } else if (value instanceof FlutterAdapterStatus) {
      stream.write(VALUE_ADAPTER_STATUS);
      final FlutterAdapterStatus status = (FlutterAdapterStatus) value;
      writeValue(stream, status.state);
      writeValue(stream, status.description);
      writeValue(stream, status.latency);
    } else if (value instanceof FlutterInitializationStatus) {
      stream.write(VALUE_INITIALIZATION_STATUS);
      final FlutterInitializationStatus status = (FlutterInitializationStatus) value;
      writeValue(stream, status.adapterStatuses);
    } else if (value instanceof FlutterServerSideVerificationOptions) {
      stream.write(VALUE_SERVER_SIDE_VERIFICATION_OPTIONS);
      FlutterServerSideVerificationOptions options = (FlutterServerSideVerificationOptions) value;
      writeValue(stream, options.getUserId());
      writeValue(stream, options.getCustomData());
    } else {
      super.writeValue(stream, value);
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  protected Object readValueOfType(byte type, ByteBuffer buffer) {
    switch (type) {
      case VALUE_ANCHORED_ADAPTIVE_BANNER_AD_SIZE:
        final String orientation = (String) readValueOfType(buffer.get(), buffer);
        final Integer width = (Integer) readValueOfType(buffer.get(), buffer);
        return new FlutterAdSize.AnchoredAdaptiveBannerAdSize(
            context, adSizeFactory, orientation, width);
      case VALUE_SMART_BANNER_AD_SIZE:
        return new FlutterAdSize.SmartBannerAdSize();
      case VALUE_AD_SIZE:
        return new FlutterAdSize(
            (Integer) readValueOfType(buffer.get(), buffer),
            (Integer) readValueOfType(buffer.get(), buffer));
      case VALUE_AD_REQUEST:
        return new FlutterAdRequest.Builder()
            .setKeywords((List<String>) readValueOfType(buffer.get(), buffer))
            .setContentUrl((String) readValueOfType(buffer.get(), buffer))
            .setTestDevices((List<String>) readValueOfType(buffer.get(), buffer))
            .setNonPersonalizedAds(booleanValueOf(readValueOfType(buffer.get(), buffer)))
            .build();
      case VALUE_REWARD_ITEM:
        return new FlutterRewardedAd.FlutterRewardItem(
            (Integer) readValueOfType(buffer.get(), buffer),
            (String) readValueOfType(buffer.get(), buffer));
      case VALUE_LOAD_AD_ERROR:
        return new FlutterAd.FlutterLoadAdError(
            (Integer) readValueOfType(buffer.get(), buffer),
            (String) readValueOfType(buffer.get(), buffer),
            (String) readValueOfType(buffer.get(), buffer));
      case VALUE_PUBLISHER_AD_REQUEST:
        return new FlutterPublisherAdRequest.Builder()
            .setKeywords((List<String>) readValueOfType(buffer.get(), buffer))
            .setContentUrl((String) readValueOfType(buffer.get(), buffer))
            .setCustomTargeting((Map<String, String>) readValueOfType(buffer.get(), buffer))
            .setCustomTargetingLists(
                (Map<String, List<String>>) readValueOfType(buffer.get(), buffer))
            .setNonPersonalizedAds((Boolean) readValueOfType(buffer.get(), buffer))
            .build();
      case VALUE_INITIALIZATION_STATE:
        final String state = (String) readValueOfType(buffer.get(), buffer);
        switch (state) {
          case "notReady":
            return FlutterAdapterStatus.AdapterInitializationState.NOT_READY;
          case "ready":
            return FlutterAdapterStatus.AdapterInitializationState.READY;
          default:
            final String message = String.format("Unable to handle state: %s", state);
            throw new IllegalArgumentException(message);
        }
      case VALUE_ADAPTER_STATUS:
        return new FlutterAdapterStatus(
            (FlutterAdapterStatus.AdapterInitializationState) readValueOfType(buffer.get(), buffer),
            (String) readValueOfType(buffer.get(), buffer),
            (Number) readValueOfType(buffer.get(), buffer));
      case VALUE_INITIALIZATION_STATUS:
        return new FlutterInitializationStatus(
            (Map<String, FlutterAdapterStatus>) readValueOfType(buffer.get(), buffer));
      case VALUE_SERVER_SIDE_VERIFICATION_OPTIONS:
        return new FlutterServerSideVerificationOptions(
            (String) readValueOfType(buffer.get(), buffer),
            (String) readValueOfType(buffer.get(), buffer));
      default:
        return super.readValueOfType(type, buffer);
    }
  }

  protected void writeAdSize(ByteArrayOutputStream stream, FlutterAdSize value) {
    if (value instanceof FlutterAdSize.AnchoredAdaptiveBannerAdSize) {
      stream.write(VALUE_ANCHORED_ADAPTIVE_BANNER_AD_SIZE);
      final FlutterAdSize.AnchoredAdaptiveBannerAdSize size =
          (FlutterAdSize.AnchoredAdaptiveBannerAdSize) value;
      writeValue(stream, size.orientation);
      writeValue(stream, size.width);
      writeValue(stream, size.height);
    } else if (value instanceof FlutterAdSize.SmartBannerAdSize) {
      stream.write(VALUE_SMART_BANNER_AD_SIZE);
    } else if (value instanceof FlutterAdSize) {
      stream.write(VALUE_AD_SIZE);
      writeValue(stream, value.width);
      writeValue(stream, value.height);
    }
  }

  @Nullable
  private static Boolean booleanValueOf(@Nullable Object object) {
    if (object == null) {
      return null;
    }
    return (Boolean) object;
  }
}
