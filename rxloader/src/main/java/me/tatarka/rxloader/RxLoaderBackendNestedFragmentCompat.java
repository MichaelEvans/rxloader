package me.tatarka.rxloader;


import android.annotation.TargetApi;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;

import java.lang.ref.WeakReference;

import rx.Observer;

/**
 * Persists the task by running it in a fragment with {@code setRetainInstanceState(true)}. This is
 * used internally by {@link me.tatarka.rxloader.RxLoaderManager}.
 *
 * @author Evan Tatarka
 */
@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class RxLoaderBackendNestedFragmentCompat extends Fragment implements RxLoaderBackend {
    private WeakReference<RxLoaderBackendFragmentHelper> helperRef;
    private boolean hasSavedState;
    private boolean wasDetached;
    private String stateId;

    public void setHelper(RxLoaderBackendFragmentHelper helper) {
        helperRef = new WeakReference<RxLoaderBackendFragmentHelper>(helper);
    }

    private RxLoaderBackendFragmentHelper getHelper() {
        if (helperRef != null) {
            return helperRef.get();
        } else {
            FragmentActivity activity = getActivity();
            if (activity == null) {
                return null;
            }

            RxLoaderBackendFragmentCompat backendFragment = (RxLoaderBackendFragmentCompat) activity
                    .getSupportFragmentManager().findFragmentByTag(RxLoaderManager.FRAGMENT_TAG);
            if (backendFragment == null) {
                backendFragment = new RxLoaderBackendFragmentCompat();
                activity.getSupportFragmentManager().beginTransaction()
                        .add(backendFragment, RxLoaderManager.FRAGMENT_TAG)
                        .commit();
            }

            RxLoaderBackendFragmentHelper helper = backendFragment.getHelper();
            helperRef = new WeakReference<RxLoaderBackendFragmentHelper>(helper);
            return helper;
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        RxLoaderBackendFragmentHelper helper = getHelper();
        if (helper != null) {
            helper.onCreate(getStateId(), savedInstanceState);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (!hasSavedState) {
            RxLoaderBackendFragmentHelper helper = getHelper();
            if (helper != null) {
                helper.onDestroy(getStateId());
            }
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        RxLoaderBackendFragmentHelper helper = getHelper();
        if (helper != null) {
            helper.onDetach(getStateId());
        }
        wasDetached = true;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        hasSavedState = true;
        RxLoaderBackendFragmentHelper helper = getHelper();
        if (helper != null) {
            helper.onSaveInstanceState(outState);
        }
    }

    @Override
    public <T> CachingWeakRefSubscriber<T> get(String tag) {
        RxLoaderBackendFragmentHelper helper = getHelper();
        if (helper != null) {
            return helper.get(getStateId(), tag);
        }
        return null;
    }

    @Override
    public <T> void put(String tag, BaseRxLoader<T> rxLoader, CachingWeakRefSubscriber<T> subscriber) {
        RxLoaderBackendFragmentHelper helper = getHelper();
        if (helper != null) {
            helper.put(getStateId(), tag, wasDetached ? null : rxLoader, subscriber);
        }
    }

    @Override
    public <T> void setSave(String tag, Observer<T> observer, WeakReference<SaveCallback<T>> saveCallbackRef) {
        RxLoaderBackendFragmentHelper helper = getHelper();
        if (helper != null) {
            helper.setSave(getStateId(), tag, observer, saveCallbackRef);
        }
    }

    @Override
    public void unsubscribeAll() {
        RxLoaderBackendFragmentHelper helper = getHelper();
        if (helper != null) {
            helper.unsubscribeAll(getStateId());
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        RxLoaderBackendFragmentHelper helper = getHelper();
        if (helper != null) {
            helper.onDestroyView(getStateId());
        }
    }

    private String getStateId() {
        if (stateId != null) {
            return stateId;
        }

        Fragment parentFragment = getParentFragment();
        stateId = parentFragment.getTag();
        if (stateId == null) {
            int id = parentFragment.getId();
            if (id > 0) {
                stateId = Integer.toString(id);
            }
        }

        if (stateId == null) {
            throw new IllegalStateException("Fragment dose not have a valid id");
        }

        return stateId;
    }
}
