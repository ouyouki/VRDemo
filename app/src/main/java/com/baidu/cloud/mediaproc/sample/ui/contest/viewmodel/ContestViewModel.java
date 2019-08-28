package com.baidu.cloud.mediaproc.sample.ui.contest.viewmodel;

import android.databinding.ObservableArrayMap;
import android.databinding.ObservableBoolean;
import android.databinding.ObservableField;
import android.databinding.ObservableInt;
import android.databinding.ObservableMap;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.cloud.media.player.IMediaPlayer;
import com.baidu.cloud.mediaproc.sample.databinding.ActivityContestBinding;
import com.baidu.cloud.mediaproc.sample.http.ContestApi;
import com.baidu.cloud.mediaproc.sample.http.request.GetStatusModel;
import com.baidu.cloud.mediaproc.sample.http.request.PostAnswerModel;
import com.baidu.cloud.mediaproc.sample.http.response.ContestStatus;
import com.baidu.cloud.mediaproc.sample.http.response.HttpResponse;
import com.baidu.cloud.mediaproc.sample.ui.base.BaseModel;
import com.baidu.cloud.mediaproc.sample.widget.CaptureProgressView;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.trello.rxlifecycle2.android.ActivityEvent;
import com.trello.rxlifecycle2.android.RxLifecycleAndroid;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.reactivex.Flowable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.BehaviorSubject;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;


public class ContestViewModel extends BaseModel implements IMediaPlayer.OnMetadataListener {
    private static final String TAG = "ContestViewModel";

    private final ContestApi contestApi;

    private GetStatusModel getStatusModel = new GetStatusModel();
    private PostAnswerModel postAnswerModel = new PostAnswerModel();
    public ObservableBoolean isQuestionDialogShow = new ObservableBoolean(false);
    public ObservableBoolean isAnswerShow = new ObservableBoolean(false);
    public ObservableBoolean userAnswerRight = new ObservableBoolean(false);
    public ObservableBoolean hasAnswerRight = new ObservableBoolean(true);
    public ObservableField<String> playerCount = new ObservableField<>("1人");
    public ObservableField<String> reviverCount = new ObservableField<>("复活卡：3");
    public ObservableField<String> topic = new ObservableField<>("");
    public ObservableMap<String, String> options = new ObservableArrayMap<>();
    public ObservableMap<String, String> answerCounts = new ObservableArrayMap<>();
    public ObservableInt answerPeopleCount = new ObservableInt(0);

    public ObservableField<String> countDown = new ObservableField<>();

    private Map<String, ProgressBar> progressBars = new HashMap<>();
    private CaptureProgressView progressView;
    private TextView countDownText;
    private RadioGroup radioGroup;

    public ContestViewModel(BehaviorSubject<ActivityEvent> lifecycleSubject,
                            final ActivityContestBinding binding, final String playUrl,
                            String hostUrl) {
        radioGroup = binding.radioGroup;
        progressBars.put("A", binding.progressBar);
        progressBars.put("B", binding.progressBar1);
        progressBars.put("C", binding.progressBar2);
        progressView = binding.countDownView;
        countDownText = binding.countDownText;
        initAnswerPanel();
        Pattern pattern = Pattern.compile("((?<=http://)|(?<=rtmp://))(.+?)(?=/)");
        Matcher matcher = pattern.matcher(playUrl);
        String domainWithPortal = "";
        if (matcher.find()) {
            domainWithPortal = matcher.group();
            if (domainWithPortal.contains(":")) {
                getStatusModel.playDomain = domainWithPortal.substring(0, domainWithPortal.indexOf(":"));
            } else {
                getStatusModel.playDomain = domainWithPortal;
            }
        }
        pattern = Pattern.compile("(?<=" + domainWithPortal + "/)(.+?)(?=/)");
        matcher = pattern.matcher(playUrl);
        if (matcher.find()) {
            getStatusModel.app = matcher.group();
        }
        pattern = Pattern.compile("(?<=" + domainWithPortal
                + "/" + getStatusModel.app + "/" + ")(.+?)((?=$)|((?=\\.flv)))");
        matcher = pattern.matcher(playUrl);
        if (matcher.find()) {
            getStatusModel.stream = matcher.group();
        }
        postAnswerModel.playDomain = getStatusModel.playDomain;
        postAnswerModel.app = getStatusModel.app;
        postAnswerModel.stream = getStatusModel.stream;
        postAnswerModel.answer = new PostAnswerModel.Answer();

        contestApi = createContestApi(hostUrl);
        contestApi.getStatus(getStatusModel)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<HttpResponse<ContestStatus>>() {
                    @Override
                    public void accept(HttpResponse<ContestStatus> response) throws Exception {
                        if (response.success) {
                            playerCount.set(response.result.playCount + "人");
                            if (response.result.status == 1) {
                                hasAnswerRight.set(false);
                            } else {
                                Toast.makeText(binding.radioGroup.getContext(),
                                        "答题还未开始", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(binding.radioGroup.getContext(),
                                    response.message.global, Toast.LENGTH_SHORT).show();
                        }
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        throwable.printStackTrace();
                        Toast.makeText(binding.radioGroup.getContext(),
                                throwable.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
        Flowable.interval(10, TimeUnit.SECONDS)
                .compose(RxLifecycleAndroid.<Long>bindActivity(lifecycleSubject))
                .flatMap(new Function<Long, Flowable<HttpResponse<ContestStatus>>>() {
                    @Override
                    public Flowable<HttpResponse<ContestStatus>> apply(Long aLong) throws Exception {
                        return contestApi.getStatus(getStatusModel);
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<HttpResponse<ContestStatus>>() {
                    @Override
                    public void accept(HttpResponse<ContestStatus> response) throws Exception {
                        if (response.success) {
                            playerCount.set(response.result.playCount + "人");
                            if (response.result.status != 1) {
                                Toast.makeText(binding.radioGroup.getContext(),
                                        "答题还未开始", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(binding.radioGroup.getContext(),
                                    response.message.global, Toast.LENGTH_SHORT).show();
                        }
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        Toast.makeText(binding.radioGroup.getContext(),
                                throwable.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    public void onAnswerClick(final View view) {
        if (isQuestionDialogShow.get() && view instanceof RadioButton) {
            postAnswerModel.answer.topic = topic.get();
            postAnswerModel.answer.option = ((RadioButton) view).getText().toString();
            contestApi.postUserAnswer(postAnswerModel)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Consumer<HttpResponse>() {
                        @Override
                        public void accept(HttpResponse httpResponse) throws Exception {

                        }
                    }, new Consumer<Throwable>() {
                        @Override
                        public void accept(Throwable throwable) throws Exception {
                            Toast.makeText(view.getContext(),
                                    throwable.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    private ContestApi createContestApi(String hostUrl) {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);

        GsonBuilder builder = new GsonBuilder();
        builder.serializeNulls();
        builder.registerTypeHierarchyAdapter(int.class, new JsonDeserializer<Integer>() {
            @Override
            public Integer deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                    throws JsonParseException {
                String s = json.getAsString();
                if ("".equals(s)) {
                    return 0;
                } else {
                    return Integer.parseInt(s);
                }
            }
        });

        OkHttpClient.Builder httpClient = new OkHttpClient.Builder();
        httpClient.addInterceptor(logging);
        return new Retrofit.Builder()
                .baseUrl(hostUrl)
                .client(httpClient.build())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create(builder.create()))
                .build()
                .create(ContestApi.class);
    }

    @Override
    public void onResume() {

    }

    @Override
    public void onPause() {

    }

    @Override
    public void onDestroy() {

    }

    @Override
    public void onMetadata(IMediaPlayer iMediaPlayer, Bundle bundle) {
        if (bundle.keySet().contains("metadata")) {
            for (String key : bundle.keySet()) {
                Log.d(TAG, "onMetadata: key = " + key + ", value = " + bundle.getString(key));
            }
            try {
                JSONObject jsonObject = new JSONObject(bundle.getString("metadata"));
                if (TextUtils.isEmpty(jsonObject.optString("standardAnswer"))) {
                    showQuestion(jsonObject.optJSONObject("question"));
                } else if (topic.get().equals(jsonObject.optJSONObject("question").optString("topic"))) {
                    // 只有答案和问题匹配才会显示答案，如果答案的问题和当前的问题不一样，说明控制台没控制好下发答案和题目的节奏，顺序错乱
                    showAnswer(jsonObject.optString("standardAnswer"), jsonObject.optJSONObject("userAnswerCount"));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private void showQuestion(JSONObject jsonObject) throws JSONException {
        // 如果正在显示问题，就返回，说明控制台没控制好下发答案和题目的节奏，顺序错乱
        if (isQuestionDialogShow.get() || isAnswerShow.get()) {
            return;
        }
        topic.set(jsonObject.optString("topic"));
        JSONArray optionsArray = jsonObject.getJSONArray("options");
        options.put("A", optionsArray.getString(0));
        options.put("B", optionsArray.getString(1));
        options.put("C", optionsArray.getString(2));
        isQuestionDialogShow.set(true);
        Flowable.interval(0, 1, TimeUnit.SECONDS)
                .take(10)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Long>() {
                    @Override
                    public void accept(Long aLong) throws Exception {
                        if (aLong <= 7) {
                            progressView.setFinishedStrokeColor(Color.parseColor("#108CEE"));
                            countDownText.setTextColor(Color.parseColor("#108CEE"));
                        } else {
                            progressView.setFinishedStrokeColor(Color.parseColor("#E44E4E"));
                            countDownText.setTextColor(Color.parseColor("#E44E4E"));
                        }
                        progressView.setProgress(aLong);
                        countDown.set((10 - aLong) + "");
                    }
                });
        Flowable.timer(10, TimeUnit.SECONDS)
                .subscribe(new Consumer<Long>() {
                    @Override
                    public void accept(Long aLong) throws Exception {
                        isQuestionDialogShow.set(false);
                    }
                });
    }

    private void showAnswer(String standardAnswer, JSONObject userAnswerCount) throws JSONException {
        if (isAnswerShow.get()) {
            return;
        }
        userAnswerRight.set(standardAnswer.equals(postAnswerModel.answer.option));
        if (userAnswerCount != null) {
            int a = userAnswerCount.optInt(options.get("A"));
            int b = userAnswerCount.optInt(options.get("B"));
            int c = userAnswerCount.optInt(options.get("C"));
            answerCounts.put("A", a + "人");
            answerCounts.put("B", b + "人");
            answerCounts.put("C", c + "人");
            answerPeopleCount.set(a + b + c);
            if (standardAnswer.equals(options.get("A"))) {
                progressBars.get("A").setProgress(a);
            } else {
                progressBars.get("A").setSecondaryProgress(a);
            }
            if (standardAnswer.equals(options.get("B"))) {
                progressBars.get("B").setProgress(a);
            } else {
                progressBars.get("B").setSecondaryProgress(a);
            }
            if (standardAnswer.equals(options.get("C"))) {
                progressBars.get("C").setProgress(a);
            } else {
                progressBars.get("C").setSecondaryProgress(a);
            }
        }
        isQuestionDialogShow.set(true);
        isAnswerShow.set(true);
        Flowable.timer(5, TimeUnit.SECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Long>() {
                    @Override
                    public void accept(Long aLong) throws Exception {
                        isQuestionDialogShow.set(false);
                        isAnswerShow.set(false);
                        // 答案结果消失后，用户是否还有回答问题的权利
                        hasAnswerRight.set(userAnswerRight.get());
                        initAnswerPanel();
                    }
                });
    }

    private void initAnswerPanel() {
        radioGroup.clearCheck();
        for (ProgressBar progressBar : progressBars.values()) {
            progressBar.setProgress(0);
            progressBar.setSecondaryProgress(0);
        }
        answerCounts.put("A", "0人");
        answerCounts.put("B", "0人");
        answerCounts.put("C", "0人");
    }
}
