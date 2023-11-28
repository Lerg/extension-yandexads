package com.defold.extension;

import androidx.annotation.Nullable;
import androidx.annotation.NonNull;
import android.util.Log;
import android.app.Activity;
import android.view.Display;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewGroup.MarginLayoutParams;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.location.Location;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.util.DisplayMetrics;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

import com.yandex.mobile.ads.banner.AdSize;
import com.yandex.mobile.ads.banner.BannerAdEventListener;
import com.yandex.mobile.ads.banner.BannerAdView;
import com.yandex.mobile.ads.common.AdRequest;
import com.yandex.mobile.ads.common.AdRequestError;
import com.yandex.mobile.ads.common.ImpressionData;
import com.yandex.mobile.ads.common.InitializationListener;
import com.yandex.mobile.ads.common.MobileAds;
import com.yandex.mobile.ads.interstitial.InterstitialAd;
import com.yandex.mobile.ads.interstitial.InterstitialAdEventListener;
import com.yandex.mobile.ads.rewarded.Reward;
import com.yandex.mobile.ads.rewarded.RewardedAd;
import com.yandex.mobile.ads.rewarded.RewardedAdEventListener;

public class ExtensionYandexAds {

	private static final String TAG = "ExtensionYandexAdsEasy";

	public static native void AddToQueue(int msg, String json);

	private static final int MSG_ADS_INITED = 0;
	private static final int MSG_INTERSTITIAL = 1;
	private static final int MSG_REWARDED = 2;
	private static final int MSG_BANNER = 3;

	private static final int EVENT_LOADED = 0;
	private static final int EVENT_ERROR_LOAD = 1;
	private static final int EVENT_SHOWN = 2;
	private static final int EVENT_DISMISSED = 3;
	private static final int EVENT_CLICKED = 4;
	private static final int EVENT_IMPRESSION = 5;
	private static final int EVENT_NOT_LOADED = 6;
	private static final int EVENT_REWARDED = 7;
	private static final int EVENT_DESTROYED = 8;

	private static final int POS_NONE =                 0;
	private static final int POS_TOP_LEFT =             1;
	private static final int POS_TOP_CENTER =           2;
	private static final int POS_TOP_RIGHT =            3;
	private static final int POS_BOTTOM_LEFT =          4;
	private static final int POS_BOTTOM_CENTER =        5;
	private static final int POS_BOTTOM_RIGHT =         6;
	private static final int POS_CENTER =               7;

	private static final int BANNER_320_50 = 0;

	private Activity activity;
	private InterstitialAd mInterstitialAd;
	private RewardedAd mRewardedAd;

	public ExtensionYandexAds(Activity mainActivity) {
		activity = mainActivity;
	}

	public void initialize() {
		activity.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				MobileAds.initialize(activity, new InitializationListener() {
					@Override
					public void onInitializationCompleted() {
						Log.d(TAG, "onInitializationCompleted");
						sendSimpleMessage(MSG_ADS_INITED, EVENT_LOADED);
					}
				});
				//MobileAds.enableLogging(true);
			}
		});
	}


	public void setUserConsent(boolean enable_rdp) {
		Log.d(TAG, "setUserConsent:"+enable_rdp);
		MobileAds.setUserConsent(enable_rdp);
	}

// ------------------------------------------------------------------------------------------
	private String lastInterstitialId = "";
	public void loadInterstitial(final String unitId) {
		activity.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				Log.d(TAG, "loadInterstitial");
				// если разные ид объявлений
				if (!lastInterstitialId.equals(unitId)){
					lastInterstitialId = unitId;
					if (mInterstitialAd != null)
						mInterstitialAd.destroy();
					mInterstitialAd = null;
					mInterstitialAd = new InterstitialAd(activity);
					mInterstitialAd.setAdUnitId(unitId);

					mInterstitialAd.setInterstitialAdEventListener(new InterstitialAdEventListener() {
						@Override
						public void onAdLoaded() {
							Log.d(TAG, "interstitial:onAdLoaded");
							sendSimpleMessage(MSG_INTERSTITIAL, EVENT_LOADED);
						}

						@Override
						public void onAdFailedToLoad(AdRequestError adRequestError) {
							Log.e(TAG, "interstitial:onAdFailedToLoad" + adRequestError.toString());
							sendSimpleMessage(MSG_INTERSTITIAL, EVENT_ERROR_LOAD, "error", adRequestError.toString());
						}

						@Override
						public void onAdShown() {
							Log.d(TAG, "interstitial:onAdShown");
							sendSimpleMessage(MSG_INTERSTITIAL, EVENT_SHOWN);
						}

						@Override
						public void onAdDismissed() {
							Log.d(TAG, "interstitial:onAdDismissed");
							sendSimpleMessage(MSG_INTERSTITIAL, EVENT_DISMISSED);
						}

						@Override
						public void onAdClicked() {
							Log.d(TAG, "interstitial:onAdClicked");
							sendSimpleMessage(MSG_INTERSTITIAL, EVENT_CLICKED);
						}

						@Override
						public void onImpression(@Nullable ImpressionData impressionData) {
							Log.d(TAG, "interstitial:onImpression");
							sendSimpleMessage(MSG_INTERSTITIAL, EVENT_IMPRESSION);
						}

						@Override
						public void onLeftApplication() {}

						@Override
						public void onReturnedToApplication() {}
					});
				}

				AdRequest adRequest = new AdRequest.Builder().build();
				mInterstitialAd.loadAd(adRequest);
			}
		});
	}

	public boolean isInterstitialLoaded() {
		return mInterstitialAd != null && mInterstitialAd.isLoaded();
	}

	public void showInterstitial() {
		activity.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				if (isInterstitialLoaded()) {
					Log.d(TAG, "showInterstitial");
					mInterstitialAd.show();
				} else {
					Log.e(TAG, "The interstitial ad wasn't ready yet.");
					sendSimpleMessage(MSG_INTERSTITIAL, EVENT_NOT_LOADED, "error",
						"Can't show Interstitial AD that wasn't loaded.");
				}
			}
		});
	}

// ------------------------------------------------------------------------------------------
	private String lastRewardedId = "";
	public void loadRewarded(final String unitId) {
		activity.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				Log.d(TAG, "loadRewarded");
				// если разные ид объявлений
				if (!lastRewardedId.equals(unitId)){
					lastRewardedId = unitId;
					if (mRewardedAd != null)
						mRewardedAd.destroy();
					mRewardedAd = null;
					mRewardedAd = new RewardedAd(activity);
					mRewardedAd.setAdUnitId(unitId);

					mRewardedAd.setRewardedAdEventListener(new RewardedAdEventListener() {
						@Override
						public void onRewarded(final Reward reward) {
							Log.d(TAG, "rewarded:onRewarded");
							sendSimpleMessage(MSG_REWARDED, EVENT_REWARDED);
						}

						@Override
						public void onAdClicked() {
							Log.d(TAG, "rewarded:onAdClicked");
							sendSimpleMessage(MSG_REWARDED, EVENT_CLICKED);
						}

						@Override
						public void onAdLoaded() {
							Log.d(TAG, "rewarded:onAdLoaded");
							sendSimpleMessage(MSG_REWARDED, EVENT_LOADED);
						}

						@Override
						public void onAdFailedToLoad(final AdRequestError adRequestError) {
							Log.e(TAG, "rewarded:onAdFailedToLoad" + adRequestError.toString());
							sendSimpleMessage(MSG_REWARDED, EVENT_ERROR_LOAD, "error", adRequestError.toString());
						}

						@Override
						public void onAdShown() {
							Log.d(TAG, "rewarded:onAdShown");
							sendSimpleMessage(MSG_REWARDED, EVENT_SHOWN);
						}

						@Override
						public void onAdDismissed() {
							Log.d(TAG, "rewarded:onAdDismissed");
							sendSimpleMessage(MSG_REWARDED, EVENT_DISMISSED);
						}

						@Override
						public void onImpression(@Nullable ImpressionData impressionData) {
							Log.d(TAG, "rewarded:onImpression");
							sendSimpleMessage(MSG_REWARDED, EVENT_IMPRESSION);
						}

						@Override
						public void onLeftApplication() {}

						@Override
						public void onReturnedToApplication() {}
					});
				}

				AdRequest adRequest = new AdRequest.Builder().build();
				mRewardedAd.loadAd(adRequest);
			}
		});
	}

	public boolean isRewardedLoaded() {
		return mRewardedAd != null && mRewardedAd.isLoaded();
	}

	public void showRewarded() {
		activity.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				if (isRewardedLoaded()) {
					Log.d(TAG, "showRewarded");
					mRewardedAd.show();
				} else {
					Log.e(TAG, "The rewarded ad wasn't ready yet.");
					sendSimpleMessage(MSG_REWARDED, EVENT_NOT_LOADED, "error",
						"Can't show rewarded AD that wasn't loaded.");
				}
			}
		});
	}

// ------------------------------------------------------------------------------------------
	private LinearLayout layout;
	private BannerAdView mBannerAdView;
	private WindowManager windowManager;
	private boolean isBannerShown = false;
	private int m_bannerPosition = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;

	public void loadBanner(final String unitId, int bannerSize) {
		int w = 320;
		int h = 50;
		// todo add other...
		if (bannerSize == BANNER_320_50) {}

		activity.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				Log.d(TAG, "loadBanner");
				if (isBannerLoaded())
					_destroyBanner();

				final BannerAdView view = new BannerAdView(activity);
				view.setAdUnitId(unitId);
				view.setAdSize(AdSize.flexibleSize(w, h));
				view.setVisibility(View.INVISIBLE);
				mBannerAdView = view;
				createLayout();

				AdRequest adRequest = new AdRequest.Builder().build();
				view.setBannerAdEventListener(new BannerAdEventListener() {
					@Override
					public void onAdLoaded() {
						Log.d(TAG, "banner:onAdLoaded");
						sendSimpleMessage(MSG_BANNER, EVENT_LOADED);
					}

					@Override
					public void onAdFailedToLoad(AdRequestError adRequestError) {
						Log.e(TAG, "banner:onAdFailedToLoad" + adRequestError.toString());
						sendSimpleMessage(MSG_BANNER, EVENT_ERROR_LOAD, "error", adRequestError.toString());
					}

					@Override
					public void onAdClicked() {
						Log.d(TAG, "banner:onAdClicked");
						sendSimpleMessage(MSG_BANNER, EVENT_CLICKED);
					}

					@Override
					public void onImpression(@Nullable ImpressionData impressionData) {
						Log.d(TAG, "banner:onImpression");
						sendSimpleMessage(MSG_BANNER, EVENT_IMPRESSION);
					}

					@Override
					public void onLeftApplication() {}

					@Override
					public void onReturnedToApplication() {}
				});
				// Загрузка объявления.
				view.loadAd(adRequest);
			}
		});
	}

	public boolean isBannerLoaded() {
		return mBannerAdView != null;
	}

	private void _destroyBanner() {
		Log.d(TAG, "destroyBanner");
		if (!isBannerLoaded())
			return;

		if (isBannerShown && windowManager != null && layout != null) {
			try {
				windowManager.removeView(layout);
			} catch (Exception e) {
				Log.e(TAG, "_destroyBanner: " + e);
			}
		}
		mBannerAdView.destroy();
		mBannerAdView = null;
		layout = null;
		isBannerShown = false;
		sendSimpleMessage(MSG_BANNER, EVENT_DESTROYED);
	}

	public void destroyBanner() {
		activity.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				_destroyBanner();
			}
		});
	}

	private int getGravity(int bannerPosConst) {
		int bannerPos = Gravity.NO_GRAVITY;
		switch (bannerPosConst) {
		case POS_TOP_LEFT:
			bannerPos = Gravity.TOP | Gravity.LEFT;
			break;
		case POS_TOP_CENTER:
			bannerPos = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
			break;
		case POS_TOP_RIGHT:
			bannerPos = Gravity.TOP | Gravity.RIGHT;
			break;
		case POS_BOTTOM_LEFT:
			bannerPos = Gravity.BOTTOM | Gravity.LEFT;
			break;
		case POS_BOTTOM_CENTER:
			bannerPos = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
			break;
		case POS_BOTTOM_RIGHT:
			bannerPos = Gravity.BOTTOM | Gravity.RIGHT;
			break;
		case POS_CENTER:
			bannerPos = Gravity.CENTER;
			break;
		}
		return bannerPos;
	}

	public void showBanner(final int pos) {
		activity.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				Log.d(TAG, "showBanner");
				if (!isBannerLoaded()) {
					return;
				}
				if (layout == null)
					return;
				layout.setSystemUiVisibility(activity.getWindow().getDecorView().getSystemUiVisibility());
				int gravity = getGravity(pos);
				if ( m_bannerPosition != gravity && isBannerShown) {
					try {
						m_bannerPosition = gravity;
						windowManager.updateViewLayout(layout, getParameters());
					} catch (Exception e) {
						Log.e(TAG, "showBanner: " + e);
					}
					return;
				}
				if (!layout.isShown()) {
					m_bannerPosition = gravity;
					windowManager.addView(layout, getParameters());
					mBannerAdView.setVisibility(View.VISIBLE);
					isBannerShown = true;
				}
			}
		});
	}

	public void hideBanner() {
		activity.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				Log.d(TAG, "hideBanner");
				if (!isBannerLoaded() || !isBannerShown) {
					return;
				}
				isBannerShown = false;
				if (windowManager != null && layout != null) {
					try {
						windowManager.removeView(layout);
					} catch (Exception e) {
						Log.e(TAG, "hideBanner: " + e);
					}
				}
				mBannerAdView.setVisibility(View.INVISIBLE);
			}
		});
	}

	public void updateBannerLayout() {
		activity.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				if (!isBannerLoaded()) {
					return;
				}
				if (windowManager != null && layout != null) {
					layout.setSystemUiVisibility(activity.getWindow().getDecorView().getSystemUiVisibility());
					if (!isBannerShown) {
						return;
					}

					try {
						windowManager.removeView(layout);
					} catch (Exception e) {
						Log.e(TAG, "updateBannerLayout(remove): " + e);
					}

					if (isBannerShown) {
						try {
							windowManager.updateViewLayout(layout, getParameters());
						}
						 catch (Exception e) {
							Log.e(TAG, "updateBannerLayout(update): " + e);
						}
						if (!layout.isShown()) {
							windowManager.addView(layout, getParameters());
						}
					}
				}
			}
		});
	}

	private void createLayout() {
		if (layout == null){
			windowManager = activity.getWindowManager();
			layout = new LinearLayout(activity);
			layout.setOrientation(LinearLayout.VERTICAL);
		}
		MarginLayoutParams params = new MarginLayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
		params.setMargins(0, 0, 0, 0);
		layout.setSystemUiVisibility(activity.getWindow().getDecorView().getSystemUiVisibility());
		layout.addView(mBannerAdView, params);
	}

	private WindowManager.LayoutParams getParameters() {
		WindowManager.LayoutParams windowParams = new WindowManager.LayoutParams();
		windowParams.x = WindowManager.LayoutParams.WRAP_CONTENT;
		windowParams.y = WindowManager.LayoutParams.WRAP_CONTENT;
		windowParams.width = dpToPx(320);//WindowManager.LayoutParams.WRAP_CONTENT;
		windowParams.height = dpToPx(50); //WindowManager.LayoutParams.WRAP_CONTENT;
		windowParams.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
		WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
		windowParams.gravity = m_bannerPosition;
		return windowParams;
	}

	 public int dpToPx(int dp) {
        return (int) (dp * ((float) activity.getResources().getDisplayMetrics().densityDpi / DisplayMetrics.DENSITY_DEFAULT));

    }

// ------------------------------------------------------------------------------------------

	private String getJsonConversionErrorMessage(String messageText) {
		String message = null;
		try {
			JSONObject obj = new JSONObject();
			obj.put("error", messageText);
			message = obj.toString();
		} catch (JSONException e) {
			message = "{ \"error\": \"Error while converting simple message to JSON.\" }";
		}
		return message;
	}

	private void sendSimpleMessage(int msg, int eventId) {
		String message = null;
		try {
			JSONObject obj = new JSONObject();
			obj.put("event", eventId);
			message = obj.toString();
		} catch (JSONException e) {
			message = getJsonConversionErrorMessage(e.getLocalizedMessage());
		}
		AddToQueue(msg, message);
	}

	private void sendSimpleMessage(int msg, int eventId, String key_2, String value_2) {
		String message = null;
		try {
			JSONObject obj = new JSONObject();
			obj.put("event", eventId);
			obj.put(key_2, value_2);
			message = obj.toString();
		} catch (JSONException e) {
			message = getJsonConversionErrorMessage(e.getLocalizedMessage());
		}
		AddToQueue(msg, message);
	}

}