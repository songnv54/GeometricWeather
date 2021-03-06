package wangdaye.com.geometricweather.view.widget.weatherView.trend;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v7.widget.LinearLayoutManager;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.FrameLayout;

import wangdaye.com.geometricweather.R;
import wangdaye.com.geometricweather.data.entity.model.History;
import wangdaye.com.geometricweather.data.entity.model.weather.Weather;
import wangdaye.com.geometricweather.utils.DisplayUtils;
import wangdaye.com.geometricweather.view.adapter.TrendAdapter;
import wangdaye.com.geometricweather.view.widget.SwitchImageButton;

/**
 * Trend view.
 * */

public class TrendView extends FrameLayout
        implements TrendAdapter.OnTrendItemClickListener {
    // widget
    private TrendRecyclerView recyclerView;
    private SwitchImageButton popBtn;
    private SwitchImageButton dateBtn;

    // data
    private TrendAdapter adapter;
    private Weather weather;
    private History history;
    private boolean showDailyPop;
    private boolean showDate;

    private int state = TrendItemView.DATA_TYPE_DAILY;

    private boolean animating = false;

    private static final String PREFERENCE_NAME = "sp_trend_view";
    private static final String KEY_DAILY_POP_SWITCH = "daily_pop_switch";
    private static final String KEY_DATE_SWITCH = "date_switch";

    // animator
    private AnimatorSet viewIn;
    private AnimatorSet viewOut;

    /** <br> life cycle. */

    public TrendView(Context context) {
        super(context);
        this.initialize();
    }

    public TrendView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.initialize();
    }

    public TrendView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.initialize();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public TrendView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        this.initialize();
    }

    @SuppressLint("InflateParams")
    private void initialize() {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.container_trend_view, null);
        addView(view);

        SharedPreferences sharedPreferences = getContext().getSharedPreferences(
                PREFERENCE_NAME, Context.MODE_PRIVATE);
        showDailyPop = sharedPreferences.getBoolean(KEY_DAILY_POP_SWITCH, false);
        showDate = sharedPreferences.getBoolean(KEY_DATE_SWITCH, false);

        this.adapter = new TrendAdapter(getContext(), null, null, showDailyPop, showDate, this);

        this.recyclerView = (TrendRecyclerView) findViewById(R.id.container_trend_view_recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        recyclerView.setAdapter(adapter);

        this.popBtn = (SwitchImageButton) findViewById(R.id.container_trend_view_popBtn);
        popBtn.initSwitchState(showDailyPop);
        popBtn.setOnSwitchListener(popSwitchListener);

        this.dateBtn = (SwitchImageButton) findViewById(R.id.container_trend_view_dateBtn);
        dateBtn.initSwitchState(showDate);
        dateBtn.setOnSwitchListener(dateSwitchListener);

        viewIn = (AnimatorSet) AnimatorInflater.loadAnimator(getContext(), R.animator.view_in);
        viewIn.setInterpolator(new AccelerateDecelerateInterpolator());
        viewIn.addListener(viewInListener);
        viewIn.setTarget(recyclerView);

        viewOut = (AnimatorSet) AnimatorInflater.loadAnimator(getContext(), R.animator.view_out);
        viewOut.setInterpolator(new AccelerateDecelerateInterpolator());
        viewOut.addListener(viewOutListener);
        viewOut.setTarget(recyclerView);
    }

    /** <br> UI. */

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = (int) (getContext().getResources().getDisplayMetrics().widthPixels - 2.0 * DisplayUtils.dpToPx(getContext(), 8));
        int height = TrendItemView.calcHeaderHeight(getContext()) + TrendItemView.calcDrawSpecHeight(getContext());
        getChildAt(0).measure(
                MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
        setMeasuredDimension(width, height);
    }

    public void reset() {
        adapter.setData(weather, history, showDailyPop, showDate, state);
        adapter.notifyDataSetChanged();

        switch (state) {
            case TrendItemView.DATA_TYPE_DAILY:
                popBtn.initSwitchState(popBtn.isSwitchOn());
                popBtn.setAlpha(1f);
                popBtn.setEnabled(true);
                dateBtn.initSwitchState(dateBtn.isSwitchOn());
                dateBtn.setAlpha(1f);
                dateBtn.setEnabled(true);
                break;

            default:
                popBtn.setAlpha(0f);
                popBtn.setEnabled(false);
                dateBtn.setAlpha(0f);
                dateBtn.setEnabled(false);
                break;
        }
    }

    /** data. */

    public void setData(Weather weather, History history) {
        if (weather != null) {
            this.weather = weather;
            this.history = history;
        }
    }

    public void setState(int stateTo, boolean animate) {
        if (animate) {
            if (stateTo == state || animating) {
                return;
            }
            this.state = stateTo;
            viewOut.start();
        } else {
            viewIn.cancel();
            viewOut.cancel();
            this.state = stateTo;

            reset();
            recyclerView.setData(weather, history, state);
        }
    }

    /** <br> interface. */

    // on trend item click listener.

    @Override
    public void onTrendItemClick() {
        switch (state) {
            case TrendItemView.DATA_TYPE_DAILY:
                setState(TrendItemView.DATA_TYPE_HOURLY, true);
                break;

            case TrendItemView.DATA_TYPE_HOURLY:
                setState(TrendItemView.DATA_TYPE_DAILY, true);
                break;
        }
    }

    // animator listener.

    private AnimatorListenerAdapter viewOutListener = new AnimatorListenerAdapter() {

        @Override
        public void onAnimationStart(Animator animation) {
            animating = true;
        }

        @Override
        public void onAnimationEnd(Animator animation) {
            animating = false;

            reset();
            recyclerView.setData(weather, history, state);

            viewIn.start();
        }
    };

    private AnimatorListenerAdapter viewInListener = new AnimatorListenerAdapter() {

        @Override
        public void onAnimationStart(Animator animation) {
            animating = true;
        }

        @Override
        public void onAnimationEnd(Animator animation) {
            animating = false;
        }
    };

    // on switch listener.

    private SwitchImageButton.OnSwitchListener popSwitchListener = new SwitchImageButton.OnSwitchListener() {
        @Override
        public void onSwitch(boolean on) {
            showDailyPop = on;
            adapter.setData(weather, history, showDailyPop, showDate, state);
            adapter.notifyDataSetChanged();

            SharedPreferences.Editor editor = getContext().getSharedPreferences(
                    PREFERENCE_NAME, Context.MODE_PRIVATE).edit();
            editor.putBoolean(KEY_DAILY_POP_SWITCH, on);
            editor.apply();
        }
    };

    private SwitchImageButton.OnSwitchListener dateSwitchListener = new SwitchImageButton.OnSwitchListener() {
        @Override
        public void onSwitch(boolean on) {
            showDate = on;
            adapter.setData(weather, history, showDailyPop, showDate, state);
            adapter.notifyDataSetChanged();

            SharedPreferences.Editor editor = getContext().getSharedPreferences(
                    PREFERENCE_NAME, Context.MODE_PRIVATE).edit();
            editor.putBoolean(KEY_DATE_SWITCH, on);
            editor.apply();
        }
    };
}
