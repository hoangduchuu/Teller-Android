package com.levibostian.teller.datastate.online.statemachine


/**
 * Immutable state machine for the phase of data's lifecycle when cached data exists.
 */
internal class CacheStateMachine<CACHE: Any> private constructor(val state: State, val cache: CACHE?) {

    companion object {
        fun <CACHE: Any> cacheEmpty(): CacheStateMachine<CACHE> = CacheStateMachine(State.CACHE_EMPTY, null)
        fun <CACHE: Any> cacheExists(cache: CACHE): CacheStateMachine<CACHE> = CacheStateMachine(State.CACHE_NOT_EMPTY, cache)
    }

    internal enum class State {
        CACHE_EMPTY,
        CACHE_NOT_EMPTY
    }

    override fun toString(): String {
        return when (state) {
            State.CACHE_EMPTY -> "Cache data exists and is empty."
            State.CACHE_NOT_EMPTY -> "Cache data exists and is not empty (cache value: ${cache.toString()})."
        }
    }

}