package com.levibostian.teller.cachestate.online

import com.google.common.truth.Truth.assertThat
import com.levibostian.teller.cachestate.OnlineCacheState
import com.levibostian.teller.cachestate.listener.OnlineCacheStateCacheListener
import com.levibostian.teller.cachestate.listener.OnlineCacheStateFetchingListener
import com.levibostian.teller.cachestate.listener.OnlineCacheStateNoCacheStateListener
import com.levibostian.teller.cachestate.online.statemachine.OnlineCacheStateStateMachine
import com.levibostian.teller.repository.OnlineRepository
import org.junit.Test

import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import com.nhaarman.mockito_kotlin.*
import java.util.*

@RunWith(MockitoJUnitRunner::class)
class OnlineCacheStateTest {

    private lateinit var cacheState: OnlineCacheState<String>
    @Mock private lateinit var cacheRequirements: OnlineRepository.GetCacheRequirements
    @Mock private lateinit var cacheStateListener: OnlineCacheStateCacheListener<String>
    @Mock private lateinit var noCacheStateListener: OnlineCacheStateNoCacheStateListener
    @Mock private lateinit var fetchingFreshCacheListener: OnlineCacheStateFetchingListener

    @Test
    fun none_setsCorrectProperties() {
        cacheState = OnlineCacheState.none()

        assertThat(cacheState.noCacheExists).isFalse()
        assertThat(cacheState.fetchingForFirstTime).isFalse()
        assertThat(cacheState.cacheData).isNull()
        assertThat(cacheState.lastTimeFetched).isNull()
        assertThat(cacheState.isFetchingFreshData).isFalse()
        assertThat(cacheState.requirements).isNull()
        assertThat(cacheState.stateMachine).isNull()
        assertThat(cacheState.errorDuringFirstFetch).isNull()
        assertThat(cacheState.justCompletedSuccessfulFirstFetch).isFalse()
        assertThat(cacheState.errorDuringFetch).isNull()
        assertThat(cacheState.justCompletedSuccessfullyFetchingFreshData).isFalse()
    }

    @Test
    fun deliverCacheState_cacheEmpty_expectCallListenerFunctions() {
        val fetched = Date()
        cacheState = OnlineCacheStateStateMachine.cacheExists<String>(cacheRequirements, fetched).change().cacheEmpty()

        cacheState.deliverCacheState(cacheStateListener)

        verify(cacheStateListener).cacheEmpty(fetched)
        verify(cacheStateListener, never()).cache(anyOrNull(), any())
    }

    @Test
    fun deliverCacheState_cacheData_expectCallListenerFunctions() {
        val data = "foo"
        val dataFetched = Date()
        cacheState = OnlineCacheStateStateMachine.cacheExists<String>(cacheRequirements, dataFetched).change().cache(data)

        cacheState.deliverCacheState(cacheStateListener)

        verify(cacheStateListener, never()).cacheEmpty(any())
        verify(cacheStateListener).cache(data, dataFetched)
    }

    @Test
    fun deliverCacheState_noCache_expectNoListenerCalls() {
        cacheState = OnlineCacheStateStateMachine.noCacheExists<String>(cacheRequirements).change().firstFetch()

        cacheState.deliverCacheState(cacheStateListener)

        verify(cacheStateListener, never()).cacheEmpty(any())
        verify(cacheStateListener, never()).cache(anyOrNull(), any())
    }

    @Test
    fun deliverNoCacheState_noCache_expectCallListenerFunctions() {
        cacheState = OnlineCacheStateStateMachine.noCacheExists(cacheRequirements)

        cacheState.deliverNoCacheState(noCacheStateListener)

        verify(noCacheStateListener).noCache()
        verify(noCacheStateListener, never()).firstFetch()
        verify(noCacheStateListener, never()).finishedFirstFetch(anyOrNull())
    }

    @Test
    fun deliverNoCacheState_fetchingForFirstTime_expectCallListenerFunctions() {
        cacheState = OnlineCacheStateStateMachine.noCacheExists<String>(cacheRequirements).change().firstFetch()

        cacheState.deliverNoCacheState(noCacheStateListener)

        verify(noCacheStateListener).noCache()
        verify(noCacheStateListener).firstFetch()
        verify(noCacheStateListener, never()).finishedFirstFetch(anyOrNull())
    }

    @Test
    fun deliverNoCacheState_finishedFirstFetch_expectCallListenerFunctions() {
        val timeFetched = Date()
        cacheState = OnlineCacheStateStateMachine.noCacheExists<String>(cacheRequirements).change().firstFetch().change().successfulFirstFetch(timeFetched)

        cacheState.deliverNoCacheState(noCacheStateListener)

        verify(noCacheStateListener, never()).noCache()
        verify(noCacheStateListener, never()).firstFetch()
        verify(noCacheStateListener).finishedFirstFetch(null)
    }

    @Test
    fun deliverNoCacheState_errorFirstFetch_expectCallListenerFunctions() {
        val error = RuntimeException("")
        cacheState = OnlineCacheStateStateMachine.noCacheExists<String>(cacheRequirements).change().firstFetch().change().failFirstFetch(error)

        cacheState.deliverNoCacheState(noCacheStateListener)

        verify(noCacheStateListener).noCache()
        verify(noCacheStateListener, never()).firstFetch()
        verify(noCacheStateListener).finishedFirstFetch(error)
    }

    @Test
    fun deliverNoCacheState_cacheExists_expectNoListenerCalls() {
        cacheState = OnlineCacheStateStateMachine.cacheExists<String>(cacheRequirements, Date()).change().cacheEmpty()

        cacheState.deliverNoCacheState(noCacheStateListener)

        verify(noCacheStateListener, never()).noCache()
        verify(noCacheStateListener, never()).firstFetch()
        verify(noCacheStateListener, never()).finishedFirstFetch(anyOrNull())
    }

    @Test
    fun deliverFetchingFreshCacheState_fetchingFreshCache_expectCallListenerFunctions() {
        cacheState = OnlineCacheStateStateMachine.cacheExists<String>(cacheRequirements, Date()).change().fetchingFreshCache()

        cacheState.deliverFetchingFreshCacheState(fetchingFreshCacheListener)

        verify(fetchingFreshCacheListener).fetching()
        verify(fetchingFreshCacheListener, never()).finishedFetching(anyOrNull())
    }

    @Test
    fun deliverFetchingFreshCacheState_finishedFetchingFreshCache_expectCallListenerFunctions() {
        val error = RuntimeException()
        cacheState = OnlineCacheStateStateMachine.cacheExists<String>(cacheRequirements, Date()).change().fetchingFreshCache().change().failRefreshCache(error)

        cacheState.deliverFetchingFreshCacheState(fetchingFreshCacheListener)

        verify(fetchingFreshCacheListener, never()).fetching()
        verify(fetchingFreshCacheListener).finishedFetching(error)
    }

    @Test
    fun deliverFetchingFreshCacheState_notFetchingFreshData_expectNoListenerCalls() {
        cacheState = OnlineCacheStateStateMachine.cacheExists<String>(cacheRequirements, Date()).change().cacheEmpty()

        cacheState.deliverFetchingFreshCacheState(fetchingFreshCacheListener)

        verify(fetchingFreshCacheListener, never()).fetching()
        verify(fetchingFreshCacheListener, never()).finishedFetching(anyOrNull())
    }

}