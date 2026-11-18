package dev.aaow.launcher;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.Application;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Looper;
import android.preference.PreferenceFragment;
import android.util.AttributeSet;
import android.util.Xml;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.view.animation.AlphaAnimation;
import android.view.animation.LinearInterpolator;
import android.view.animation.PathInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toolbar;
import dev.aaow.apk.ApkFile;
import dev.aaow.rt.AndroidRuntimeContext;
import dev.aaow.rt.SimpleMenu;
import dev.aaow.rt.compat.CompatCardView;
import dev.aaow.rt.compat.CompatConstraintLayout;
import dev.aaow.rt.compat.CompatRecyclerView;
import dev.aaow.rt.compat.CompatViewPager;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.xmlpull.v1.XmlPullParser;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Kiểm thử đơn vị toàn bộ tính năng Sprint 4:
 * Menu/MenuInflater, Fragment/FragmentManager/OnBackInvoked, Resources/Theme layer stack,
 * Compat containers (ConstraintLayout/CardView/RecyclerView/ViewPager), Animations, Gestures & SurfaceView.
 */
class Sprint4FrameworkTest {

    private static Path resolveRepoFile(String relative) {
        Path p = Path.of(relative);
        if (Files.exists(p)) return p;
        return Path.of("..").resolve(relative);
    }

    private static Context createInMemoryContext() {
        Resources res = new Resources(null);
        Resources.Theme theme = res.newTheme();
        return new ContextWrapper(null) {
            @Override
            public Resources getResources() {
                return res;
            }

            @Override
            public AssetManager getAssets() {
                return res.getAssets();
            }

            @Override
            public Resources.Theme getTheme() {
                return theme;
            }

            @Override
            public String getPackageName() {
                return "dev.aaow.test";
            }

            @Override
            public ClassLoader getClassLoader() {
                return Sprint4FrameworkTest.class.getClassLoader();
            }

            @Override
            public PackageManager getPackageManager() {
                return null;
            }

            @Override
            public Context getApplicationContext() {
                return this;
            }

            @Override
            public Looper getMainLooper() {
                return Looper.getMainLooper();
            }

            @Override
            public Object getSystemService(String name) {
                if (SENSOR_SERVICE.equals(name)) return new SensorManager();
                return null;
            }
        };
    }

    @Test
    void menuInflaterAndToolbarDispatchWorkOnBenchmarkApks(@TempDir Path tempDir) throws Exception {
        Path editorApkPath = resolveRepoFile("build/benchmark-apks/org.billthefarmer.editor_198.apk");
        Path notesApkPath = resolveRepoFile("build/benchmark-apks/org.billthefarmer.notes_142.apk");
        Assumptions.assumeTrue(Files.exists(editorApkPath) && Files.exists(notesApkPath));

        try (ApkFile apk = new ApkFile(editorApkPath)) {
            AndroidRuntimeContext ctx = new AndroidRuntimeContext(apk, getClass().getClassLoader(), tempDir.resolve("editor"));
            SimpleMenu menu = new SimpleMenu(ctx);
            new MenuInflater(ctx).inflate(0x7f070000, menu); // menu/main
            assertTrue(menu.size() >= 5, "Editor menu/main phải có ít nhất 5 mục");
            assertFalse(menu.getVisibleItems().isEmpty());

            AtomicInteger clickedId = new AtomicInteger(-1);
            Toolbar toolbar = new Toolbar(ctx);
            toolbar.setOnMenuItemClickListener(item -> {
                clickedId.set(item.getItemId());
                return true;
            });
            toolbar.inflateMenu(0x7f070000);
            MenuItem first = toolbar.getMenu().getItem(0);
            assertNotNull(first);
            assertTrue(toolbar.dispatchMenuItemClick(first));
            assertEquals(first.getItemId(), clickedId.get());
        }

        try (ApkFile apk = new ApkFile(notesApkPath)) {
            AndroidRuntimeContext ctx = new AndroidRuntimeContext(apk, getClass().getClassLoader(), tempDir.resolve("notes"));
            SimpleMenu navMenu = new SimpleMenu(ctx);
            new MenuInflater(ctx).inflate(0x7f080001, navMenu); // menu/navigation
            assertTrue(navMenu.size() >= 1);

            Activity host = new Activity();
            host.attach(ctx, new Application(), new Intent(), "NotesPrefs", () -> {}, () -> {});
            PreferenceFragment pf = new PreferenceFragment() {
                @Override
                public void onCreate(Bundle savedInstanceState) {
                    super.onCreate(savedInstanceState);
                    addPreferencesFromResource(0x7f0c0001); // xml/preferences
                }
            };
            FrameLayout container = new FrameLayout(host);
            container.setId(42);
            host.setContentView(container);
            host.getFragmentManager().beginTransaction().add(42, pf, "prefs").commitNow();
            host.performCreate(null);
            assertNotNull(pf.getPreferenceScreen());
            assertTrue(pf.getPreferenceScreen().getPreferenceCount() > 0);
        }
    }

    @Test
    void fragmentBackStackAndOnBackInvokedPriority() {
        Context base = createInMemoryContext();
        Application app = new Application();
        Activity act = new Activity();
        act.attach(base, app, new Intent(), "FragHost", () -> {}, () -> {});

        List<String> events = new ArrayList<>();
        act.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
            @Override public void onActivityCreated(Activity a, Bundle b) { events.add("act:created"); }
            @Override public void onActivityPostCreated(Activity a, Bundle b) { events.add("act:postCreated"); }
            @Override public void onActivityStarted(Activity a) { events.add("act:started"); }
            @Override public void onActivityResumed(Activity a) { events.add("act:resumed"); }
            @Override public void onActivityPaused(Activity a) {}
            @Override public void onActivityStopped(Activity a) {}
            @Override public void onActivitySaveInstanceState(Activity a, Bundle out) {}
            @Override public void onActivityDestroyed(Activity a) {}
        });

        FrameLayout container = new FrameLayout(act);
        container.setId(1001);
        act.setContentView(container);

        Fragment f1 = new Fragment() {
            @Override
            public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle saved) {
                events.add("f1:createView");
                TextView tv = new TextView(getActivity());
                tv.setText("FirstFragment");
                return tv;
            }
        };
        Fragment f2 = new Fragment() {
            @Override
            public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle saved) {
                events.add("f2:createView");
                TextView tv = new TextView(getActivity());
                tv.setText("SecondFragment");
                return tv;
            }
        };

        FragmentManager fm = act.getFragmentManager();
        fm.registerFragmentLifecycleCallbacks(new FragmentManager.FragmentLifecycleCallbacks() {
            @Override
            public void onFragmentResumed(FragmentManager mgr, Fragment f) {
                events.add("cb:resumed:" + f.getTag());
            }
        }, false);

        fm.beginTransaction().add(1001, f1, "f1").commitNow();
        act.performCreate(null);
        act.performStart();
        act.performResume();

        assertEquals(1, container.getChildCount());
        assertEquals("FirstFragment", ((TextView) container.getChildAt(0)).getText().toString());
        assertTrue(events.contains("act:postCreated"));
        assertTrue(events.contains("cb:resumed:f1"));

        fm.beginTransaction().replace(1001, f2, "f2").addToBackStack("step1").commitNow();
        assertEquals(1, container.getChildCount());
        assertEquals("SecondFragment", ((TextView) container.getChildAt(0)).getText().toString());

        // Kiểm tra OnBackInvokedDispatcher ưu tiên cao hơn BackStack:
        List<String> backLog = new ArrayList<>();
        android.window.OnBackInvokedCallback lowCb = () -> backLog.add("low");
        android.window.OnBackInvokedCallback highCb = () -> backLog.add("high");
        act.getOnBackInvokedDispatcher().registerOnBackInvokedCallback(0, lowCb);
        act.getOnBackInvokedDispatcher().registerOnBackInvokedCallback(10, highCb);

        act.onBackPressed();
        assertEquals(List.of("high"), backLog);
        assertEquals("SecondFragment", ((TextView) container.getChildAt(0)).getText().toString());

        // Sau khi gỡ bỏ back callbacks, onBackPressed() tự động pop BackStack về Fragment f1:
        act.getOnBackInvokedDispatcher().unregisterOnBackInvokedCallback(highCb);
        act.getOnBackInvokedDispatcher().unregisterOnBackInvokedCallback(lowCb);
        act.onBackPressed();
        assertEquals("FirstFragment", ((TextView) container.getChildAt(0)).getText().toString());
        assertFalse(act.isFinishing());
    }

    @Test
    void themeLayerStackXmlParserAndViewStub(@TempDir Path tempDir) throws Exception {
        Path levelApkPath = resolveRepoFile("build/benchmark-apks/org.woheller69.level_35.apk");
        Path editorApkPath = resolveRepoFile("build/benchmark-apks/org.billthefarmer.editor_198.apk");
        Assumptions.assumeTrue(Files.exists(levelApkPath) && Files.exists(editorApkPath));

        try (ApkFile levelApk = new ApkFile(levelApkPath)) {
            Resources res = new Resources(levelApk);
            Resources.Theme theme = res.newTheme();
            theme.applyStyle(0x7f1100a2, true); // LevelTheme
            // Áp đè một theme rỗng với force=true (mô phỏng AppCompatDelegateImpl.Theme_AppCompat_Empty)
            theme.applyStyle(0x7f110117, true);
            try (TypedArray ta = theme.obtainStyledAttributes(new int[]{0x7f040171})) { // windowActionBar
                assertTrue(ta.hasValue(0));
                assertTrue(ta.getBoolean(0, false));
            }
        }

        try (ApkFile editorApk = new ApkFile(editorApkPath)) {
            AndroidRuntimeContext ctx = new AndroidRuntimeContext(editorApk, getClass().getClassLoader(), tempDir.resolve("ed-xml"));
            Resources res = ctx.getResources();
            try (XmlResourceParser parser = res.getXml(0x7f070000)) {
                int ev = parser.getEventType();
                assertEquals(XmlPullParser.START_DOCUMENT, ev);
                while (ev != XmlPullParser.START_TAG && ev != XmlPullParser.END_DOCUMENT) {
                    ev = parser.next();
                }
                assertEquals("menu", parser.getName());
                AttributeSet attrs = Xml.asAttributeSet(parser);
                assertNotNull(attrs);
            }

            // ViewStub inflate thay thế chính nó trong ViewGroup cha:
            int layoutId = res.getIdentifier("edit", "layout", ctx.getPackageName());
            assertTrue(layoutId != 0);
            FrameLayout parent = new FrameLayout(ctx);
            ViewStub stub = new ViewStub(ctx, layoutId);
            stub.setInflatedId(9090);
            parent.addView(stub);
            View inflated = stub.inflate();
            assertNotNull(inflated);
            assertEquals(9090, inflated.getId());
            assertEquals(1, parent.getChildCount());
            assertSame(inflated, parent.getChildAt(0));
        }
    }

    @Test
    void compatContainersLayoutAndRecyclerAdapter() {
        Context ctx = createInMemoryContext();

        CompatConstraintLayout root = new CompatConstraintLayout(ctx);
        TextView top = new TextView(ctx);
        top.setId(1);
        top.setText("Header");
        CompatConstraintLayout.LayoutParams lpTop = new CompatConstraintLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT);
        lpTop.leftToLeft = CompatConstraintLayout.PARENT_ID;
        lpTop.rightToRight = CompatConstraintLayout.PARENT_ID;
        lpTop.topToTop = CompatConstraintLayout.PARENT_ID;
        root.addView(top, lpTop);

        CompatCardView card = new CompatCardView(ctx);
        card.setId(2);
        card.setCardBackgroundColor(Color.WHITE);
        CompatConstraintLayout.LayoutParams lpCard = new CompatConstraintLayout.LayoutParams(200, 100);
        lpCard.topToBottom = 1;
        lpCard.leftToLeft = CompatConstraintLayout.PARENT_ID;
        lpCard.rightToRight = CompatConstraintLayout.PARENT_ID;
        lpCard.horizontalBias = 0.5f;
        root.addView(card, lpCard);

        root.measure(
                View.MeasureSpec.makeMeasureSpec(400, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(600, View.MeasureSpec.EXACTLY));
        root.layout(0, 0, 400, 600);
        assertEquals(400, top.getWidth());
        assertEquals(100, card.getLeft());
        assertTrue(card.getTop() >= top.getBottom());

        CompatRecyclerView rv = new CompatRecyclerView(ctx);
        List<String> data = new ArrayList<>(List.of("One", "Two"));
        rv.setAdapter(new CompatRecyclerView.Adapter<>() {
            @Override
            public CompatRecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                return new CompatRecyclerView.ViewHolder(new TextView(ctx)) {};
            }

            @Override
            public void onBindViewHolder(CompatRecyclerView.ViewHolder holder, int position) {
                ((TextView) holder.itemView).setText(data.get(position));
            }

            @Override
            public int getItemCount() {
                return data.size();
            }
        });
        assertEquals(2, rv.getChildCount());
        data.add("Three");
        rv.getAdapter().notifyDataSetChanged();
        assertEquals(3, rv.getChildCount());

        CompatViewPager pager = new CompatViewPager(ctx);
        pager.addView(new TextView(ctx));
        pager.addView(new TextView(ctx));
        pager.measure(
                View.MeasureSpec.makeMeasureSpec(400, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(300, View.MeasureSpec.EXACTLY));
        pager.layout(0, 0, 400, 300);
        pager.setCurrentItem(1, false);
        assertEquals(1, pager.getCurrentItem());
        assertEquals(400, pager.getScrollX());
    }

    @Test
    void animatorsAndGestureDetectors() {
        Context ctx = createInMemoryContext();
        TextView view = new TextView(ctx);

        ValueAnimator va = ValueAnimator.ofFloat(0f, 100f);
        va.setInterpolator(new LinearInterpolator());
        va.setDuration(200);
        va.setCurrentFraction(0.5f);
        assertEquals(50f, ((Number) va.getAnimatedValue()).floatValue(), 0.01f);

        AtomicBoolean animEnded = new AtomicBoolean(false);
        ObjectAnimator oaAlpha = ObjectAnimator.ofFloat(view, View.ALPHA, 1f, 0.2f);
        ObjectAnimator oaTz = ObjectAnimator.ofFloat(view, View.TRANSLATION_Z, 0f, 8f);
        AnimatorSet set = new AnimatorSet();
        set.setDuration(0);
        set.playTogether(oaAlpha, oaTz);
        set.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                animEnded.set(true);
            }
        });
        set.start();
        set.end();
        assertEquals(0.2f, view.getAlpha(), 0.02f);
        assertEquals(8f, view.getTranslationZ(), 0.02f);
        assertTrue(animEnded.get());

        PathInterpolator pi = new PathInterpolator(0.4f, 0f, 0.2f, 1f);
        assertEquals(0f, pi.getInterpolation(0f), 0.001f);
        assertEquals(1f, pi.getInterpolation(1f), 0.001f);
        assertNotNull(new AlphaAnimation(0f, 1f));
        assertNotNull(new TranslateAnimation(0, 10, 0, 20));

        // GestureDetector + ScaleGestureDetector
        AtomicInteger taps = new AtomicInteger();
        AtomicInteger scrolls = new AtomicInteger();
        GestureDetector gd = new GestureDetector(ctx, new GestureDetector.SimpleOnGestureListener() {
            @Override public boolean onDown(MotionEvent e) { return true; }
            @Override public boolean onSingleTapUp(MotionEvent e) { taps.incrementAndGet(); return true; }
            @Override public boolean onScroll(MotionEvent e1, MotionEvent e2, float dx, float dy) { scrolls.incrementAndGet(); return true; }
        });
        gd.onTouchEvent(MotionEvent.obtain(0, 10, MotionEvent.ACTION_DOWN, 100f, 100f, 0));
        gd.onTouchEvent(MotionEvent.obtain(0, 20, MotionEvent.ACTION_MOVE, 130f, 140f, 0));
        gd.onTouchEvent(MotionEvent.obtain(0, 30, MotionEvent.ACTION_UP, 130f, 140f, 0));
        assertTrue(scrolls.get() >= 1);

        AtomicReference<Float> maxScale = new AtomicReference<>(1f);
        ScaleGestureDetector sgd = new ScaleGestureDetector(ctx, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                maxScale.set(Math.max(maxScale.get(), detector.getScaleFactor()));
                return true;
            }
        });
        sgd.onTouchEvent(MotionEvent.obtainMultiPointer(0, 10, MotionEvent.ACTION_POINTER_DOWN, new float[]{100f, 200f}, new float[]{100f, 100f}));
        sgd.onTouchEvent(MotionEvent.obtainMultiPointer(0, 20, MotionEvent.ACTION_MOVE, new float[]{50f, 250f}, new float[]{100f, 100f}));
        assertTrue(maxScale.get() > 1.5f);
    }

    @Test
    void surfaceViewAndVirtualSensorManager() {
        Context ctx = createInMemoryContext();
        SurfaceView sv = new SurfaceView(ctx);
        AtomicBoolean created = new AtomicBoolean(false);
        AtomicBoolean changed = new AtomicBoolean(false);
        sv.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override public void surfaceCreated(SurfaceHolder holder) { created.set(true); }
            @Override public void surfaceChanged(SurfaceHolder holder, int f, int w, int h) { changed.set(true); }
            @Override public void surfaceDestroyed(SurfaceHolder holder) {}
        });
        sv.measure(View.MeasureSpec.makeMeasureSpec(300, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(300, View.MeasureSpec.EXACTLY));
        sv.layout(0, 0, 300, 300);
        assertTrue(created.get());
        assertTrue(changed.get());

        Canvas c = sv.getHolder().lockCanvas();
        assertNotNull(c);
        c.drawColor(Color.GREEN);
        Paint p = new Paint();
        p.setColor(Color.WHITE);
        c.drawCircle(150, 150, 40, p);
        sv.getHolder().unlockCanvasAndPost(c);
        sv.draw(new Canvas());

        SensorManager sm = (SensorManager) ctx.getSystemService(Context.SENSOR_SERVICE);
        assertNotNull(sm);
        assertNotNull(sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER));
        assertFalse(sm.getSensorList(Sensor.TYPE_ALL).isEmpty());
    }
}
