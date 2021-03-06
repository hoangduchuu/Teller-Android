package com.levibostian.teller.subject


import com.google.common.truth.Truth.assertThat
import com.levibostian.teller.cachestate.OnlineCacheState
import com.levibostian.teller.cachestate.online.statemachine.OnlineCacheStateStateMachine
import com.levibostian.teller.extensions.plusAssign
import com.levibostian.teller.repository.OnlineRepository
import org.junit.Test
import com.levibostian.teller.util.AssertionUtil.Companion.check

import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import io.reactivex.disposables.CompositeDisposable
import org.junit.After
import org.junit.Before
import java.util.*

@RunWith(MockitoJUnitRunner::class)
class OnlineCacheStateBehaviorSubjectTest {

    private lateinit var subject: OnlineCacheStateBehaviorSubject<String>
    @Mock private lateinit var requirements: OnlineRepository.GetCacheRequirements

    private lateinit var compositeDisposable: CompositeDisposable

    @Before
    fun setup() {
        compositeDisposable = CompositeDisposable()
        subject = OnlineCacheStateBehaviorSubject()
    }

    @After
    fun teardown() {
        compositeDisposable.clear()
    }

    @Test
    fun init_defaultValue() {
        compositeDisposable += subject.asObservable()
                .test()
                .awaitCount(1)
                .assertValue(check {
                    assertThat(it).isEqualTo(OnlineCacheState.none<String>())
                })
    }

    @Test
    fun resetStateToNone_receiveNoDataState() {
        subject.resetStateToNone()

        compositeDisposable += subject.asObservable()
                .test()
                .awaitCount(1)
                .assertValue(check {
                    assertThat(it).isEqualTo(OnlineCacheState.none<String>())
                })
    }

    @Test
    fun resetToNoCacheState_receiveCorrectDataState() {
        subject.resetToNoCacheState(requirements)

        compositeDisposable += subject.asObservable()
                .test()
                .awaitCount(1)
                .assertValue(check {
                    assertThat(it).isEqualTo(OnlineCacheStateStateMachine.noCacheExists<String>(requirements))
                })
    }

    @Test
    fun resetToCacheState_receiveCorrectDataState() {
        val lastTimeFetched = Date()
        subject.resetToCacheState(requirements, lastTimeFetched)

        compositeDisposable += subject.asObservable()
                .test()
                .awaitCount(1)
                .assertValue(check {
                    assertThat(it).isEqualTo(OnlineCacheStateStateMachine.cacheExists<String>(requirements, lastTimeFetched))
                })
    }

    @Test
    fun changeState_sendsResultToSubject() {
        subject.resetToNoCacheState(requirements)

        val testObserver = subject.asObservable().test()

        subject.changeState { it.firstFetch() }

        compositeDisposable += testObserver
                .awaitCount(2)
                .assertValues(
                        OnlineCacheStateStateMachine.noCacheExists(requirements),
                        OnlineCacheStateStateMachine.noCacheExists<String>(requirements).change().firstFetch())
    }

    @Test
    fun multipleObservers_receiveDifferentNumberOfEvents() {
        subject.resetStateToNone()
        subject.resetToNoCacheState(requirements)
        subject.changeState { it.firstFetch() }

        val testObserver1 = subject.asObservable().test()

        val fetched = Date()
        subject.changeState { it.successfulFirstFetch(fetched) }

        val testObserver2 = subject.asObservable().test()

        val data = "foo"
        subject.changeState { it.cache(data) }

        val expectedSeriesOfEvents = listOf(
                OnlineCacheStateStateMachine.noCacheExists<String>(requirements).change()
                        .firstFetch(),
                OnlineCacheStateStateMachine.noCacheExists<String>(requirements).change()
                        .firstFetch().change()
                        .successfulFirstFetch(fetched),
                OnlineCacheStateStateMachine.cacheExists<String>(requirements, fetched).change()
                        .cache(data)
        )

        compositeDisposable += testObserver1
                .awaitCount(expectedSeriesOfEvents.size)
                .assertValueSequence(expectedSeriesOfEvents)

        compositeDisposable += testObserver2
                .awaitCount(expectedSeriesOfEvents.size - 1)
                .assertValueSequence(expectedSeriesOfEvents.subList(1, expectedSeriesOfEvents.size))
    }

}