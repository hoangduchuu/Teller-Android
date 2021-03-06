package com.levibostian.teller.cachestate.listener

import java.util.*

interface OnlineCacheStateCacheListener<in CACHE> {
    fun cacheEmpty(fetched: Date)
    fun cache(data: CACHE, fetched: Date)
}