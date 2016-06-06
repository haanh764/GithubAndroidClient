package com.frodo.github.business.repository;


import android.support.v4.util.Pair;
import android.util.LruCache;

import com.fasterxml.jackson.core.type.TypeReference;
import com.frodo.app.android.core.task.AndroidFetchNetworkDataTask;
import com.frodo.app.android.core.toolbox.JsonConverter;
import com.frodo.app.framework.controller.AbstractModel;
import com.frodo.app.framework.controller.MainController;
import com.frodo.app.framework.net.NetworkTransport;
import com.frodo.app.framework.net.Request;
import com.frodo.app.framework.net.Response;
import com.frodo.app.framework.toolbox.TextUtils;
import com.frodo.github.bean.dto.response.GitBlob;
import com.frodo.github.bean.dto.response.Issue;
import com.frodo.github.bean.dto.response.Label;
import com.frodo.github.bean.dto.response.PullRequest;
import com.frodo.github.bean.dto.response.Repo;
import com.frodo.github.business.user.UserModel;
import com.frodo.github.common.Path;

import java.io.IOException;
import java.util.List;

import okhttp3.ResponseBody;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Func1;

/**
 * Created by frodo on 2016/5/7.
 */
public class RepositoryModel extends AbstractModel {
    public static final String TAG = RepositoryModel.class.getSimpleName();

    private LruCache<String, List<Label>> lruLabelsCache = new LruCache<>(5);
    private AndroidFetchNetworkDataTask fetchRepositoryNetworkDataTask;
    private AndroidFetchNetworkDataTask fetchRepositoryFileNetworkDataTask;

    private AndroidFetchNetworkDataTask fetchAllLabelsFileNetworkDataTask;
    private AndroidFetchNetworkDataTask fetchIssuesFileNetworkDataTask;
    private AndroidFetchNetworkDataTask fetchPullsFileNetworkDataTask;

    public RepositoryModel(MainController controller) {
        super(controller);
    }

    @Override
    public void initBusiness() {
    }


    @Override
    public String name() {
        return TAG;
    }

    public Observable<Repo> loadRepositoryDetailWithReactor(final String ownerName, final String repoName) {
        return Observable.create(new Observable.OnSubscribe<Response>() {
            @Override
            public void call(final Subscriber<? super Response> subscriber) {
                Request request = new Request.Builder()
                        .method("GET")
                        .relativeUrl(Path.replace(Path.Repositories.REPOS_FULLNAME, new Pair<>("owner", ownerName), new Pair<>("repo", repoName)))
                        .build();
                final NetworkTransport networkTransport = getMainController().getNetworkTransport();
                networkTransport.setAPIUrl(Path.HOST_GITHUB);
                fetchRepositoryNetworkDataTask = new AndroidFetchNetworkDataTask(getMainController().getNetworkTransport(), request, subscriber);
                getMainController().getBackgroundExecutor().execute(fetchRepositoryNetworkDataTask);
            }
        }).flatMap(new Func1<Response, Observable<Repo>>() {
            @Override
            public Observable<Repo> call(Response response) {
                ResponseBody rb = (ResponseBody) response.getBody();
                try {
                    Repo repo = JsonConverter.convert(rb.string(), Repo.class);
                    return Observable.just(repo);
                } catch (IOException e) {
                    return Observable.error(e);
                }
            }
        });
    }

    public Observable<List<Repo>> loadUsersRepos(String username) {
        UserModel userModel = getMainController().getModelFactory()
                .getOrCreateIfAbsent(UserModel.TAG, UserModel.class, getMainController());
        return userModel.loadRepositoriesWithReactor(username);
    }

    public Observable<GitBlob> loadFile(final String ownerName, final String repoName, final String fileName) {
        return Observable.create(new Observable.OnSubscribe<Response>() {
            @Override
            public void call(final Subscriber<? super Response> subscriber) {
                Request request = new Request.Builder()
                        .method("GET")
                        .relativeUrl(Path.replace(Path.Repositories.REPOS_CONTENTS,
                                new Pair<>("owner", ownerName),
                                new Pair<>("repo", repoName),
                                new Pair<>("+path", fileName)))
                        .build();
                final NetworkTransport networkTransport = getMainController().getNetworkTransport();
                networkTransport.setAPIUrl(Path.HOST_GITHUB);
                fetchRepositoryFileNetworkDataTask = new AndroidFetchNetworkDataTask(getMainController().getNetworkTransport(), request, subscriber);
                getMainController().getBackgroundExecutor().execute(fetchRepositoryFileNetworkDataTask);
            }
        }).flatMap(new Func1<Response, Observable<GitBlob>>() {
            @Override
            public Observable<GitBlob> call(Response response) {
                ResponseBody rb = (ResponseBody) response.getBody();
                try {
                    GitBlob blob = JsonConverter.convert(rb.string(), GitBlob.class);
                    return Observable.just(blob);
                } catch (IOException e) {
                    return Observable.error(e);
                }
            }
        });
    }

    public List<Label> getAllLables(final String ownerName, final String repoName) {
        return lruLabelsCache.get(ownerName + "/" + repoName);
    }

    public void putAllLables(final String ownerName, final String repoName, List<Label> labels) {
        lruLabelsCache.put(ownerName + "/" + repoName, labels);
    }

    public Observable<List<Label>> loadAllLabelsWithReactor(final String ownerName, final String repoName) {
        return Observable.create(new Observable.OnSubscribe<Response>() {
            @Override
            public void call(final Subscriber<? super Response> subscriber) {
                Request request = new Request.Builder()
                        .method("GET")
                        .relativeUrl(Path.replace(Path.Repositories.REPOS_LABELS, new Pair<>("owner", ownerName), new Pair<>("repo", repoName)))
                        .build();
                final NetworkTransport networkTransport = getMainController().getNetworkTransport();
                networkTransport.setAPIUrl(Path.HOST_GITHUB);
                fetchAllLabelsFileNetworkDataTask = new AndroidFetchNetworkDataTask(getMainController().getNetworkTransport(), request, subscriber);
                getMainController().getBackgroundExecutor().execute(fetchAllLabelsFileNetworkDataTask);
            }
        }).flatMap(new Func1<Response, Observable<List<Label>>>() {
            @Override
            public Observable<List<Label>> call(Response response) {
                ResponseBody rb = (ResponseBody) response.getBody();
                try {
                    List<Label> labels = JsonConverter.convert(rb.string(), new TypeReference<List<Label>>() {
                    });
                    return Observable.just(labels);
                } catch (IOException e) {
                    return Observable.error(e);
                }
            }
        });
    }

    public Observable<List<Issue>> loadRecentIssuesWithReactor(final String ownerName, final String repoName) {
        return loadIssuesWithReactor(ownerName, repoName, null, null, null, "comments", null, null, 0, 5);
    }

    /**
     * List Issues
     *
     * @param ownerName owner login
     * @param repoName  repo name
     * @param filter    Indicates which sorts of issues to return. Can be one of:
     *                  assigned: Issues assigned to you
     *                  created: Issues created by you
     *                  mentioned: Issues mentioning you
     *                  subscribed: Issues you're subscribed to updates for
     *                  all: All issues the authenticated user can see, regardless of participation or creation
     *                  Default: assigned
     * @param state     Indicates the state of the issues to return. Can be either open, closed, or all. Default: open
     * @param labels    A list of comma separated label names. Example: bug,ui,@high
     * @param sort      What to sort results by. Can be either created, updated, comments. Default: created
     * @param direction The direction of the sort. Can be either asc or desc. Default: desc
     * @param since     Only issues updated at or after this time are returned. This is a timestamp in ISO 8601 format: YYYY-MM-DDTHH:MM:SSZ.
     * @param page
     * @param perPage
     * @return Observable<List<Issue>>
     */
    public Observable<List<Issue>> loadIssuesWithReactor(final String ownerName, final String repoName,
                                                         final String filter,
                                                         final String state,
                                                         final String labels,
                                                         final String sort,
                                                         final String direction,
                                                         final String since,
                                                         final int page, final int perPage) {
        return Observable.create(new Observable.OnSubscribe<Response>() {
            @Override
            public void call(final Subscriber<? super Response> subscriber) {
                Request request = new Request.Builder()
                        .method("GET")
                        .relativeUrl(Path.replace(Path.Repositories.REPOS_ISSUES, new Pair<>("owner", ownerName), new Pair<>("repo", repoName)))
                        .build();
                if (!TextUtils.isEmpty(filter)) {
                    request.addQueryParam("filter", filter);
                }
                if (!TextUtils.isEmpty(state)) {
                    request.addQueryParam("state", state);
                }
                if (!TextUtils.isEmpty(labels)) {
                    request.addQueryParam("labels", labels);
                }
                if (!TextUtils.isEmpty(sort)) {
                    request.addQueryParam("sort", sort);
                }
                if (!TextUtils.isEmpty(direction)) {
                    request.addQueryParam("direction", direction);
                }
                if (!TextUtils.isEmpty(since)) {
                    request.addQueryParam("since", since);
                }
                if (page != -1) {
                    request.addQueryParam("page", String.valueOf(page));
                }
                if (perPage != -1) {
                    request.addQueryParam("per_page", String.valueOf(perPage));
                }
                final NetworkTransport networkTransport = getMainController().getNetworkTransport();
                networkTransport.setAPIUrl(Path.HOST_GITHUB);
                fetchIssuesFileNetworkDataTask = new AndroidFetchNetworkDataTask(getMainController().getNetworkTransport(), request, subscriber);
                getMainController().getBackgroundExecutor().execute(fetchIssuesFileNetworkDataTask);
            }
        }).flatMap(new Func1<Response, Observable<List<Issue>>>() {
            @Override
            public Observable<List<Issue>> call(Response response) {
                ResponseBody rb = (ResponseBody) response.getBody();
                try {
                    List<Issue> issues = JsonConverter.convert(rb.string(), new TypeReference<List<Issue>>() {
                    });
                    return Observable.just(issues);
                } catch (IOException e) {
                    return Observable.error(e);
                }
            }
        });
    }

    public Observable<List<PullRequest>> loadRecentPullsWithReactor(final String ownerName, final String repoName) {
        return loadPullsWithReactor(ownerName, repoName, null, null, null, "popularity", null, 0, 5);
    }

    /**
     * List pull requests
     *
     * @param ownerName owner login
     * @param repoName  repo name
     * @param state     Either open, closed, or all to filter by state. Default: open
     * @param head      Filter pulls by head user and branch name in the format of user:ref-name. Example: github:new-script-format.
     * @param base      Filter pulls by base branch name. Example: gh-pages.
     * @param sort      What to sort results by. Can be either created, updated, popularity (comment count) or long-running (age, filtering by pulls updated in the last month). Default: created
     * @param direction The direction of the sort. Can be either asc or desc. Default: desc when sort is created or sort is not specified, otherwise asc.
     * @param page
     * @param perPage
     * @return Observable<List<PullRequest>>
     */
    public Observable<List<PullRequest>> loadPullsWithReactor(final String ownerName, final String repoName,
                                                              final String state,
                                                              final String head,
                                                              final String base,
                                                              final String sort,
                                                              final String direction,
                                                              final int page, final int perPage) {
        return Observable.create(new Observable.OnSubscribe<Response>() {
            @Override
            public void call(final Subscriber<? super Response> subscriber) {
                Request request = new Request.Builder()
                        .method("GET")
                        .relativeUrl(Path.replace(Path.Repositories.REPOS_PULLS, new Pair<>("owner", ownerName), new Pair<>("repo", repoName)))
                        .build();

                if (!TextUtils.isEmpty(state)) {
                    request.addQueryParam("state", state);
                }
                if (!TextUtils.isEmpty(head)) {
                    request.addQueryParam("head", head);
                }
                if (!TextUtils.isEmpty(base)) {
                    request.addQueryParam("base", base);
                }
                if (!TextUtils.isEmpty(sort)) {
                    request.addQueryParam("sort", sort);
                }
                if (!TextUtils.isEmpty(direction)) {
                    request.addQueryParam("direction", direction);
                }
                if (page != -1) {
                    request.addQueryParam("page", String.valueOf(page));
                }
                if (perPage != -1) {
                    request.addQueryParam("per_page", String.valueOf(perPage));
                }
                final NetworkTransport networkTransport = getMainController().getNetworkTransport();
                networkTransport.setAPIUrl(Path.HOST_GITHUB);
                fetchPullsFileNetworkDataTask = new AndroidFetchNetworkDataTask(getMainController().getNetworkTransport(), request, subscriber);
                getMainController().getBackgroundExecutor().execute(fetchPullsFileNetworkDataTask);
            }
        }).flatMap(new Func1<Response, Observable<List<PullRequest>>>() {
            @Override
            public Observable<List<PullRequest>> call(Response response) {
                ResponseBody rb = (ResponseBody) response.getBody();
                try {
                    List<PullRequest> pulls = JsonConverter.convert(rb.string(), new TypeReference<List<PullRequest>>() {
                    });
                    return Observable.just(pulls);
                } catch (IOException e) {
                    return Observable.error(e);
                }
            }
        });
    }
}
