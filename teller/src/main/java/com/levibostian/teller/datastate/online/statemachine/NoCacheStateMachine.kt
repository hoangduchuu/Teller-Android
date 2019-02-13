package com.levibostian.teller.datastate.online.statemachine

/**
 * State machine for the phase of data's lifecycle when no cache exists.
 *
 * This is always the first phase all cache data will go through. You must have a fetch successful fetch to move onto any other node in the state machine.
 */
internal class NoCacheStateMachine private constructor(val state: State, val errorDuringFetch: Throwable?) {

    var isFetching: Boolean = false
        get() = state == State.IS_FETCHING

    companion object {
        fun noCacheExists(): NoCacheStateMachine = NoCacheStateMachine(State.NO_CACHE_EXISTS, null)
    }

    fun fetching(): NoCacheStateMachine = NoCacheStateMachine(State.IS_FETCHING, null)
    fun failedFetching(error: Throwable) = NoCacheStateMachine(State.NO_CACHE_EXISTS, error)

    internal enum class State {
        NO_CACHE_EXISTS,
        IS_FETCHING
    }

    override fun toString(): String {
        return when (state) {
            State.IS_FETCHING -> "Cache data does not exist, but it is being fetched for first time."
            State.NO_CACHE_EXISTS -> "Cache data does not exist. It is not fetching data."
        }
    }

}