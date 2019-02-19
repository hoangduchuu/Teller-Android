package com.levibostian.tellerexample.activity

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.levibostian.tellerexample.R
import com.levibostian.tellerexample.model.db.AppDatabase
import com.levibostian.tellerexample.service.GitHubService
import com.levibostian.tellerexample.viewmodel.ReposViewModel
import retrofit2.Retrofit
import android.arch.persistence.room.Room
import android.os.Handler
import android.support.design.widget.Snackbar
import android.support.v7.widget.LinearLayoutManager
import android.text.format.DateUtils
import android.view.View
import android.widget.TextView
import com.levibostian.teller.datastate.listener.LocalDataStateListener
import com.levibostian.teller.datastate.listener.OnlineDataStateListener
import com.levibostian.tellerexample.adapter.RepoRecyclerViewAdapter
import com.levibostian.tellerexample.model.RepoModel
import com.levibostian.tellerexample.viewmodel.GitHubUsernameViewModel
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import java.util.*
import android.support.v7.app.AlertDialog
import com.levibostian.tellerexample.extensions.closeKeyboard
import com.levibostian.tellerexample.util.DependencyUtil


class MainActivity : AppCompatActivity() {

    private lateinit var reposViewModel: ReposViewModel
    private lateinit var gitHubUsernameViewModel: GitHubUsernameViewModel
    private lateinit var service: GitHubService
    private lateinit var db: AppDatabase

    private var updateLastSyncedHandler: Handler? = null
    private var updateLastSyncedRunnable: Runnable? = null

    private var fetchingSnackbar: Snackbar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        showEmptyView()

        initialize()
    }

    private fun initialize() {
        service = DependencyUtil.serviceInstance()
        db = DependencyUtil.dbInstance(application)

        reposViewModel = ViewModelProviders.of(this).get(ReposViewModel::class.java)
        gitHubUsernameViewModel = ViewModelProviders.of(this).get(GitHubUsernameViewModel::class.java)
        reposViewModel.init(service, db)
        gitHubUsernameViewModel.init(this)

        reposViewModel.observeRepos()
                .observe(this, Observer { reposState ->
                    reposState?.deliverAllStates(object : OnlineDataStateListener<List<RepoModel>> {
                        override fun noCache() {
                            // great place to show empty state where first fetch fails.
                        }
                        override fun finishedFirstFetch(errorDuringFetch: Throwable?) {
                            if (errorDuringFetch != null) {
                                errorDuringFetch.message?.let { showEmptyView(it) }

                                AlertDialog.Builder(this@MainActivity)
                                        .setTitle("Error")
                                        .setMessage(errorDuringFetch.message?: "Unknown error. Please, try again.")
                                        .setPositiveButton("Ok") { dialog, _ ->
                                            dialog.dismiss()
                                        }
                                        .create()
                                        .show()
                            }
                        }
                        override fun firstFetch() {
                            showLoadingView()
                        }
                        override fun cacheEmpty(fetched: Date) {
                            showEmptyView()
                        }
                        override fun cacheData(data: List<RepoModel>, fetched: Date) {
                            showDataView()

                            updateLastSyncedRunnable?.let { updateLastSyncedHandler?.removeCallbacks(it) }
                            updateLastSyncedHandler = Handler()
                            updateLastSyncedRunnable = Runnable {
                                data_age_textview.text = "Data last synced ${DateUtils.getRelativeTimeSpanString(fetched.time, System.currentTimeMillis(), DateUtils.SECOND_IN_MILLIS)}"
                                updateLastSyncedHandler?.postDelayed(updateLastSyncedRunnable!!, 1000)
                            }
                            updateLastSyncedHandler?.post(updateLastSyncedRunnable!!)

                            repos_recyclerview.apply {
                                layoutManager = LinearLayoutManager(this@MainActivity)
                                adapter = RepoRecyclerViewAdapter(data)
                                setHasFixedSize(true)
                            }
                        }
                        override fun fetching() {
                            fetchingSnackbar = Snackbar.make(parent_view, "Updating repos list...", Snackbar.LENGTH_LONG)
                            fetchingSnackbar?.show()
                        }
                        override fun finishedFetching(errorDuringFetch: Throwable?) {
                            fetchingSnackbar?.dismiss()
                        }
                    })
                })
        gitHubUsernameViewModel.observeUsername()
                .observe(this, Observer { username ->
                    username?.deliverState(object : LocalDataStateListener<String> {
                        override fun isEmpty() {
                            username_edittext.setText("", TextView.BufferType.EDITABLE)
                        }
                        override fun data(data: String) {
                            username_edittext.setText(data, TextView.BufferType.EDITABLE)
                            reposViewModel.setUsername(data)
                        }
                    })
                })

        go_button.setOnClickListener {
            if (username_edittext.text.isBlank()) {
                username_edittext.error = "Enter a GitHub githubUsername"
            } else {
                gitHubUsernameViewModel.setUsername(username_edittext.text.toString())

                closeKeyboard()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        updateLastSyncedRunnable?.let { updateLastSyncedHandler?.removeCallbacks(it) }
        gitHubUsernameViewModel.dispose()
        reposViewModel.dispose()
    }

    private fun showLoadingView() {
        loading_view.visibility = View.VISIBLE
        empty_view.visibility = View.GONE
        data_view.visibility = View.GONE
    }

    private fun showEmptyView(message: String = "There are no repos.") {
        loading_view.visibility = View.GONE
        empty_view.visibility = View.VISIBLE
        data_view.visibility = View.GONE

        empty_view_textview.text = message
    }

    private fun showDataView() {
        loading_view.visibility = View.GONE
        empty_view.visibility = View.GONE
        data_view.visibility = View.VISIBLE
    }

}
