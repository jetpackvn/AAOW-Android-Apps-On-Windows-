package android.app;

import android.animation.Animator;
import android.content.ComponentCallbacks2;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import java.io.FileDescriptor;
import java.io.PrintWriter;

/**
 * Cài đặt android.app.Fragment với đầy đủ vòng đời và kết nối tới Activity/FragmentManager.
 */
public class Fragment implements ComponentCallbacks2, View.OnCreateContextMenuListener {
    public static class SavedState implements Parcelable {
        final Bundle state;

        SavedState(Bundle state) {
            this.state = state;
        }

        SavedState(Parcel in, ClassLoader loader) {
            this.state = in != null ? in.readBundle(loader) : null;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeBundle(state);
        }
    }

    static final int INITIALIZING = 0;
    static final int CREATED = 1;
    static final int ACTIVITY_CREATED = 2;
    static final int STARTED = 3;
    static final int RESUMED = 4;

    int state = INITIALIZING;
    int containerId;
    int fragmentId;
    String tag;
    boolean added;
    boolean removing;
    boolean hidden;
    boolean detached;
    boolean inLayout;
    boolean retainInstance;
    boolean hasMenu;
    boolean menuVisible = true;
    boolean userVisibleHint = true;

    Activity activity;
    FragmentManager fragmentManager;
    FragmentManager childFragmentManager;
    Fragment parentFragment;
    Fragment targetFragment;
    int targetRequestCode;
    Bundle arguments;
    Bundle savedFragmentState;
    View view;
    ViewGroup container;

    public Fragment() {
    }

    public static Fragment instantiate(Context context, String fname) {
        return instantiate(context, fname, null);
    }

    public static Fragment instantiate(Context context, String fname, Bundle args) {
        try {
            ClassLoader cl = context != null && context.getClassLoader() != null
                    ? context.getClassLoader()
                    : Fragment.class.getClassLoader();
            Class<?> cls = Class.forName(fname, true, cl);
            Fragment f = (Fragment) cls.getDeclaredConstructor().newInstance();
            if (args != null) {
                f.setArguments(args);
            }
            return f;
        } catch (Exception e) {
            throw new RuntimeException("Không khởi tạo được Fragment " + fname, e);
        }
    }

    public final Activity getActivity() {
        return activity;
    }

    public Context getContext() {
        return activity;
    }

    public final Object getHost() {
        return activity;
    }

    public final Resources getResources() {
        if (activity == null) {
            throw new IllegalStateException("Fragment " + this + " not attached to Activity");
        }
        return activity.getResources();
    }

    public final CharSequence getText(int resId) {
        return getResources().getText(resId);
    }

    public final String getString(int resId) {
        return getResources().getString(resId);
    }

    public final String getString(int resId, Object... formatArgs) {
        return getResources().getString(resId, formatArgs);
    }

    public final FragmentManager getFragmentManager() {
        return fragmentManager;
    }

    public final FragmentManager getChildFragmentManager() {
        if (childFragmentManager == null) {
            childFragmentManager = new FragmentManager.Impl(activity, this);
        }
        return childFragmentManager;
    }

    public final Fragment getParentFragment() {
        return parentFragment;
    }

    public final int getId() {
        return fragmentId;
    }

    public final String getTag() {
        return tag;
    }

    public void setArguments(Bundle args) {
        this.arguments = args;
    }

    public final Bundle getArguments() {
        return arguments;
    }

    public void setInitialSavedState(SavedState state) {
        this.savedFragmentState = state != null ? state.state : null;
    }

    public void setTargetFragment(Fragment fragment, int requestCode) {
        this.targetFragment = fragment;
        this.targetRequestCode = requestCode;
    }

    public final Fragment getTargetFragment() {
        return targetFragment;
    }

    public final int getTargetRequestCode() {
        return targetRequestCode;
    }

    public final boolean isAdded() {
        return activity != null && added;
    }

    public final boolean isDetached() {
        return detached;
    }

    public final boolean isRemoving() {
        return removing;
    }

    public final boolean isInLayout() {
        return inLayout;
    }

    public final boolean isResumed() {
        return state >= RESUMED;
    }

    public final boolean isVisible() {
        return isAdded() && !isHidden() && view != null && view.getVisibility() == View.VISIBLE;
    }

    public final boolean isHidden() {
        return hidden;
    }

    public final boolean isStateSaved() {
        return fragmentManager != null && fragmentManager.isStateSaved();
    }

    public void setRetainInstance(boolean retain) {
        this.retainInstance = retain;
    }

    public final boolean getRetainInstance() {
        return retainInstance;
    }

    public void setHasOptionsMenu(boolean hasMenu) {
        if (this.hasMenu != hasMenu) {
            this.hasMenu = hasMenu;
            if (isAdded() && !isHidden()) {
                activity.invalidateOptionsMenu();
            }
        }
    }

    public void setMenuVisibility(boolean menuVisible) {
        if (this.menuVisible != menuVisible) {
            this.menuVisible = menuVisible;
            if (hasMenu && isAdded() && !isHidden() && activity != null) {
                activity.invalidateOptionsMenu();
            }
        }
    }

    public void setUserVisibleHint(boolean isVisibleToUser) {
        this.userVisibleHint = isVisibleToUser;
    }

    public boolean getUserVisibleHint() {
        return userVisibleHint;
    }

    public final LayoutInflater getLayoutInflater() {
        return onGetLayoutInflater(savedFragmentState);
    }

    public LayoutInflater onGetLayoutInflater(Bundle savedInstanceState) {
        return activity != null ? activity.getLayoutInflater() : new LayoutInflater(null);
    }

    public View getView() {
        return view;
    }

    public void startActivity(Intent intent) {
        if (activity != null) activity.startActivity(intent);
    }

    public void startActivity(Intent intent, Bundle options) {
        if (activity != null) activity.startActivity(intent, options);
    }

    public void startActivityForResult(Intent intent, int requestCode) {
        if (activity != null) activity.startActivityForResult(intent, requestCode);
    }

    public void startActivityForResult(Intent intent, int requestCode, Bundle options) {
        startActivityForResult(intent, requestCode);
    }

    public final void requestPermissions(String[] permissions, int requestCode) {
        if (activity != null) activity.requestPermissions(permissions, requestCode);
    }

    public void registerForContextMenu(View view) {
    }

    public void unregisterForContextMenu(View view) {
    }

    public void dump(String prefix, FileDescriptor fd, PrintWriter writer, String[] args) {
    }

    // --------------------------------------------------------- Lifecycle Callbacks

    @Deprecated
    public void onAttach(Activity activity) {
    }

    public void onAttach(Context context) {
        if (context instanceof Activity a) {
            onAttach(a);
        }
    }

    public void onAttachFragment(Fragment childFragment) {
    }

    public void onInflate(Context context, AttributeSet attrs, Bundle savedInstanceState) {
    }

    @Deprecated
    public void onInflate(Activity activity, AttributeSet attrs, Bundle savedInstanceState) {
    }

    @Deprecated
    public void onInflate(AttributeSet attrs, Bundle savedInstanceState) {
    }

    public void onCreate(Bundle savedInstanceState) {
    }

    public Animator onCreateAnimator(int transit, boolean enter, int nextAnim) {
        return null;
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return null;
    }

    public void onViewCreated(View view, Bundle savedInstanceState) {
    }

    public void onActivityCreated(Bundle savedInstanceState) {
    }

    public void onViewStateRestored(Bundle savedInstanceState) {
    }

    public void onStart() {
    }

    public void onResume() {
    }

    public void onSaveInstanceState(Bundle outState) {
    }

    public void onPause() {
    }

    public void onStop() {
    }

    public void onDestroyView() {
    }

    public void onDestroy() {
    }

    public void onDetach() {
    }

    public void onHiddenChanged(boolean hidden) {
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
    }

    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    }

    public void onPrepareOptionsMenu(Menu menu) {
    }

    public void onDestroyOptionsMenu() {
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        return false;
    }

    public void onOptionsMenuClosed(Menu menu) {
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
    }

    public boolean onContextItemSelected(MenuItem item) {
        return false;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
    }

    @Override
    public void onLowMemory() {
    }

    @Override
    public void onTrimMemory(int level) {
    }
}
