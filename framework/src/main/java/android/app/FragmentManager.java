package android.app;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public abstract class FragmentManager {
    public static final int POP_BACK_STACK_INCLUSIVE = 1 << 0;

    public interface BackStackEntry {
        int getId();

        String getName();

        int getBreadCrumbTitleRes();

        int getBreadCrumbShortTitleRes();

        CharSequence getBreadCrumbTitle();

        CharSequence getBreadCrumbShortTitle();
    }

    public interface OnBackStackChangedListener {
        void onBackStackChanged();
    }

    public static abstract class FragmentLifecycleCallbacks {
        public void onFragmentPreAttached(FragmentManager fm, Fragment f, Context context) {
        }

        public void onFragmentAttached(FragmentManager fm, Fragment f, Context context) {
        }

        public void onFragmentCreated(FragmentManager fm, Fragment f, Bundle savedInstanceState) {
        }

        public void onFragmentViewCreated(FragmentManager fm, Fragment f, View v, Bundle savedInstanceState) {
        }

        public void onFragmentStarted(FragmentManager fm, Fragment f) {
        }

        public void onFragmentResumed(FragmentManager fm, Fragment f) {
        }

        public void onFragmentPaused(FragmentManager fm, Fragment f) {
        }

        public void onFragmentStopped(FragmentManager fm, Fragment f) {
        }

        public void onFragmentViewDestroyed(FragmentManager fm, Fragment f) {
        }

        public void onFragmentDestroyed(FragmentManager fm, Fragment f) {
        }

        public void onFragmentDetached(FragmentManager fm, Fragment f) {
        }
    }

    public static void enableDebugLogging(boolean enabled) {
    }

    public abstract FragmentTransaction beginTransaction();

    public abstract boolean executePendingTransactions();

    public abstract Fragment findFragmentById(int id);

    public abstract Fragment findFragmentByTag(String tag);

    public abstract void popBackStack();

    public abstract boolean popBackStackImmediate();

    public abstract void popBackStack(String name, int flags);

    public abstract boolean popBackStackImmediate(String name, int flags);

    public abstract void popBackStack(int id, int flags);

    public abstract boolean popBackStackImmediate(int id, int flags);

    public abstract int getBackStackEntryCount();

    public abstract BackStackEntry getBackStackEntryAt(int index);

    public abstract void addOnBackStackChangedListener(OnBackStackChangedListener listener);

    public abstract void removeOnBackStackChangedListener(OnBackStackChangedListener listener);

    public abstract void putFragment(Bundle bundle, String key, Fragment fragment);

    public abstract Fragment getFragment(Bundle bundle, String key);

    public abstract List<Fragment> getFragments();

    public abstract Fragment.SavedState saveFragmentInstanceState(Fragment f);

    public abstract boolean isDestroyed();

    public abstract void registerFragmentLifecycleCallbacks(FragmentLifecycleCallbacks cb, boolean recursive);

    public abstract void unregisterFragmentLifecycleCallbacks(FragmentLifecycleCallbacks cb);

    public abstract Fragment getPrimaryNavigationFragment();

    public void invalidateOptionsMenu() {
    }

    public abstract boolean isStateSaved();

    public void dispatchCreate() {
    }

    public void dispatchStart() {
    }

    public void dispatchResume() {
    }

    public void dispatchPause() {
    }

    public void dispatchStop() {
    }

    public void dispatchDestroy() {
    }

    public boolean dispatchCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        return false;
    }

    public boolean dispatchPrepareOptionsMenu(Menu menu) {
        return false;
    }

    public boolean dispatchOptionsItemSelected(MenuItem item) {
        return false;
    }

    /**
     * Cài đặt thực thi thực tế của FragmentManager trong AAOW.
     */
    static final class Impl extends FragmentManager {
        private static final int OP_ADD = 1;
        private static final int OP_REPLACE = 2;
        private static final int OP_REMOVE = 3;
        private static final int OP_HIDE = 4;
        private static final int OP_SHOW = 5;
        private static final int OP_DETACH = 6;
        private static final int OP_ATTACH = 7;

        private record Op(int cmd, int containerId, Fragment fragment, String tag) {
        }

        private static final class BackStackRecord implements BackStackEntry {
            final int id;
            final String name;
            final List<Op> ops;

            BackStackRecord(int id, String name, List<Op> ops) {
                this.id = id;
                this.name = name;
                this.ops = List.copyOf(ops);
            }

            @Override
            public int getId() {
                return id;
            }

            @Override
            public String getName() {
                return name;
            }

            @Override
            public int getBreadCrumbTitleRes() {
                return 0;
            }

            @Override
            public int getBreadCrumbShortTitleRes() {
                return 0;
            }

            @Override
            public CharSequence getBreadCrumbTitle() {
                return name;
            }

            @Override
            public CharSequence getBreadCrumbShortTitle() {
                return name;
            }
        }

        private final Activity activity;
        private final Fragment parent;
        private final List<Fragment> added = new ArrayList<>();
        private final List<BackStackRecord> backStack = new ArrayList<>();
        private final List<OnBackStackChangedListener> backStackListeners = new CopyOnWriteArrayList<>();
        private final List<FragmentLifecycleCallbacks> lifecycleCallbacks = new CopyOnWriteArrayList<>();
        private int curActivityState = Fragment.INITIALIZING;
        private boolean destroyed;
        private boolean stateSaved;
        private int nextBackStackId = 1;
        private Fragment primaryNavigationFragment;

        Impl(Activity activity, Fragment parent) {
            this.activity = activity;
            this.parent = parent;
        }

        @Override
        public FragmentTransaction beginTransaction() {
            return new TxImpl();
        }

        @Override
        public boolean executePendingTransactions() {
            return false;
        }

        @Override
        public Fragment findFragmentById(int id) {
            for (int i = added.size() - 1; i >= 0; i--) {
                Fragment f = added.get(i);
                if (f.fragmentId == id || f.containerId == id) return f;
            }
            return null;
        }

        @Override
        public Fragment findFragmentByTag(String tag) {
            if (tag == null) return null;
            for (int i = added.size() - 1; i >= 0; i--) {
                Fragment f = added.get(i);
                if (tag.equals(f.tag)) return f;
            }
            return null;
        }

        @Override
        public void popBackStack() {
            popBackStackImmediate();
        }

        @Override
        public boolean popBackStackImmediate() {
            if (backStack.isEmpty()) return false;
            BackStackRecord rec = backStack.remove(backStack.size() - 1);
            reverseRecord(rec);
            notifyBackStackChanged();
            return true;
        }

        @Override
        public void popBackStack(String name, int flags) {
            popBackStackImmediate(name, flags);
        }

        @Override
        public boolean popBackStackImmediate(String name, int flags) {
            if (backStack.isEmpty()) return false;
            if (name == null) return popBackStackImmediate();
            int idx = -1;
            for (int i = backStack.size() - 1; i >= 0; i--) {
                if (name.equals(backStack.get(i).name)) {
                    idx = i;
                    break;
                }
            }
            if (idx < 0) return false;
            int targetIdx = (flags & POP_BACK_STACK_INCLUSIVE) != 0 ? idx : idx + 1;
            while (backStack.size() > targetIdx) {
                reverseRecord(backStack.remove(backStack.size() - 1));
            }
            notifyBackStackChanged();
            return true;
        }

        @Override
        public void popBackStack(int id, int flags) {
            popBackStackImmediate(id, flags);
        }

        @Override
        public boolean popBackStackImmediate(int id, int flags) {
            int idx = -1;
            for (int i = backStack.size() - 1; i >= 0; i--) {
                if (backStack.get(i).id == id) {
                    idx = i;
                    break;
                }
            }
            if (idx < 0) return false;
            int targetIdx = (flags & POP_BACK_STACK_INCLUSIVE) != 0 ? idx : idx + 1;
            while (backStack.size() > targetIdx) {
                reverseRecord(backStack.remove(backStack.size() - 1));
            }
            notifyBackStackChanged();
            return true;
        }

        @Override
        public int getBackStackEntryCount() {
            return backStack.size();
        }

        @Override
        public BackStackEntry getBackStackEntryAt(int index) {
            return backStack.get(index);
        }

        @Override
        public void addOnBackStackChangedListener(OnBackStackChangedListener listener) {
            if (listener != null) backStackListeners.add(listener);
        }

        @Override
        public void removeOnBackStackChangedListener(OnBackStackChangedListener listener) {
            backStackListeners.remove(listener);
        }

        @Override
        public void putFragment(Bundle bundle, String key, Fragment fragment) {
            if (bundle != null && fragment != null && fragment.tag != null) {
                bundle.putString(key, fragment.tag);
            }
        }

        @Override
        public Fragment getFragment(Bundle bundle, String key) {
            if (bundle == null) return null;
            return findFragmentByTag(bundle.getString(key));
        }

        @Override
        public List<Fragment> getFragments() {
            return new ArrayList<>(added);
        }

        @Override
        public Fragment.SavedState saveFragmentInstanceState(Fragment f) {
            Bundle b = new Bundle();
            if (f != null) f.onSaveInstanceState(b);
            return new Fragment.SavedState(b);
        }

        @Override
        public boolean isDestroyed() {
            return destroyed;
        }

        @Override
        public boolean isStateSaved() {
            return stateSaved;
        }

        @Override
        public void registerFragmentLifecycleCallbacks(FragmentLifecycleCallbacks cb, boolean recursive) {
            if (cb != null) lifecycleCallbacks.add(cb);
        }

        @Override
        public void unregisterFragmentLifecycleCallbacks(FragmentLifecycleCallbacks cb) {
            lifecycleCallbacks.remove(cb);
        }

        @Override
        public Fragment getPrimaryNavigationFragment() {
            return primaryNavigationFragment;
        }

        @Override
        public void invalidateOptionsMenu() {
            if (activity != null) activity.invalidateOptionsMenu();
        }

        @Override
        public void dispatchCreate() {
            curActivityState = Math.max(curActivityState, Fragment.CREATED);
            stateSaved = false;
            moveAllToState(curActivityState);
        }

        @Override
        public void dispatchStart() {
            curActivityState = Math.max(curActivityState, Fragment.STARTED);
            stateSaved = false;
            moveAllToState(curActivityState);
        }

        @Override
        public void dispatchResume() {
            curActivityState = Math.max(curActivityState, Fragment.RESUMED);
            stateSaved = false;
            moveAllToState(curActivityState);
        }

        @Override
        public void dispatchPause() {
            curActivityState = Math.min(curActivityState, Fragment.STARTED);
            moveAllToState(Fragment.STARTED);
        }

        @Override
        public void dispatchStop() {
            curActivityState = Math.min(curActivityState, Fragment.CREATED);
            stateSaved = true;
            moveAllToState(Fragment.CREATED);
        }

        @Override
        public void dispatchDestroy() {
            destroyed = true;
            curActivityState = Fragment.INITIALIZING;
            for (Fragment f : List.copyOf(added)) {
                removeFragment(f);
            }
        }

        @Override
        public boolean dispatchCreateOptionsMenu(Menu menu, MenuInflater inflater) {
            boolean show = false;
            for (Fragment f : List.copyOf(added)) {
                if (f.isAdded() && !f.isHidden() && f.hasMenu && f.menuVisible) {
                    f.onCreateOptionsMenu(menu, inflater);
                    show = true;
                }
            }
            return show;
        }

        @Override
        public boolean dispatchPrepareOptionsMenu(Menu menu) {
            boolean show = false;
            for (Fragment f : List.copyOf(added)) {
                if (f.isAdded() && !f.isHidden() && f.hasMenu && f.menuVisible) {
                    f.onPrepareOptionsMenu(menu);
                    show = true;
                }
            }
            return show;
        }

        @Override
        public boolean dispatchOptionsItemSelected(MenuItem item) {
            for (Fragment f : List.copyOf(added)) {
                if (f.isAdded() && !f.isHidden() && f.hasMenu && f.menuVisible) {
                    if (f.onOptionsItemSelected(item)) return true;
                }
            }
            return false;
        }

        private void notifyBackStackChanged() {
            for (OnBackStackChangedListener l : backStackListeners) {
                l.onBackStackChanged();
            }
        }

        private void moveAllToState(int targetState) {
            for (Fragment f : List.copyOf(added)) {
                moveToState(f, targetState);
            }
        }

        private ViewGroup findContainer(int containerId) {
            if (activity == null) return null;
            if (containerId == 0 || containerId == Window.ID_ANDROID_CONTENT) {
                View v = activity.getWindow() != null ? activity.getWindow().getDecorView() : null;
                return v instanceof ViewGroup vg ? vg : null;
            }
            View v = activity.findViewById(containerId);
            return v instanceof ViewGroup vg ? vg : null;
        }

        private void moveToState(Fragment f, int targetState) {
            if (f == null) return;
            while (f.state < targetState) {
                switch (f.state) {
                    case Fragment.INITIALIZING -> {
                        f.activity = activity;
                        f.fragmentManager = this;
                        f.parentFragment = parent;
                        f.added = true;
                        f.onAttach((Context) activity);
                        if (activity != null) activity.onAttachFragment(f);
                        if (parent != null) parent.onAttachFragment(f);
                        f.onCreate(f.savedFragmentState);
                        for (var cb : lifecycleCallbacks) cb.onFragmentCreated(this, f, f.savedFragmentState);
                        f.state = Fragment.CREATED;
                    }
                    case Fragment.CREATED -> {
                        if (!f.detached) {
                            ViewGroup cont = findContainer(f.containerId);
                            f.container = cont;
                            if (f.view == null) {
                                LayoutInflater li = f.onGetLayoutInflater(f.savedFragmentState);
                                f.view = f.onCreateView(li, cont, f.savedFragmentState);
                                if (f.view != null) {
                                    f.onViewCreated(f.view, f.savedFragmentState);
                                    for (var cb : lifecycleCallbacks) {
                                        cb.onFragmentViewCreated(this, f, f.view, f.savedFragmentState);
                                    }
                                    if (cont != null && f.view.getParent() != cont) {
                                        cont.addView(f.view);
                                    }
                                    f.view.setVisibility(f.hidden ? View.GONE : View.VISIBLE);
                                }
                            }
                        }
                        f.onActivityCreated(f.savedFragmentState);
                        f.onViewStateRestored(f.savedFragmentState);
                        f.state = Fragment.ACTIVITY_CREATED;
                    }
                    case Fragment.ACTIVITY_CREATED -> {
                        f.onStart();
                        for (var cb : lifecycleCallbacks) cb.onFragmentStarted(this, f);
                        f.state = Fragment.STARTED;
                    }
                    case Fragment.STARTED -> {
                        f.onResume();
                        for (var cb : lifecycleCallbacks) cb.onFragmentResumed(this, f);
                        f.state = Fragment.RESUMED;
                    }
                }
            }

            while (f.state > targetState) {
                switch (f.state) {
                    case Fragment.RESUMED -> {
                        f.onPause();
                        for (var cb : lifecycleCallbacks) cb.onFragmentPaused(this, f);
                        f.state = Fragment.STARTED;
                    }
                    case Fragment.STARTED -> {
                        f.onStop();
                        for (var cb : lifecycleCallbacks) cb.onFragmentStopped(this, f);
                        f.state = Fragment.ACTIVITY_CREATED;
                    }
                    case Fragment.ACTIVITY_CREATED -> {
                        if (f.view != null) {
                            if (f.container != null) f.container.removeView(f.view);
                            f.onDestroyView();
                            for (var cb : lifecycleCallbacks) cb.onFragmentViewDestroyed(this, f);
                            f.view = null;
                        }
                        f.state = Fragment.CREATED;
                    }
                    case Fragment.CREATED -> {
                        f.onDestroy();
                        for (var cb : lifecycleCallbacks) cb.onFragmentDestroyed(this, f);
                        f.onDetach();
                        for (var cb : lifecycleCallbacks) cb.onFragmentDetached(this, f);
                        f.added = false;
                        f.activity = null;
                        f.fragmentManager = null;
                        f.state = Fragment.INITIALIZING;
                    }
                }
            }
            if (activity != null && f.hasMenu) {
                activity.invalidateOptionsMenu();
            }
        }

        private void addFragment(int containerId, Fragment f, String tag) {
            if (f == null) return;
            f.containerId = containerId;
            f.fragmentId = containerId;
            if (tag != null) f.tag = tag;
            f.removing = false;
            f.detached = false;
            if (!added.contains(f)) added.add(f);
            int target = Math.max(Fragment.ACTIVITY_CREATED, curActivityState);
            moveToState(f, target);
        }

        private void removeFragment(Fragment f) {
            if (f == null) return;
            f.removing = true;
            moveToState(f, Fragment.INITIALIZING);
            added.remove(f);
        }

        private void executeOps(List<Op> ops) {
            for (Op op : ops) {
                switch (op.cmd()) {
                    case OP_ADD -> addFragment(op.containerId(), op.fragment(), op.tag());
                    case OP_REPLACE -> {
                        if (op.containerId() != 0) {
                            for (Fragment existing : List.copyOf(added)) {
                                if (existing.containerId == op.containerId() && existing != op.fragment()) {
                                    removeFragment(existing);
                                }
                            }
                        }
                        addFragment(op.containerId(), op.fragment(), op.tag());
                    }
                    case OP_REMOVE -> removeFragment(op.fragment());
                    case OP_HIDE -> {
                        Fragment f = op.fragment();
                        if (f != null && !f.hidden) {
                            f.hidden = true;
                            if (f.view != null) f.view.setVisibility(View.GONE);
                            f.onHiddenChanged(true);
                        }
                    }
                    case OP_SHOW -> {
                        Fragment f = op.fragment();
                        if (f != null && f.hidden) {
                            f.hidden = false;
                            if (f.view != null) f.view.setVisibility(View.VISIBLE);
                            f.onHiddenChanged(false);
                        }
                    }
                    case OP_DETACH -> {
                        Fragment f = op.fragment();
                        if (f != null && !f.detached) {
                            f.detached = true;
                            if (f.view != null && f.container != null) {
                                f.container.removeView(f.view);
                                f.onDestroyView();
                                f.view = null;
                            }
                        }
                    }
                    case OP_ATTACH -> {
                        Fragment f = op.fragment();
                        if (f != null && f.detached) {
                            f.detached = false;
                            moveToState(f, Math.max(Fragment.ACTIVITY_CREATED, curActivityState));
                        }
                    }
                }
            }
        }

        private void reverseRecord(BackStackRecord rec) {
            List<Op> ops = rec.ops;
            for (int i = ops.size() - 1; i >= 0; i--) {
                Op op = ops.get(i);
                switch (op.cmd()) {
                    case OP_ADD, OP_REPLACE -> removeFragment(op.fragment());
                    case OP_REMOVE -> addFragment(op.containerId(), op.fragment(), op.tag());
                    case OP_HIDE -> {
                        Fragment f = op.fragment();
                        if (f != null) {
                            f.hidden = false;
                            if (f.view != null) f.view.setVisibility(View.VISIBLE);
                            f.onHiddenChanged(false);
                        }
                    }
                    case OP_SHOW -> {
                        Fragment f = op.fragment();
                        if (f != null) {
                            f.hidden = true;
                            if (f.view != null) f.view.setVisibility(View.GONE);
                            f.onHiddenChanged(true);
                        }
                    }
                    case OP_DETACH -> {
                        Fragment f = op.fragment();
                        if (f != null) {
                            f.detached = false;
                            moveToState(f, Math.max(Fragment.ACTIVITY_CREATED, curActivityState));
                        }
                    }
                    case OP_ATTACH -> removeFragment(op.fragment());
                }
            }
        }

        private final class TxImpl extends FragmentTransaction {
            private final List<Op> ops = new ArrayList<>();
            private final List<Runnable> runOnCommitActions = new ArrayList<>();
            private boolean addToBackStack;
            private boolean allowAddToBackStack = true;
            private String backStackName;

            @Override
            public FragmentTransaction add(Fragment fragment, String tag) {
                ops.add(new Op(OP_ADD, 0, fragment, tag));
                return this;
            }

            @Override
            public FragmentTransaction add(int containerViewId, Fragment fragment) {
                return add(containerViewId, fragment, null);
            }

            @Override
            public FragmentTransaction add(int containerViewId, Fragment fragment, String tag) {
                ops.add(new Op(OP_ADD, containerViewId, fragment, tag));
                return this;
            }

            @Override
            public FragmentTransaction replace(int containerViewId, Fragment fragment) {
                return replace(containerViewId, fragment, null);
            }

            @Override
            public FragmentTransaction replace(int containerViewId, Fragment fragment, String tag) {
                ops.add(new Op(OP_REPLACE, containerViewId, fragment, tag));
                return this;
            }

            @Override
            public FragmentTransaction remove(Fragment fragment) {
                ops.add(new Op(OP_REMOVE, 0, fragment, null));
                return this;
            }

            @Override
            public FragmentTransaction hide(Fragment fragment) {
                ops.add(new Op(OP_HIDE, 0, fragment, null));
                return this;
            }

            @Override
            public FragmentTransaction show(Fragment fragment) {
                ops.add(new Op(OP_SHOW, 0, fragment, null));
                return this;
            }

            @Override
            public FragmentTransaction detach(Fragment fragment) {
                ops.add(new Op(OP_DETACH, 0, fragment, null));
                return this;
            }

            @Override
            public FragmentTransaction attach(Fragment fragment) {
                ops.add(new Op(OP_ATTACH, 0, fragment, null));
                return this;
            }

            @Override
            public FragmentTransaction setPrimaryNavigationFragment(Fragment fragment) {
                primaryNavigationFragment = fragment;
                return this;
            }

            @Override
            public boolean isEmpty() {
                return ops.isEmpty();
            }

            @Override
            public FragmentTransaction setCustomAnimations(int enter, int exit) {
                return this;
            }

            @Override
            public FragmentTransaction setCustomAnimations(int enter, int exit, int popEnter, int popExit) {
                return this;
            }

            @Override
            public FragmentTransaction setTransition(int transit) {
                return this;
            }

            @Override
            public FragmentTransaction setTransitionStyle(int styleRes) {
                return this;
            }

            @Override
            public FragmentTransaction addSharedElement(View sharedElement, String name) {
                return this;
            }

            @Override
            public FragmentTransaction addToBackStack(String name) {
                if (!allowAddToBackStack) {
                    throw new IllegalStateException("This FragmentTransaction is not allowed to be added to the back stack.");
                }
                this.addToBackStack = true;
                this.backStackName = name;
                return this;
            }

            @Override
            public boolean isAddToBackStackAllowed() {
                return allowAddToBackStack;
            }

            @Override
            public FragmentTransaction disallowAddToBackStack() {
                if (addToBackStack) {
                    throw new IllegalStateException("This transaction is already being added to the back stack");
                }
                allowAddToBackStack = false;
                return this;
            }

            @Override
            public FragmentTransaction setBreadCrumbTitle(int res) {
                return this;
            }

            @Override
            public FragmentTransaction setBreadCrumbTitle(CharSequence text) {
                return this;
            }

            @Override
            public FragmentTransaction setBreadCrumbShortTitle(int res) {
                return this;
            }

            @Override
            public FragmentTransaction setBreadCrumbShortTitle(CharSequence text) {
                return this;
            }

            @Override
            public FragmentTransaction setReorderingAllowed(boolean reorderingAllowed) {
                return this;
            }

            @Override
            public FragmentTransaction runOnCommit(Runnable runnable) {
                if (runnable != null) runOnCommitActions.add(runnable);
                return this;
            }

            @Override
            public int commit() {
                return commitInternal();
            }

            @Override
            public int commitAllowingStateLoss() {
                return commitInternal();
            }

            @Override
            public void commitNow() {
                commitInternal();
            }

            @Override
            public void commitNowAllowingStateLoss() {
                commitInternal();
            }

            private int commitInternal() {
                List<Op> expanded = expandOps(ops);
                int id = -1;
                if (addToBackStack) {
                    id = nextBackStackId++;
                    backStack.add(new BackStackRecord(id, backStackName, expanded));
                }
                executeOps(expanded);
                for (Runnable r : runOnCommitActions) r.run();
                if (addToBackStack) notifyBackStackChanged();
                return id;
            }

            private List<Op> expandOps(List<Op> rawOps) {
                List<Op> out = new ArrayList<>();
                for (Op op : rawOps) {
                    if (op.cmd() == OP_REPLACE && op.containerId() != 0) {
                        for (Fragment existing : List.copyOf(added)) {
                            if (existing.containerId == op.containerId() && existing != op.fragment()) {
                                out.add(new Op(OP_REMOVE, existing.containerId, existing, existing.tag));
                            }
                        }
                        out.add(new Op(OP_ADD, op.containerId(), op.fragment(), op.tag()));
                    } else {
                        out.add(op);
                    }
                }
                return out;
            }
        }
    }

    public void dump(String prefix, FileDescriptor fd, PrintWriter writer, String[] args) {
    }
}
